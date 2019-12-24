package com.sjfalken.wellnesstracker

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.Menu
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.transition.Fade
import androidx.transition.Slide

open class BaseFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Fade()
        exitTransition = Fade()
    }
}