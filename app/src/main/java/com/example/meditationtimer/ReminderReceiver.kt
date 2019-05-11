package com.example.meditationtimer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat.getSystemService

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channelId = "reminder_channel"
        val channelName = "Reminder Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance)

        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.createNotificationChannel(channel)


        val reminderType = (intent.extras?.get(BundleKeys.REMINDER_TYPE) as? String) ?: "[Empty]"
        val reminderId = (intent.extras?.get(BundleKeys.REMINDER_ID) as? Int) ?: 0
        val notification = Notification.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_timer_notif)
            .setContentTitle("Wellness Reminder")
            .setContentIntent(
                PendingIntent.getActivity(context, 0,
                    Intent(context, MainActivity::class.java), 0)
            )
            .setContentText("Reminder to record a $reminderType")
            .setAutoCancel(true)
            .build()


        notifManager.notify(reminderId, notification)
    }
}
