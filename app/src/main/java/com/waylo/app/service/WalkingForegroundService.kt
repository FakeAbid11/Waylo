package com.waylo.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.waylo.app.MainActivity
import com.waylo.app.R
import com.waylo.app.WayloApplication
import com.waylo.app.core.util.WayloFormat
import com.waylo.app.data.walk.WalkingRepository
import com.waylo.app.domain.model.WalkingState
import com.waylo.app.domain.model.WalkingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WalkingForegroundService : Service() {

    private val repository: WalkingRepository
        get() = (application as WayloApplication).walkingRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var statusJob: Job? = null
    private var lastRenderedText: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        repository.attachService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startAsForeground(repository.status.value)
        } catch (exception: Exception) {
            repository.reportError("Walk tracking could not continue.")
            stopSelf()
            return START_NOT_STICKY
        }
        statusJob = scope.launch {
            repository.status.collect { status -> render(status) }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        statusJob?.cancel()
        scope.cancel()
        repository.detachService()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun startAsForeground(status: WalkingStatus) {
        val notification = buildNotification(status)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun render(status: WalkingStatus) {
        when (status.state) {
            WalkingState.Idle,
            WalkingState.Stopping,
            WalkingState.Completed,
            WalkingState.Error,
            -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                val text = notificationText(status)
                if (text != lastRenderedText) {
                    lastRenderedText = text
                    notificationManager()?.notify(NOTIFICATION_ID, buildNotification(status))
                }
            }
        }
    }

    private fun buildNotification(status: WalkingStatus): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_walk)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(notificationText(status))
            .setContentIntent(openAppPendingIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun notificationText(status: WalkingStatus): String = when (status.state) {
        WalkingState.Starting -> "Starting walk…"
        WalkingState.Paused -> "Walk paused${distanceSuffix(status)}"
        WalkingState.Error -> status.errorMessage ?: "Walk tracking stopped."
        else -> "Walk in progress${distanceSuffix(status)}"
    }

    private fun distanceSuffix(status: WalkingStatus): String =
        " · ${WayloFormat.distance(status.distanceMeters / 1_000.0)}"

    private fun openAppPendingIntent(): PendingIntent? {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Walks",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shown while Waylo is recording a walk."
                setShowBadge(false)
            }
            notificationManager()?.createNotificationChannel(channel)
        }
    }

    private fun notificationManager(): NotificationManager? =
        getSystemService(NotificationManager::class.java)

    companion object {
        const val ACTION_START = "com.waylo.app.action.START_WALK"
        private const val CHANNEL_ID = "waylo_walk"
        private const val NOTIFICATION_ID = 41
    }
}
