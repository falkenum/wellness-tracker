package com.sjfalken.wellnesstracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.android.synthetic.main.fragment_settings.view.*

class SettingsFragment : BaseFragment() {

    private fun updateSignedInUser() {
        signedInUser.text = (activity!! as MainActivity).signedInAccount?.email ?: "Not signed in"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_settings, container, false)
        val mainActivity = (activity!! as MainActivity)
        rootView.signInOutButton.apply {
            setOnClickListener {
                if (text == getString(R.string.sign_in)) {
                    mainActivity.requestSignIn()
                } else {
                    mainActivity.signOut()
                    text = getString(R.string.sign_in)
                    updateSignedInUser()
                }
            }
            mainActivity.addOnSignInAction {
                text = getString(R.string.sign_out)
                updateSignedInUser()
            }
        }
        rootView.syncButton.setOnClickListener {
            mainActivity.doSync()
        }
        return rootView
    }

    override fun onStart() {
        super.onStart()
        updateSignedInUser()
    }
}