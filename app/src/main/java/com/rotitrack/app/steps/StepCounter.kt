package com.rotitrack.app.steps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.fitness.FitnessLocal
import com.google.android.gms.fitness.LocalRecordingClient
import com.google.android.gms.fitness.data.LocalDataType
import com.google.android.gms.fitness.data.LocalField
import com.google.android.gms.fitness.request.LocalDataReadRequest
import com.rotitrack.app.domain.Days
import com.rotitrack.app.rotiTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Counts steps for the signed-in user.
 *
 * Normally Google Play services records steps on the phone in the background (the Recording
 * API, as Google Fit does), even while Roti Track is closed and the screen is off; the app reads
 * the daily totals when it opens, every 20 seconds while it's on screen, and every 15 minutes in
 * the background. Phones without a recent Play services fall back to reading the step sensor
 * directly, which only counts while the app is open or a background check runs.
 */
object StepCounter {
    private const val WORK = "step-count"
    private const val DAYS = 7

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var liveJob: Job? = null
    private var listener: SensorEventListener? = null
    private var savedAt = 0L
    private var savedCounter = -1L

    private fun sensors(context: Context) = context.getSystemService(SensorManager::class.java)

    private fun hasSensor(context: Context): Boolean = sensors(context)?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    /** Google's on-device step recording: Android 10+ with a recent Google Play services. */
    fun recordingAvailable(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                context, LocalRecordingClient.LOCAL_RECORDING_CLIENT_MIN_VERSION_CODE,
            ) == ConnectionResult.SUCCESS

    fun available(context: Context): Boolean = recordingAvailable(context) || hasSensor(context)

    fun permitted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    /** Switches background counting on or off. */
    fun schedule(context: Context, on: Boolean) {
        val app = context.applicationContext
        val wm = WorkManager.getInstance(app)
        if (on) {
            wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<StepWorker>(15, TimeUnit.MINUTES).build())
            if (recordingAvailable(app) && permitted(app)) {
                scope.launch { runCatching { FitnessLocal.getLocalRecordingClient(app).subscribe(LocalDataType.TYPE_STEP_COUNT_DELTA).await() } }
            }
        } else {
            wm.cancelUniqueWork(WORK)
            if (recordingAvailable(app)) {
                scope.launch { runCatching { FitnessLocal.getLocalRecordingClient(app).unsubscribe(LocalDataType.TYPE_STEP_COUNT_DELTA).await() } }
            }
        }
    }

    /**
     * While the app is on screen the number goes up as you walk, read straight from the phone's
     * step counter (the same one Google's recording uses). Watch steps are read too.
     */
    fun startLive(context: Context) {
        val app = context.applicationContext
        if (!permitted(app) || liveJob?.isActive == true) return
        val recording = recordingAvailable(app)
        startSensor(app, recording)
        liveJob = scope.launch {
            while (isActive) {
                // The live listener already reads the step counter, so no extra sensor read here.
                refresh(app, phone = recording, sensor = false)
                delay(20_000)
            }
        }
    }

    fun stopLive(context: Context) {
        liveJob?.cancel()
        liveJob = null
        listener?.let { sensors(context)?.unregisterListener(it) }
        listener = null
        context.rotiTrack.activeStore?.clearLiveSteps()
        savedCounter = -1L
    }

    /**
     * Reads the latest steps into the store: one step-counter reading ([sensor]), daily totals from
     * Google's recording ([phone]), and a watch's steps from Health Connect. See
     * [com.rotitrack.app.domain.Steps.merge]. A day's total never goes down.
     */
    suspend fun refresh(context: Context, phone: Boolean = true, sensor: Boolean = true) {
        val store = context.rotiTrack.activeStore ?: return
        val allowed = permitted(context)
        val recording = allowed && recordingAvailable(context)
        if (sensor && allowed) {
            readSensorOnce(context)?.let { counter ->
                withContext(Dispatchers.Main) { store.recordStepCounter(counter, Days.today(), sameDayOnly = recording) }
            }
        }
        val days = if (phone && recording) readDays(context) else null
        val watch = HealthSteps.readDays(context)
        val watchUnread = watch == null && HealthSteps.connected(context)
        withContext(Dispatchers.Main) { store.mergeSteps(days, watch, watchUnread) }
    }

    /** Steps per day for the last week, from Google's on-device recording. */
    private suspend fun readDays(context: Context): Map<String, Int>? = runCatching {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).minusDays((DAYS - 1).toLong()).atStartOfDay(zone).toEpochSecond()
        val end = Instant.now().epochSecond
        val request = LocalDataReadRequest.Builder()
            .aggregate(LocalDataType.TYPE_STEP_COUNT_DELTA)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(start, end, TimeUnit.SECONDS)
            .build()
        val response = FitnessLocal.getLocalRecordingClient(context).readData(request).await()
        response.buckets.associate { bucket ->
            val day = Instant.ofEpochMilli(bucket.getStartTime(TimeUnit.MILLISECONDS)).atZone(zone).toLocalDate().toString()
            day to bucket.dataSets.sumOf { set -> set.dataPoints.sumOf { it.getValue(LocalField.FIELD_STEPS).asInt() } }
        }
    }.getOrNull()

    /**
     * The phone's step counter, delivered as it changes (no batching), so the number on screen
     * moves while you walk. The reading is saved at most every 10 steps or 15 seconds, so the count
     * carries on from where it was after the app is closed or the screen turns off.
     */
    private fun startSensor(context: Context, recording: Boolean) {
        if (listener != null) return
        val manager = sensors(context) ?: return
        val sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val store = context.rotiTrack.activeStore ?: return
                val counter = event.values[0].toLong()
                val today = Days.today()
                val now = SystemClock.elapsedRealtime()
                if (savedCounter < 0 || counter - savedCounter >= 10 || counter < savedCounter || now - savedAt > 15_000) {
                    store.recordStepCounter(counter, today, sameDayOnly = recording)
                    store.clearLiveSteps()
                    savedCounter = counter
                    savedAt = now
                } else {
                    store.showLiveSteps(store.stepsWithCounter(counter, today, sameDayOnly = recording), today)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI, 0)
    }

    /** Fallback: one reading of the step sensor, or null if it doesn't answer within 10 seconds. */
    private suspend fun readSensorOnce(context: Context): Long? {
        val manager = sensors(context) ?: return null
        val sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null
        return withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine { cont ->
                val l = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        manager.unregisterListener(this)
                        if (cont.isActive) cont.resume(event.values[0].toLong())
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                manager.registerListener(l, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                cont.invokeOnCancellation { manager.unregisterListener(l) }
            }
        }
    }
}

class StepWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        StepCounter.refresh(applicationContext)
        return Result.success()
    }
}
