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
    companion object {
        val ARG_TIMER_SERVICE_BINDER = "timer service binder"
    }

    private lateinit var timerServiceIntent: Intent

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
        timerServiceIntent = Intent(this, TimerService::class.java)

        // this is creating the service if it does not exist
        startService(timerServiceIntent)

        Thread {
            RecordDatabase.init(this)
            // this is creating a connection to the service
            // wait for database to init first
            bindService(timerServiceIntent, timerConnection, 0)
        }.start()

    }
}
