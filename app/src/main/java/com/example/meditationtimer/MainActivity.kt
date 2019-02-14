package com.example.meditationtimer

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import java.lang.Exception
import java.lang.String.format
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var serviceIntent: Intent

    private val timerConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val timerServiceBinder = (binder as TimerService.TimerBinder)
            val viewPager = findViewById<ViewPager>(R.id.viewPager)

            // once the service is connected, setup the tabs
            viewPager.adapter = MainPagerAdapter(supportFragmentManager, timerServiceBinder)
            findViewById<TabLayout>(R.id.tabLayout).setupWithViewPager(viewPager)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        serviceIntent = Intent(this, TimerService::class.java)

        // this is creating the service if it does not exist
        startService(serviceIntent)

        // this is creating a connection to the service
        bindService(serviceIntent, timerConnection, 0)
    }
}

val ARG_SERVICE_BINDER = "service binder"

class MainPagerAdapter(fm : FragmentManager, val timerBinder: TimerService.TimerBinder) :
    FragmentPagerAdapter(fm) {

    // timer and history
    override fun getCount(): Int = 2

    override fun getItem(position: Int): Fragment {
        // deciding which layout to use based on the tab position
        return when (position) {
            0 -> TimerFragment().apply {
                arguments = Bundle().apply { putBinder(ARG_SERVICE_BINDER, timerBinder) }
            }
            1 -> HistoryFragment()
            else -> throw Exception("getItem fragment error")
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        // deciding the tab title based on position
        return when (position) {
            0 -> "Timer"
            1 -> "History"
            else -> throw Exception("getPageTitle fragment error")
        }
    }
}

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
        timerService = (arguments!![ARG_SERVICE_BINDER] as TimerService.TimerBinder).getService()

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
            // if the activity is open, update the time on screen
            activity?.runOnUiThread { setTimerStr(minutes, seconds) }
        }

        timerService.onTimerFinish = {
            // if the activity is open, update the ui for stopped timer
            activity?.runOnUiThread { setupStoppedTimer() }
        }

        return rootView
    }
}

class HistoryFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.tab_history, container, false)
    }
}

