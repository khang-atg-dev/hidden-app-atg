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
import moe.shizuku.manager.utils.formatMilliseconds
import moe.shizuku.manager.utils.getTimeAsString
import java.util.Calendar

class CountdownService : Service() {
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "countdown_channel"
        const val NOTIFICATION_ID = 1
    }

    private var countDownTimer: CountDownTimer? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ShizukuSettings.getCurrentFocusTask()?.let {
            countDownTimer?.cancel()
            countDownTimer = object : CountDownTimer(it.remainingTime, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    ShizukuSettings.saveCurrentFocusTask(
                        it.copy(remainingTime = millisUntilFinished)
                    )
                    ShizukuSettings.updateRunningTimeStatisticCurrentFocus(1000)
                    updateNotification(millisUntilFinished)
                }

                override fun onFinish() {
                    val currentTime = Calendar.getInstance().time.getTimeAsString()
                    ShizukuSettings.updateEndTimeStatisticCurrentFocus(currentTime)
                    ShizukuSettings.removeCurrentFocusTask()
                    this@CountdownService.stopSelf()
                }
            }.start()
            startForeground(NOTIFICATION_ID, createNotification(it.remainingTime))
            return START_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }

    private fun createNotification(remainingTimeMillis: Long): Notification {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel if on Android 8.0 or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                applicationContext.getString(R.string.focus),
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
            .setContentTitle(applicationContext.getString(R.string.title_focus_notification))
            .setContentText(
                "${applicationContext.getString(R.string.msg_focus_notification)} ${
                    remainingTimeMillis.formatMilliseconds(
                        this
                    )
                }"
            )
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