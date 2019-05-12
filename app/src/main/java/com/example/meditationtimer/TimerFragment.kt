package com.example.meditationtimer

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import java.lang.String.format
import java.util.*

class TimerFragment : androidx.fragment.app.Fragment() {
    private var timerService: TimerService? = null
    private lateinit var rootView: View
    private var lengthMinutes: Long = 10

    private val timerConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val timerServiceBinder = (binder as TimerService.TimerBinder)
            timerService = timerServiceBinder.getService()

            setupTimer()

            timerService!!.onTimeChanged = { minutes, seconds ->
                // if the activity is open, update the instant on screen
                activity?.runOnUiThread { setTimerStr(minutes, seconds) }
            }

            timerService!!.onTimerFinishTasks.add {
                // if the activity is open, update the ui for stopped timer
                activity?.runOnUiThread { setupTimer() }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
    }

    private fun setTimerStr(minutes: Long, seconds: Long) {
        rootView.findViewById<TextView>(R.id.timer)?.text =
            format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
    }

    private fun onStartClick() {
        timerService!!.startTimer(lengthMinutes, 0)
        setupTimer()
    }

    private fun onStopClick() {
        timerService!!.stopTimerEarly()
        setupTimer()
    }

    private fun onPlusClick() {
        lengthMinutes += 1
        setTimerStr(lengthMinutes, 0)
    }

    private fun onMinusClick() {
        if (lengthMinutes > 1) lengthMinutes -= 1
        setTimerStr(lengthMinutes, 0)
    }

    private fun setupTimer() {
        var buttonText = "Start"
        var visibility = View.VISIBLE
        var onClick = { onStartClick() }
        setTimerStr(lengthMinutes, 0)

        timerService?.apply {
            if (isRunning) {
                buttonText = "Stop"
                visibility = View.GONE
                onClick = { onStopClick() }
            }
        }

        rootView.findViewById<Button>(R.id.startStop)?.apply {
            text = buttonText
            setOnClickListener { onClick() }
        }
        rootView.findViewById<Button>(R.id.plus)?.visibility = visibility
        rootView.findViewById<Button>(R.id.minus)?.visibility = visibility
    }


    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // get connection to the timer service
        activity!!.bindService(Intent(activity, TimerService::class.java), timerConnection, 0)

        // setup the layout
        rootView = inflater.inflate(R.layout.tab_timer, container, false)
        // setup the plus and minus buttons
        rootView.findViewById<Button>(R.id.plus).setOnClickListener { onPlusClick() }
        rootView.findViewById<Button>(R.id.minus).setOnClickListener { onMinusClick() }

        return rootView
    }

    override fun onDestroy() {
        activity!!.unbindService(timerConnection)
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        setupTimer()
    }
}

