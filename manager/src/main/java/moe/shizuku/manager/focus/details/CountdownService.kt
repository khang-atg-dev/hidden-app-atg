package moe.shizuku.manager.focus.details

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import moe.shizuku.manager.MainActivity
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings

class CountdownService : Service() {
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "countdown_channel"
        const val NOTIFICATION_ID = 1
    }

    private lateinit var countDownTimer: CountDownTimer

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val currentFocus = ShizukuSettings.getCurrentFocusTask()
            ?: return super.onStartCommand(intent, flags, startId)
        countDownTimer = object : CountDownTimer(currentFocus.remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                ShizukuSettings.saveCurrentFocusTask(
                    currentFocus.copy(remainingTime = millisUntilFinished)
                )
                updateNotification(millisUntilFinished)
            }

            override fun onFinish() {
                ShizukuSettings.removeCurrentFocusTask()
                this@CountdownService.stopSelf()
            }
        }.start()
        startForeground(NOTIFICATION_ID, createNotification(currentFocus.remainingTime))
        return START_STICKY
    }

    override fun onDestroy() {
        countDownTimer.cancel()
        super.onDestroy()
    }

    private fun createNotification(remainingTimeMillis: Long): Notification {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel if on Android 8.0 or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Countdown Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to launch the app when the notification is clicked
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Countdown Timer")
            .setContentText("Time is counting down: ${remainingTimeMillis}s")
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(remainingTimeMillis: Long) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = createNotification(remainingTimeMillis)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}