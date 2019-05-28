package com.example.meditationtimer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout

class HomeFragment : Fragment(), TabLayout.OnTabSelectedListener {
    override fun onTabReselected(tab: TabLayout.Tab?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private lateinit var rootView : View
    private lateinit var dataView : RecordDataView
    private lateinit var spinner : Spinner

//    private fun setInputType(type : String) {
//        dataView = RecordTypes.getConfig(type).getDataInputView(activity!!)
//        rootView.findViewById<FrameLayout>(R.id.dataInputHolder).apply {
//            removeAllViews()
//            addView(dataView)
//        }
//    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_home, container, false)

        rootView.findViewById<Button>(R.id.newLogEntryButton).setOnClickListener {
            findNavController().navigate(R.id.newEntryFragment)
        }

        activity!!.findViewById<TabLayout>(R.id.tabLayout).run {
            clearOnTabSelectedListeners()
            addOnTabSelectedListener(this@HomeFragment)
        }

        return rootView
    }

    companion object SavedInstanceItems {
        const val SELECTED_ITEM_POS = "com.example.meditationtimer.SELECTED_ITEM_POS"
    }
}