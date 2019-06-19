package com.sjfalken.wellnesstracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        context.startService(Intent(context, BellService::class.java))
        // TODO cancel timer notification here instead
    }
}
