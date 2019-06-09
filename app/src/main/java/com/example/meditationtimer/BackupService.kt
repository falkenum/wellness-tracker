package com.example.meditationtimer

import android.app.Service
import android.content.Intent
import android.os.IBinder

class BackupService : Service() {

    override fun onCreate() {
        super.onCreate()

    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

}
