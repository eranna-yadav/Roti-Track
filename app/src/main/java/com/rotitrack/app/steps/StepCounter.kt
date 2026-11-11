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
import com.rotitrack.app.rotiTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Reads the phone's step counter sensor (steps since the phone started) into the signed-in
 * user's store: live while the app is open, and every 15 minutes in the background so the
 * count keeps up across midnight and restarts.
 */
object StepCounter {
    private const val WORK = "step-count"

    private var listener: SensorEventListener? = null
    private var savedAt = 0L
    private var savedCounter = -1L

    private fun sensors(context: Context) = context.getSystemService(SensorManager::class.java)

    fun available(context: Context): Boolean = sensors(context)?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    fun permitted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    /** Background readings on or off. */
    fun schedule(context: Context, on: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (on) {
            wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<StepWorker>(15, TimeUnit.MINUTES).build())
        } else {
            wm.cancelUniqueWork(WORK)
        }
    }

    /** Live counting while the app is on screen. Saves at most every 10 steps or 15 seconds. */
    fun startLive(context: Context) {
        if (listener != null || !permitted(context)) return
        val manager = sensors(context) ?: return
        val sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        val app = context.applicationContext
        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val counter = event.values[0].toLong()
                val now = SystemClock.elapsedRealtime()
                if (savedCounter < 0 || counter - savedCounter >= 10 || counter < savedCounter || now - savedAt > 15_000) {
                    app.rotiTrack.activeStore?.recordStepCounter(counter)
                    savedCounter = counter
                    savedAt = now
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stopLive(context: Context) {
        listener?.let { sensors(context)?.unregisterListener(it) }
        listener = null
    }

    /** One reading of the counter, or null if the sensor doesn't answer within 10 seconds. */
    suspend fun readOnce(context: Context): Long? {
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
        if (!StepCounter.permitted(applicationContext)) return Result.success()
        val counter = StepCounter.readOnce(applicationContext) ?: return Result.success()
        withContext(Dispatchers.Main) { applicationContext.rotiTrack.activeStore?.recordStepCounter(counter) }
        return Result.success()
    }
}
