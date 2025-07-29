package com.stip.stip.iptransaction.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.util.Pair
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.stip.stip.MainActivity
import com.stip.stip.R
import com.stip.stip.databinding.FragmentInvestmentFilterBinding
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class InvestmentFilterFragment : Fragment() {

    private lateinit var mainViewModel: com.stip.stip.MainViewModel


    private var _binding: FragmentInvestmentFilterBinding? = null
    private val binding get() = _binding!!

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var selectedStartDate: Calendar? = null
    private var selectedEndDate: Calendar? = null

    companion object {
        private const val DATE_PICKER_TAG = "InvestmentDateRangePicker"
        @JvmStatic
        fun newInstance() = InvestmentFilterFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInvestmentFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[com.stip.stip.MainViewModel::class.java]
        mainViewModel.memberInfo.observe(viewLifecycleOwner) { memberInfo ->
            // TODO: 회원정보를 UI에 반영하는 코드 작성
        }

        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        setupChipListeners()
        setupButtonListeners()
        setupDateListeners()
        setInitialState()
    }

    private fun setInitialState() {
        binding.chipTypeAll.isChecked = true
        handleChipGroupSelection(binding.chipgroupTransactionType, binding.chipTypeAll)

        binding.chipPeriod1m.isChecked = true
        handleChipGroupSelection(binding.chipgroupDateRange, binding.chipPeriod1m)
        updateDatesBasedOnChip(binding.chipPeriod1m)
    }

    private fun setupChipListeners() {
        binding.chipgroupTransactionType.children.forEach { child ->
            if (child is Chip) {
                child.setOnClickListener {
                    handleChipGroupSelection(binding.chipgroupTransactionType, child)
                }
            }
        }

        binding.chipgroupDateRange.children.forEach { child ->
            if (child is Chip) {
                child.setOnClickListener {
                    handleChipGroupSelection(binding.chipgroupDateRange, child)
                    updateDatesBasedOnChip(child)
                }
            }
        }
    }

    private fun handleChipGroupSelection(chipContainer: ViewGroup, selectedChip: Chip) {
        chipContainer.children.forEach { child ->
            if (child is Chip && child != selectedChip) {
                child.isChecked = false
            }
        }
        if (!selectedChip.isChecked) {
            selectedChip.isChecked = true
        }
    }

    private fun updateDatesBasedOnChip(selectedChip: Chip) {
        val utc = TimeZone.getTimeZone("UTC")
        val endDate = Calendar.getInstance(utc)
        val startDate = Calendar.getInstance(utc)

        when (selectedChip.id) {
            R.id.chip_period_1w -> startDate.add(Calendar.WEEK_OF_YEAR, -1)
            R.id.chip_period_1m -> startDate.add(Calendar.MONTH, -1)
            R.id.chip_period_3m -> startDate.add(Calendar.MONTH, -3)
            R.id.chip_period_6m -> startDate.add(Calendar.MONTH, -6)
            R.id.chip_period_direct -> return
            else -> return
        }

        selectedStartDate = startDate
        selectedEndDate = endDate
        updateDateTextViews()
    }

    private fun updateDateTextViews() {
        binding.textViewStartDate.text = selectedStartDate?.let { dateFormat.format(it.time) } ?: "----.--.--"
        binding.textViewEndDate.text = selectedEndDate?.let { dateFormat.format(it.time) } ?: "----.--.--"
    }

    private fun setupButtonListeners() {
        binding.buttonReset.setOnClickListener {
            resetFilters()
        }

        binding.buttonSearch.setOnClickListener {
            applyFiltersAndReturnResult()
        }
    }

    private fun setupDateListeners() {
        val listener = View.OnClickListener { showDateRangePicker() }
        binding.textViewStartDate.setOnClickListener(listener)
        binding.textViewEndDate.setOnClickListener(listener)
    }

    private fun showDateRangePicker() {
        val selectionPair = selectedStartDate?.timeInMillis?.let { start ->
            selectedEndDate?.timeInMillis?.let { end -> Pair(start, end) }
        } ?: Pair(
            MaterialDatePicker.thisMonthInUtcMilliseconds(),
            MaterialDatePicker.todayInUtcMilliseconds()
        )

        val datePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.date_range_picker_title))
            .setSelection(selectionPair)
            .setTheme(R.style.CustomDateRangePickerTheme)
            .build()

        datePicker.addOnPositiveButtonClickListener {
            val utc = TimeZone.getTimeZone("UTC")
            selectedStartDate = Calendar.getInstance(utc).apply { timeInMillis = it.first }
            selectedEndDate = Calendar.getInstance(utc).apply { timeInMillis = it.second }
            updateDateTextViews()
            binding.chipPeriodDirect.isChecked = true
            handleChipGroupSelection(binding.chipgroupDateRange, binding.chipPeriodDirect)
        }

        datePicker.show(parentFragmentManager, DATE_PICKER_TAG)
    }

    private fun applyFiltersAndReturnResult() {
        val selectedTypes = mutableListOf<String>()
        if (binding.chipTypeBuy.isChecked) selectedTypes.add("\uB9E4\uC218")
        if (binding.chipTypeSell.isChecked) selectedTypes.add("\uB9E4\uB3C4")

        val startDateString = selectedStartDate?.let { dateFormat.format(it.time) }
        val endDateString = selectedEndDate?.let { dateFormat.format(it.time) }

        val selectedPeriodLabel = when {
            binding.chipPeriod1w.isChecked -> "1\uC8FC\uC77C"
            binding.chipPeriod1m.isChecked -> "1\uAC1C\uC6D4"
            binding.chipPeriod3m.isChecked -> "3\uAC1C\uC6D4"
            binding.chipPeriod6m.isChecked -> "6\uAC1C\uC6D4"
            binding.chipPeriodDirect.isChecked -> {
                val start = startDateString ?: "-"
                val end = endDateString ?: "-"
                "$start ~ $end"
            }
            else -> ""
        }

        val result = Bundle().apply {
            putStringArrayList("filterTypes", ArrayList(selectedTypes))
            putString("filterStartDate", startDateString)
            putString("filterEndDate", endDateString)
            putString("filterPeriodLabel", selectedPeriodLabel)
        }

        parentFragmentManager.setFragmentResult("investmentFilterResult", result)
        parentFragmentManager.popBackStack()
    }

    private fun resetFilters() {
        setInitialState()
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.hideHeaderAndTabs()
    }

    override fun onPause() {
        super.onPause()
        (activity as? MainActivity)?.showHeader()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}