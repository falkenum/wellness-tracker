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

    private var lengthMinutes: Long = 10
    private var timerService : TimerService? = null
    private lateinit var serviceIntent: Intent
    private lateinit var viewPager: ViewPager

    fun setTimerStr(minutes: Long, seconds: Long) {
        findViewById<TextView>(R.id.timer).text =
            format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
    }


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
        setTimerStr(lengthMinutes, 0)
    }

    fun onMinusClick(view : View) {
        if (lengthMinutes > 1) lengthMinutes -= 1
        setTimerStr(lengthMinutes, 0)
    }

    fun setupRunningTimer() {
        findViewById<Button>(R.id.startStop).apply {
            text = "Stop"
            setOnClickListener { onStopClick() }
        }

        findViewById<Button>(R.id.plus).visibility = View.GONE
        findViewById<Button>(R.id.minus).visibility = View.GONE
    }

    fun setupStoppedTimer() {
        findViewById<Button>(R.id.startStop).apply {
            text = "Start"
            setOnClickListener { onStartClick() }
        }
        findViewById<Button>(R.id.plus).visibility = View.VISIBLE
        findViewById<Button>(R.id.minus).visibility = View.VISIBLE
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = MainPagerAdapter(supportFragmentManager)
        findViewById<TabLayout>(R.id.tabLayout).setupWithViewPager(viewPager)

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

val ARG_WHICH_TAB = "which tab"

class MainPagerAdapter(fm : FragmentManager) : FragmentPagerAdapter(fm) {
    // timer and history
    override fun getCount(): Int = 2

    override fun getItem(position: Int): Fragment {
        val fragment = MainFragment()

        // deciding which layout to use based on the tab position
        fragment.arguments = Bundle().apply {
            val arg = when (position) {
                0 -> R.layout.tab_timer
                1 -> R.layout.tab_history
                else -> throw Exception("getItem fragment error")
            }
            putInt(ARG_WHICH_TAB, arg)
        }

        return fragment
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

class MainFragment() : Fragment() {
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // extract the layout arg and inflate
        return inflater.inflate(arguments!![ARG_WHICH_TAB] as Int,
            container, false)
    }
}

