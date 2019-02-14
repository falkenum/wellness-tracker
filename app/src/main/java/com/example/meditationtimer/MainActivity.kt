package com.example.meditationtimer

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.support.constraint.ConstraintLayout
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.lang.String.format
import java.util.*

class MainActivity : AppCompatActivity() {

    private var lengthMinutes: Long = 10
    private var timerService : TimerService? = null
    private lateinit var serviceIntent: Intent

    // making a parent view where I can easily modify the three buttons I need
    class TimerLayout(context : Activity) : ConstraintLayout(context) {
        lateinit var plus : Button
        lateinit var minus : Button
        lateinit var startStop : Button
        lateinit var timer : TextView
        init {
            context.layoutInflater.inflate(R.layout.activity_main, this)
            for (i in 0..childCount) when (getChildAt(i).id) {
                R.id.plus -> plus = getChildAt(i) as Button
                R.id.minus -> minus = getChildAt(i) as Button
                R.id.startStop -> startStop = getChildAt(i) as Button
                R.id.timer -> timer = getChildAt(i) as TextView
            }
        }

        fun setTimerStr(minutes: Long, seconds: Long) {
            timer.text = format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
        }
    }
    private lateinit var parent: TimerLayout


    fun onStartClick() {
        setupRunningTimer()

        timerService!!.startTimer(lengthMinutes, 0)
    }

    fun onStopClick() {
        setupStoppedTimer()

        timerService!!.stopTimerEarly()
    }

    fun onPlusClick(view : View) {
        lengthMinutes += 1
        parent.setTimerStr(lengthMinutes, 0)
    }

    fun onMinusClick(view : View) {
        if (lengthMinutes > 1) lengthMinutes -= 1
        parent.setTimerStr(lengthMinutes, 0)
    }

    fun setupRunningTimer() {
        parent.startStop.apply {
            text = "Stop"
            setOnClickListener { onStopClick() }
        }
        parent.setTimerStr(lengthMinutes, 0)
        parent.plus.visibility = View.GONE
        parent.minus.visibility = View.GONE
    }

    fun setupStoppedTimer() {
        parent.startStop.apply {
            text = "Start"
            setOnClickListener { onStartClick() }
        }
        parent.setTimerStr(lengthMinutes, 0)
        parent.plus.visibility = View.VISIBLE
        parent.minus.visibility = View.VISIBLE

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
                runOnUiThread { parent.setTimerStr(minutes, seconds) }
            }

            timerService!!.onTimerFinish = {
                runOnUiThread { setupStoppedTimer() }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parent = TimerLayout(this)
        setContentView(parent)

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

}
