package com.afraz.campusos.feature_toolkit.focus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.afraz.campusos.MainActivity
import com.afraz.campusos.R

class PomodoroForegroundService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var paused = false
    private var remainingMillis = FOCUS_DURATION
    private var endAtElapsedMillis = 0L
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_STOP && running && !paused) {
            handler.postDelayed({
                if (ProcessLifecycleOwner.get().lifecycle.currentState < Lifecycle.State.STARTED && running && !paused) {
                    postNotification(RETURN_NOTIFICATION_ID, "Back to your focus session", "Your Pomodoro is still running. Come back to CampusOS.", false)
                }
            }, 3_000L)
        }
    }
    private val tick = object : Runnable {
        override fun run() {
            if (!running || paused) return
            remainingMillis = (endAtElapsedMillis - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            if (remainingMillis == 0L) {
                running = false
                postNotification(SESSION_NOTIFICATION_ID, "Focus session complete", "Take a short break, then start another session.", false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }
            updateSessionNotification()
            handler.postDelayed(this, 1_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannels()
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_START -> beginOrResume()
            ACTION_PAUSE -> togglePause()
            ACTION_SKIP, ACTION_STOP -> finishSession()
        }
        return START_STICKY
    }

    private fun beginOrResume() {
        if (!running) remainingMillis = FOCUS_DURATION
        running = true
        paused = false
        endAtElapsedMillis = SystemClock.elapsedRealtime() + remainingMillis
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(SESSION_NOTIFICATION_ID, buildSessionNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(SESSION_NOTIFICATION_ID, buildSessionNotification())
        }
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    private fun togglePause() {
        if (!running) {
            beginOrResume()
        } else if (paused) {
            beginOrResume()
        } else {
            remainingMillis = (endAtElapsedMillis - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            paused = true
            handler.removeCallbacks(tick)
            updateSessionNotification()
        }
    }

    private fun finishSession() {
        running = false
        paused = false
        remainingMillis = FOCUS_DURATION
        handler.removeCallbacks(tick)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateSessionNotification() {
        getSystemService(NotificationManager::class.java).notify(SESSION_NOTIFICATION_ID, buildSessionNotification())
    }

    private fun buildSessionNotification() = NotificationCompat.Builder(this, SESSION_CHANNEL)
        .setSmallIcon(R.drawable.ic_stat_campus)
        .setContentTitle(if (paused) "Focus paused" else "Focus session")
        .setContentText(if (paused) "Paused · ${remainingMillis.asClock()} remaining" else "${remainingMillis.asClock()} remaining")
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(openAppPendingIntent())
        .addAction(0, if (paused) "Resume" else "Pause", serviceActionPendingIntent(if (paused) ACTION_START else ACTION_PAUSE, 1))
        .addAction(0, "Skip", serviceActionPendingIntent(ACTION_SKIP, 2))
        .addAction(0, "Stop", serviceActionPendingIntent(ACTION_STOP, 3))
        .build()

    private fun openAppPendingIntent() = PendingIntent.getActivity(
        this,
        10,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun serviceActionPendingIntent(action: String, request: Int) = PendingIntent.getService(
        this,
        request,
        Intent(this, PomodoroForegroundService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun ensureNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(SESSION_CHANNEL, "Focus timer", NotificationManager.IMPORTANCE_LOW))
        manager.createNotificationChannel(NotificationChannel(RETURN_CHANNEL, "Focus reminders", NotificationManager.IMPORTANCE_HIGH))
    }

    private fun postNotification(id: Int, title: String, message: String, ongoing: Boolean) {
        val builder = NotificationCompat.Builder(this, RETURN_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_campus)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent())
        if (ongoing) builder.setOngoing(true)
        getSystemService(NotificationManager::class.java).notify(id, builder.build())
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.afraz.campusos.focus.START"
        const val ACTION_PAUSE = "com.afraz.campusos.focus.PAUSE"
        const val ACTION_SKIP = "com.afraz.campusos.focus.SKIP"
        const val ACTION_STOP = "com.afraz.campusos.focus.STOP"
        private const val SESSION_CHANNEL = "campus_focus_session"
        private const val RETURN_CHANNEL = "campus_focus_return"
        private const val SESSION_NOTIFICATION_ID = 32_010
        private const val RETURN_NOTIFICATION_ID = 32_011
        private const val FOCUS_DURATION = 25 * 60_000L

        fun sendAction(context: Context, action: String) {
            val intent = Intent(context, PomodoroForegroundService::class.java).setAction(action)
            if (action == ACTION_START || action == ACTION_PAUSE) context.startForegroundService(intent) else context.startService(intent)
        }
    }
}

private fun Long.asClock(): String {
    val seconds = this / 1_000L
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}
