package com.example.meditationtimer

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import java.lang.String.format
import java.util.*

class TimerFragment() : Fragment() {
    private lateinit var timerService: TimerService
    private lateinit var rootView: View
    private var lengthMinutes: Long = 10

    fun setTimerStr(minutes: Long, seconds: Long) {
        rootView.findViewById<TextView>(R.id.timer)?.text =
            format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
    }

    fun onStartClick() {
        setupRunningTimer()
        timerService.startTimer(lengthMinutes, 0)
    }

    fun onStopClick() {
        setupStoppedTimer()
        timerService.stopTimerEarly()
    }

    fun onPlusClick() {
        lengthMinutes += 1
        setTimerStr(lengthMinutes, 0)
    }

    fun onMinusClick() {
        if (lengthMinutes > 1) lengthMinutes -= 1
        setTimerStr(lengthMinutes, 0)
    }

    fun setupRunningTimer() {
        rootView.findViewById<Button>(R.id.startStop)?.apply {
            text = "Stop"
            setOnClickListener { onStopClick() }
        }

        rootView.findViewById<Button>(R.id.plus)?.visibility = View.GONE
        rootView.findViewById<Button>(R.id.minus)?.visibility = View.GONE
    }

    fun setupStoppedTimer() {
        rootView.findViewById<Button>(R.id.startStop)?.apply {
            text = "Start"
            setOnClickListener { onStartClick() }
        }
        rootView.findViewById<Button>(R.id.plus)?.visibility = View.VISIBLE
        rootView.findViewById<Button>(R.id.minus)?.visibility = View.VISIBLE
        setTimerStr(lengthMinutes, 0)

    }


    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // get connection to the timer service and configure
        timerService = (arguments!![BundleKeys.ARG_TIMER_SERVICE_BINDER] as TimerService.TimerBinder).getService()

        rootView = inflater.inflate(R.layout.tab_timer, container, false)
        // setup the plus and minus buttons
        rootView.findViewById<Button>(R.id.plus).setOnClickListener { onPlusClick() }
        rootView.findViewById<Button>(R.id.minus).setOnClickListener { onMinusClick() }

        if (timerService.isRunning) {
            setupRunningTimer()
        }
        else {
            setupStoppedTimer()
        }

        timerService.onTimeChanged = { minutes, seconds ->
            // if the activity is open, update the instant on screen
            activity?.runOnUiThread { setTimerStr(minutes, seconds) }
        }

        timerService.onTimerFinishTasks.add {
            // if the activity is open, update the ui for stopped timer
            activity?.runOnUiThread { setupStoppedTimer() }
        }

        return rootView
    }
}

