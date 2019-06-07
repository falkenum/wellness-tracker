package com.example.meditationtimer

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import okhttp3.*
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount




class BackupService : Service() {

    override fun onCreate() {
        super.onCreate()



    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

}
