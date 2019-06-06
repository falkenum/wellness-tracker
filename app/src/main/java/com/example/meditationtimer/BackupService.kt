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
    private val httpClient = OkHttpClient()

    override fun onCreate() {
        super.onCreate()

        val url = "https://reqres.in/api/users/2"

        val request = Request.Builder()
            .url(url)
            .build()

        Thread {
            val response = httpClient.newCall(request).execute()
            Log.d("debugging", response.body!!.string())
        }.start()


        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(Scopes.DRIVE_APPFOLDER))
            .build()

        val googleClient = GoogleSignIn.getClient(this, gso)
        
        val account = GoogleSignIn.getLastSignedInAccount(this)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

}
