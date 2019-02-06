package com.example.meditationtimer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.lang.String.format
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private var lengthMinutes: Long = 10
    private var service : TimerService? = null

    private val timerConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as TimerService.TimerBinder).getService()

            service!!.onTimeChanged = { minutes, seconds ->
                runOnUiThread { setTimerStr(minutes, seconds) }
            }

            service!!.onTimerFinish = {
                playBell()

                runOnUiThread {
                    findViewById<Button>(R.id.minus).visibility = View.VISIBLE
                    findViewById<Button>(R.id.plus).visibility = View.VISIBLE
                    findViewById<Button>(R.id.startStop).text = "Start"
                    resetTimer()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    fun setTimerStr(minutes: Long, seconds: Long) {
        findViewById<TextView>(R.id.timerView).text = format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
    }

    fun playBell() {
        // make sure to start from the beginning
        mediaPlayer.seekTo(0)
        mediaPlayer.start()
    }

    fun resetTimer() {
        findViewById<Button>(R.id.startStop).setOnClickListener {
            (it as Button).apply {
                setOnClickListener { service!!.stopTimer() }
                text = "Stop"
            }

            findViewById<Button>(R.id.minus).visibility = View.GONE
            findViewById<Button>(R.id.plus).visibility = View.GONE

            playBell()
            service!!.startTimer(lengthMinutes, 0)
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
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mediaPlayer = MediaPlayer.create(this, R.raw.bell)

        resetTimer()
    }


    override fun onStart() {
        super.onStart()
        bindService(Intent(this, TimerService::class.java), timerConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(timerConnection)
    }
}
