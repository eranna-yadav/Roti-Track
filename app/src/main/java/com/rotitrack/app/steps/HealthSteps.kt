package com.rotitrack.app.steps

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.rotitrack.app.ui.WatchSteps
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period

/**
 * Steps from a smartwatch or fitness band, read from Android's Health Connect, where watch apps
 * (Fitbit, Samsung Health, Mi Fitness, Google Fit…) save them. Read only; nothing is written.
 */
object HealthSteps {
    val PERMISSION: String = HealthPermission.getReadPermission(StepsRecord::class)
    private const val DAYS = 7L

    fun status(context: Context): WatchSteps = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> WatchSteps.OFF
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> WatchSteps.INSTALL
        else -> WatchSteps.UNSUPPORTED
    }

    /** Whether Health Connect is there and the user allowed reading steps. */
    suspend fun connected(context: Context): Boolean = runCatching {
        status(context) == WatchSteps.OFF &&
            PERMISSION in HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()
    }.getOrDefault(false)

    suspend fun state(context: Context): WatchSteps {
        val s = status(context)
        return if (s == WatchSteps.OFF && connected(context)) WatchSteps.ON else s
    }

    suspend fun disconnect(context: Context) {
        runCatching { HealthConnectClient.getOrCreate(context).permissionController.revokeAllPermissions() }
    }

    /** Opens Health Connect in the Play Store, for phones that need to install or update it. */
    fun installIntent(): Intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"),
    ).setPackage("com.android.vending").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Steps per day for the last week (day → steps), or null if not connected or unreadable. */
    suspend fun readDays(context: Context): Map<String, Int>? {
        if (!connected(context)) return null
        return runCatching {
            val start = LocalDate.now().minusDays(DAYS - 1).atStartOfDay()
            val request = AggregateGroupByPeriodRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, LocalDateTime.now()),
                timeRangeSlicer = Period.ofDays(1),
            )
            HealthConnectClient.getOrCreate(context).aggregateGroupByPeriod(request).associate { group ->
                group.startTime.toLocalDate().toString() to (group.result[StepsRecord.COUNT_TOTAL] ?: 0L).toInt()
            }
        }.getOrNull()
    }
}
