package com.sjfalken.wellnesstracker

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.fragment_history.view.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import java.time.LocalDate


class HomeFragmentViewModel : ViewModel() {
    val selectedTypeIndices = MutableLiveData<List<Int>>(List(EntryTypes.getTypes().size) {i-> i})
}

class HomeFragment : BaseFragment(), ViewPager.OnPageChangeListener {

    class EntryTypeSelectDialogFragment : DialogFragment() {
        lateinit var onConfirm : () -> Unit
        private val _selectedTypeIndices = ArrayList<Int>()
        val selectedTypeIndices : List<Int>
            get() = _selectedTypeIndices.toList()

        fun addIndices(indices : List<Int>) {
            _selectedTypeIndices.addAll(indices)
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

            val checkedBoxes = BooleanArray(EntryTypes.getTypes().size) {i ->
                _selectedTypeIndices.contains(i)
            }

            return AlertDialog.Builder(activity!!)
                .setMultiChoiceItems(EntryTypes.getTypes().toTypedArray(), checkedBoxes) {
                    _, which, isChecked ->
                    if (isChecked) {
                        _selectedTypeIndices.add(which)
                    }
                    else if (_selectedTypeIndices.contains(which)) {
                        _selectedTypeIndices.remove(which)
                    }


                }
                .setPositiveButton("Ok") {_, _ ->
                    onConfirm()
                }
                .create()
        }
    }

    companion object {
        const val HISTORY_POS = 0
        const val STATS_POS = 1
    }

    private lateinit var typeDialog: EntryTypeSelectDialogFragment
    private val onTypeSelectedActions = arrayListOf<() -> Unit>()
    private val viewModel get() = ViewModelProvider(this)[HomeFragmentViewModel::class.java]
    val selectedTypes: List<String>
        get() = viewModel.selectedTypeIndices.value!!.map { i -> EntryTypes.getTypes()[i] }



    fun addOnTypesSelectedAction(action : () -> Unit) = onTypeSelectedActions.add(action)

    private fun updateNumTypes() {
        view!!.numEntryTypes.text = selectedTypes.size.toString()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        updateNumTypes()

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

            val args = Bundle()
            val date = view.calendarView.selectedDate
            if (view.homePager.currentItem == HISTORY_POS && date != null) {
                args.putString(NewEntryFragment.ArgumentKeys.DATE, date.toString())
            }
            findNavController().navigate(R.id.newEntryFragment, args, null)
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

        view.changeButton.setOnClickListener {
            typeDialog.show(childFragmentManager, "EntryTypeSelectDialogFragment")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        typeDialog = EntryTypeSelectDialogFragment().apply {
            onConfirm = {
                viewModel.selectedTypeIndices.value = selectedTypeIndices
                updateNumTypes()
                onTypeSelectedActions.forEach { it.invoke() }
            }
            addIndices(viewModel.selectedTypeIndices.value!!)
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