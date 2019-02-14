package com.example.meditationtimer

import android.content.ComponentName
import android.content.Context
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

class MainActivity : AppCompatActivity() {

    private var lengthMinutes: Long = 10
    private var timerService : TimerService? = null

    private val timerConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            timerService = (binder as TimerService.TimerBinder).getService()

            timerService!!.onTimeChanged = { minutes, seconds ->
                runOnUiThread { setTimerStr(minutes, seconds) }
            }

            timerService!!.onTimerFinish = {
                runOnUiThread {
                    findViewById<Button>(R.id.minus).visibility = View.VISIBLE
                    findViewById<Button>(R.id.plus).visibility = View.VISIBLE
                    findViewById<Button>(R.id.startStop).text = "Start"
                    resetTimer()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
        }
    }

    fun setTimerStr(minutes: Long, seconds: Long) {
        findViewById<TextView>(R.id.timerView).text = format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
    }

    fun resetTimer() {
        findViewById<Button>(R.id.startStop).setOnClickListener {
            (it as Button).apply {
                setOnClickListener { timerService!!.stopTimerEarly() }
                text = "Stop"
            }

            findViewById<Button>(R.id.minus).visibility = View.GONE
            findViewById<Button>(R.id.plus).visibility = View.GONE

            timerService!!.startTimer(lengthMinutes, 0)
        }


        findViewById<Button>(R.id.minus).setOnClickListener {
            if (lengthMinutes > 1) lengthMinutes -= 1
            setTimerStr(lengthMinutes, 0)
        }

        findViewById<Button>(R.id.plus).setOnClickListener {
            lengthMinutes += 1
            setTimerStr(lengthMinutes, 0)
        }

        setTimerStr(lengthMinutes, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v("MainActivity", "creating")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindService(Intent(this, TimerService::class.java), timerConnection, Context.BIND_AUTO_CREATE)

        resetTimer()
    }

    override fun onDestroy() {
        unbindService(timerConnection)
        super.onDestroy()
    }
}
