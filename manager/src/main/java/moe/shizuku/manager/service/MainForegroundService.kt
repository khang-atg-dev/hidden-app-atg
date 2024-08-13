package moe.shizuku.manager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import moe.shizuku.manager.MainActivity
import moe.shizuku.manager.R

const val CHANNEL_NAME = "MAIN_FOREGROUND_CHANNEL"
const val CHANNEL_ID = "111222"
const val PENDING_INTENT_REQUEST_CODE = 100
const val NOTIFICATION_ID = 101

class MainForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        displayNotification()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun displayNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_NONE
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            PENDING_INTENT_REQUEST_CODE,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentTitle("Hidden app is running in the background.")
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }
}