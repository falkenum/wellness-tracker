package com.sjfalken.wellnesstracker

import android.animation.ObjectAnimator
import android.view.Menu
import android.view.ViewGroup
import androidx.fragment.app.Fragment

open class BaseFragment : Fragment() {
    var showTypeTabs = false
    var options : Menu? = null

    override fun onStart() {
        super.onStart()
        ObjectAnimator.ofFloat((view!! as ViewGroup), "translationX", 0f, -100f).apply {
            duration = 1000
//            start()
        }
    }

    override fun onStop() {
        super.onStop()
    }
}