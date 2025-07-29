package com.stip.stip.iphome.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.util.Pair
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import com.stip.stip.R
import com.stip.stip.databinding.FragmentIpHomeFilledFilterBinding
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class FilledFilterDialogFragment : DialogFragment() {

    private var _binding: FragmentIpHomeFilledFilterBinding? = null
    private val binding get() = _binding!!

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var selectedStartDate: Calendar? = null
    private var selectedEndDate: Calendar? = null

    companion object {
        const val TAG = "FilledFilterDialog"
        const val REQUEST_KEY = "filled_filter_request"
        private const val DATE_PICKER_TAG = "FilledDateRangePicker"
        fun newInstance() = FilledFilterDialogFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIpHomeFilledFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { dismiss() }

        setupChipListeners()
        setupButtonListeners()
        setupDateListeners()
        setInitialState()
    }

    private fun setInitialState() {
        binding.chipTypeAllFilled.isChecked = true
        handleChipGroupSelection(binding.chipgroupTransactionTypeFilled, binding.chipTypeAllFilled)

        binding.chipPeriod1mFilled.isChecked = true
        handleChipGroupSelection(binding.chipgroupDateRangeFilled, binding.chipPeriod1mFilled)
        updateDatesBasedOnChip(binding.chipPeriod1mFilled)
    }

    private fun setupChipListeners() {
        binding.chipgroupTransactionTypeFilled.children.forEach { child ->
            if (child is Chip) {
                child.setOnClickListener {
                    handleChipGroupSelection(binding.chipgroupTransactionTypeFilled, child)
                }
            }
        }

        binding.chipgroupDateRangeFilled.children.forEach { child ->
            if (child is Chip) {
                child.setOnClickListener {
                    handleChipGroupSelection(binding.chipgroupDateRangeFilled, child)
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
            R.id.chip_period_1w_filled -> startDate.add(Calendar.WEEK_OF_YEAR, -1)
            R.id.chip_period_1m_filled -> startDate.add(Calendar.MONTH, -1)
            R.id.chip_period_3m_filled -> startDate.add(Calendar.MONTH, -3)
            R.id.chip_period_6m_filled -> startDate.add(Calendar.MONTH, -6)
            R.id.chip_period_direct_filled -> return
            else -> return
        }

        selectedStartDate = startDate
        selectedEndDate = endDate
        updateDateTextViews()
    }

    private fun updateDateTextViews() {
        binding.textViewStartDateFilled.text = selectedStartDate?.let { dateFormat.format(it.time) } ?: "----.--.--"
        binding.textViewEndDateFilled.text = selectedEndDate?.let { dateFormat.format(it.time) } ?: "----.--.--"
    }

    private fun setupButtonListeners() {
        binding.buttonReset.setOnClickListener {
            setInitialState()
        }

        binding.buttonSearch.setOnClickListener {
            applyFiltersAndReturnResult()
        }
    }

    private fun setupDateListeners() {
        val listener = View.OnClickListener { showDateRangePicker() }
        binding.textViewStartDateFilled.setOnClickListener(listener)
        binding.textViewEndDateFilled.setOnClickListener(listener)
    }

    private fun showDateRangePicker() {
        val selectionPair = selectedStartDate?.timeInMillis?.let { start ->
            selectedEndDate?.timeInMillis?.let { end -> Pair(start, end) }
        } ?: Pair(
            MaterialDatePicker.thisMonthInUtcMilliseconds(),
            MaterialDatePicker.todayInUtcMilliseconds()
        )

        val datePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("조회 기간 선택")
            .setSelection(selectionPair)
            .setTheme(R.style.CustomDateRangePickerTheme)
            .build()

        datePicker.addOnPositiveButtonClickListener {
            val utc = TimeZone.getTimeZone("UTC")
            selectedStartDate = Calendar.getInstance(utc).apply { timeInMillis = it.first }
            selectedEndDate = Calendar.getInstance(utc).apply { timeInMillis = it.second }
            updateDateTextViews()
            binding.chipPeriodDirectFilled.isChecked = true
            handleChipGroupSelection(binding.chipgroupDateRangeFilled, binding.chipPeriodDirectFilled)
        }

        datePicker.show(parentFragmentManager, DATE_PICKER_TAG)
    }

    private fun applyFiltersAndReturnResult() {
        val selectedTypes = mutableListOf<String>()
        if (binding.chipTypeBuyFilled.isChecked) selectedTypes.add("매수")
        if (binding.chipTypeSellFilled.isChecked) selectedTypes.add("매도")


        val startDateString = selectedStartDate?.let { dateFormat.format(it.time) }
        val endDateString = selectedEndDate?.let { dateFormat.format(it.time) }

        val selectedPeriodLabel = when {
            binding.chipPeriod1wFilled.isChecked -> "1주일"
            binding.chipPeriod1mFilled.isChecked -> "1개월"
            binding.chipPeriod3mFilled.isChecked -> "3개월"
            binding.chipPeriod6mFilled.isChecked -> "6개월"
            binding.chipPeriodDirectFilled.isChecked -> {
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

        parentFragmentManager.setFragmentResult(REQUEST_KEY, result)
        dismiss()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.white)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}