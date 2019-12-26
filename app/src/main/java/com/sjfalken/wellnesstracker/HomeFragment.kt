package com.sjfalken.wellnesstracker

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ListAdapter
import androidx.core.view.get
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*

class HomeFragment : BaseFragment(), ViewPager.OnPageChangeListener {

    class EntryTypeSelectAdapter : BaseAdapter() {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return CheckBox(parent!!.context).apply {
                text = getItem(position) as String
            }
        }

        override fun getItem(position: Int): Any = EntryTypes.getTypes()[position]
        override fun getItemId(position: Int) = 0L
        override fun getCount() = EntryTypes.getTypes().size
    }

    class EntryTypeSelectDialogFragment : DialogFragment() {
        lateinit var onConfirm : (List<String>) -> Unit

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return AlertDialog.Builder(activity!!)
                .setAdapter(EntryTypeSelectAdapter()) {_,_ -> }
                .setPositiveButton("Ok") {dialog, _ ->
                    val listView = (dialog as AlertDialog).listView
                    val selectedTypes = EntryTypes.getTypes().filterIndexed { index, _ ->
                        (listView[index] as CheckBox).isChecked
                    }

                    onConfirm(selectedTypes)
                }
                .create()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    companion object {
        const val HISTORY_POS = 0
        const val STATS_POS = 1
    }

    // TODO
    var selectedTypes: List<String> = listOf(EntryTypes.MEDITATION)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.homePager.apply {
            adapter = object : FragmentPagerAdapter(
                childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
                override fun getItem(position: Int): Fragment {
                    return when (position) {
                        HISTORY_POS -> HistoryFragment()
                        STATS_POS -> StatsFragment()
                        else -> throw Exception("shouldn't get here")
                    }
                }

                override fun getCount(): Int = 2
            }

            addOnPageChangeListener(this@HomeFragment)
        }

        view.fab.setOnClickListener {
            (activity!! as MainActivity).navController.navigate(R.id.newEntryFragment)
        }

        view.bottom_navigation.setOnNavigationItemSelectedListener {
            val pos = when (it.itemId) {
                R.id.historyMenuItem -> HISTORY_POS
                R.id.statsMenuItem -> STATS_POS
                else -> throw Exception("shouldn't get here")
            }

            homePager.setCurrentItem(pos, true)

            true
        }
        val dialog = EntryTypeSelectDialogFragment().apply {
            onConfirm = { selectedTypes_ ->
                selectedTypes = selectedTypes_
                view.numEntryTypes.text = selectedTypes.size.toString()


                // TODO refresh history and stats views

            }
        }

        view.changeButton.setOnClickListener {
            dialog.show(parentFragmentManager, "EntryTypeSelectDialogFragment")
        }
    }

    override fun onPageScrollStateChanged(state: Int) = Unit
    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) = Unit
    override fun onPageSelected(position: Int) {
        bottom_navigation.selectedItemId = when (position) {
            HISTORY_POS -> R.id.historyMenuItem
            STATS_POS -> R.id.statsMenuItem
            else -> throw Exception("shouldn't get here")
        }
    }
}