package com.example.meditationtimer

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.lang.String.format
import java.util.*
import kotlin.concurrent.timer

class MainActivity : AppCompatActivity() {

    private var lengthMinutes: Long = 10
    private var timerService : TimerService? = null
    private lateinit var serviceIntent: Intent

    fun onStartClick(view : View) {
        setupRunningTimer()

        timerService!!.startTimer(lengthMinutes, 0)
    }

    fun onStopClick(view : View) {
        setupStoppedTimer()

        timerService!!.stopTimerEarly()
    }

    fun onPlusClick(view : View) {
        lengthMinutes += 1
        setTimerStr(lengthMinutes, 0)
    }

    fun onMinusClick(view : View) {
        if (lengthMinutes > 1) lengthMinutes -= 1
        setTimerStr(lengthMinutes, 0)
    }

    fun setupRunningTimer() {
        setContentView(R.layout.timer_running)
    }

    fun setupStoppedTimer() {
        setContentView(R.layout.timer_stopped)
        setTimerStr(lengthMinutes, 0)
    }

    private val timerConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            timerService = (binder as TimerService.TimerBinder).getService()

            if (timerService!!.isRunning) {
                runOnUiThread { setupRunningTimer() }
            }
            else {
                runOnUiThread { setupStoppedTimer() }
            }

            timerService!!.onTimeChanged = { minutes, seconds ->
                runOnUiThread { setTimerStr(minutes, seconds) }
            }

            timerService!!.onTimerFinish = {
                runOnUiThread { setupStoppedTimer() }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
        }
    }

    fun setTimerStr(minutes: Long, seconds: Long) {
        findViewById<TextView>(R.id.timerView).text =
            format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        serviceIntent = Intent(this, TimerService::class.java)
        startService(serviceIntent)
    }

    override fun onStart() {
        super.onStart()
        bindService(serviceIntent, timerConnection, 0)
    }

    override fun onStop() {
        unbindService(timerConnection)
        super.onStop()
    }

//    override fun onDestroy() {
//        stopService(serviceIntent)
//        super.onDestroy()
//    }
}
