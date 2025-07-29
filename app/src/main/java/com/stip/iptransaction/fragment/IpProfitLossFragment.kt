package com.stip.stip.iptransaction.fragment

import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.PopupWindow
import android.widget.TextView

import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.stip.stip.iptransaction.adapter.ProfitAdapter
import com.stip.stip.R
import com.stip.stip.ScrollableToTop
import com.stip.stip.databinding.FragmentIpProfitLossBinding
import com.stip.stip.ipinfo.model.ProfitItem
import com.stip.stip.iptransaction.api.IpTransactionService
import com.stip.stip.iptransaction.model.ProfitLossChartData
import com.stip.stip.iptransaction.model.ProfitLossItem
import com.stip.stip.signup.utils.PreferenceUtil
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class IpProfitLossFragment : Fragment(), ScrollableToTop {

    private lateinit var mainViewModel: com.stip.stip.MainViewModel


    private var _binding: FragmentIpProfitLossBinding? = null
    private val binding get() = _binding!!

    private lateinit var profitAdapter: ProfitAdapter // ProfitAdapter 정의 및 초기화 확인 필요

    // 상태 변수
    private var selectedYear = Calendar.getInstance().get(Calendar.YEAR) // 현재 년도로 초기화 (예시)
    private var selectedMonth = Calendar.getInstance().get(Calendar.MONTH) + 1 // 현재 월로 초기화 (1~12)
    private var isAccumulated = true // 차트 표시 기준 (누적/손익)
    private var currentProfitType: String = "" // 현재 선택된 수익률 타입
    private var isDescending = true // 리스트 정렬 순서 (true: 내림차순, false: 오름차순)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIpProfitLossBinding.inflate(inflater, container, false)
        // getString은 context가 필요하므로 onViewCreated에서 호출
        // currentProfitType = getString(R.string.profit_type_weighted) // 여기서 초기화하지 않음
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainViewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[com.stip.stip.MainViewModel::class.java]
        mainViewModel.memberInfo.observe(viewLifecycleOwner) { memberInfo ->
            // TODO: 회원정보를 UI에 반영하는 코드 작성
        }

        // getString 사용은 여기서 안전
        currentProfitType = getString(R.string.profit_type_weighted) // 기본 수익률 타입 설정

        binding.textProfitType.text = currentProfitType // 초기 텍스트 설정
        updateDateTextViews() // 초기 날짜 텍스트 설정

        // 차트 초기화
        initializeChart()

        // UI 컴포넌트 설정
        setupTabs()
        setupProfitTypePopup()
        setupMonthPickers()
        setupChartToggle()
        setupRecyclerView()
        setupSortButton() // 정렬 버튼 설정

        // 초기 데이터 로드
        loadDataForProfitType(currentProfitType) // 차트/요약 데이터 로드
        loadProfitListData() // RecyclerView 데이터 로드
    }


    private fun setupTabs() {
        // 초기에는 기본 탭 상태로 시작
        selectProfitTab(isAccumulated)
        
        // 추후 탭 UI가 추가되면 여기에 클릭 핸들러 설정
        // 현재는 기본값으로 isAccumulated = true로 유지
    }

    private fun setupProfitTypePopup() {
        binding.profitTypeContainer.setOnClickListener {
            val context = requireContext()
            val popupView = layoutInflater.inflate(R.layout.popup_profit_type, null) // 레이아웃 확인 필요
            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )

            // !! 중요: popup_profit_type.xml 에 정의된 TextView ID 확인 필요 !!
            val typeItems = listOf(
                popupView.findViewById<TextView>(R.id.item_type1),
                popupView.findViewById<TextView>(R.id.item_type2),
                popupView.findViewById<TextView>(R.id.item_type3)
            ).filterNotNull()

            typeItems.forEach { item ->
                item.setOnClickListener {
                    val selectedType = item.text.toString()
                    binding.textProfitType.text = selectedType
                    currentProfitType = selectedType
                    loadDataForProfitType(selectedType) // 요약/차트 데이터 다시 로드
                    popupWindow.dismiss()
                }
            }

            // !! 중요: popup_background drawable 리소스 필요 !!
            popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.popup_background))
            popupWindow.isOutsideTouchable = true
            popupWindow.elevation = 12f
            popupWindow.showAsDropDown(binding.profitTypeContainer, 0, 8)
        }
    }

    private fun setupMonthPickers() {
        binding.textProfitMonth.setOnClickListener {
            val context = requireContext()
            // !! 중요: popup_month_picker.xml 레이아웃 필요 !!
            val popupView = layoutInflater.inflate(R.layout.popup_month_picker, null)
            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )

            // !! 중요: popup_month_picker.xml 내부 ID 확인 필요 !!
            val pickerYear = popupView.findViewById<NumberPicker>(R.id.picker_year)
            val pickerMonth = popupView.findViewById<NumberPicker>(R.id.picker_month)
            val btnConfirm = popupView.findViewById<TextView>(R.id.btn_confirm)

            val currentCal = Calendar.getInstance()
            val currentYear = currentCal.get(Calendar.YEAR)

            pickerYear.minValue = 2020 // 시작 년도
            pickerYear.maxValue = currentYear + 10 // 종료 년도 (조정 가능)
            pickerYear.value = selectedYear

            pickerMonth.minValue = 1
            pickerMonth.maxValue = 12
            pickerMonth.value = selectedMonth
            // 월 표시 형식 (01, 02, ...)
            pickerMonth.displayedValues = (1..12).map { String.format("%02d", it) }.toTypedArray()

            btnConfirm.setOnClickListener {
                selectedYear = pickerYear.value
                selectedMonth = pickerMonth.value

                updateDateTextViews() // 날짜 표시 업데이트

                loadDataForProfitType(currentProfitType) // 요약/차트 데이터 로드
                loadProfitListData() // 리스트 데이터 다시 로드

                popupWindow.dismiss()
            }

            popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.popup_background))
            popupWindow.isOutsideTouchable = true
            popupWindow.elevation = 10f
            popupWindow.showAsDropDown(binding.textProfitMonth, 0, 8)
        }
    }

    private fun setupChartToggle() {
        // !! 중요: ic_arrow_down, ic_arrow_up drawable 리소스 필요 !!
        binding.iconGraphArrow.setOnClickListener {
            val isVisible = binding.lineChartProfit.visibility == View.VISIBLE
            binding.lineChartProfit.visibility = if (isVisible) View.GONE else View.VISIBLE
            binding.iconGraphArrow.setImageResource(
                if (isVisible) R.drawable.ic_arrow_down else R.drawable.ic_arrow_up
            )
        }
        // 초기 상태 설정 (예: 보이게)
        binding.lineChartProfit.visibility = View.VISIBLE
        binding.iconGraphArrow.setImageResource(R.drawable.ic_arrow_up)
    }

    private val ID_RECYCLERVIEW_PROFIT = View.generateViewId() // 고유한 ID 생성
    private var cardProfitDetailContainer: ViewGroup? = null

    private fun setupRecyclerView() {
        try {
            val context = context ?: return
            profitAdapter = ProfitAdapter()
            
            // 기존에 추가된 RecyclerView 제거 (재설정 시)
            cardProfitDetailContainer = view?.findViewById(R.id.card_profit_detail_container)
            if (cardProfitDetailContainer == null) {
                Log.e("IpProfitLossFragment", "card_profit_detail_container를 찾을 수 없습니다.")
                return
            }
            
            cardProfitDetailContainer?.findViewById<androidx.recyclerview.widget.RecyclerView>(ID_RECYCLERVIEW_PROFIT)?.let { oldRecyclerView ->
                try {
                    (cardProfitDetailContainer as? ViewGroup)?.removeView(oldRecyclerView)
                } catch (e: Exception) {
                    Log.e("IpProfitLossFragment", "기존 RecyclerView 제거 중 오류: ${e.message}")
                }
            }
            
            // 새 RecyclerView 생성
            val recyclerView = androidx.recyclerview.widget.RecyclerView(context).apply {
                id = ID_RECYCLERVIEW_PROFIT
                layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
                adapter = profitAdapter
                isNestedScrollingEnabled = false
                
                // 구분선 추가
                try {
                    val dividerItemDecoration = androidx.recyclerview.widget.DividerItemDecoration(
                        context,
                        androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
                    )
                    addItemDecoration(dividerItemDecoration)
                } catch (e: Exception) {
                    Log.e("IpProfitLossFragment", "구분선 추가 중 오류: ${e.message}")
                }
            }
            
            // 레이아웃 파라미터 설정
            val layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            
            // card_profit_detail_container에 RecyclerView 추가
            try {
                val headerContainer = cardProfitDetailContainer?.findViewById<ViewGroup>(R.id.container_trade_table_header)
                if (headerContainer != null) {
                    // 헤더 뒤에 RecyclerView 추가
                    val containerParent = headerContainer.parent as? ViewGroup
                    if (containerParent != null) {
                        val index = containerParent.indexOfChild(headerContainer)
                        if (index >= 0) {
                            containerParent.addView(recyclerView, index + 1, layoutParams)
                            Log.d("IpProfitLossFragment", "RecyclerView가 카드 컨테이너에 추가되었습니다.")
                        } else {
                            cardProfitDetailContainer?.addView(recyclerView, layoutParams)
                        }
                    } else {
                        cardProfitDetailContainer?.addView(recyclerView, layoutParams)
                    }
                } else {
                    Log.e("IpProfitLossFragment", "container_trade_table_header를 찾을 수 없습니다.")
                    cardProfitDetailContainer?.addView(recyclerView, layoutParams) // 헤더가 없으면 그냥 추가
                }
            } catch (e: Exception) {
                Log.e("IpProfitLossFragment", "RecyclerView 추가 중 오류: ${e.message}")
                // 마지막 대안으로 직접 추가 시도
                try {
                    (cardProfitDetailContainer as? ViewGroup)?.addView(recyclerView, layoutParams)
                } catch (e2: Exception) {
                    Log.e("IpProfitLossFragment", "최종 RecyclerView 추가 시도 실패: ${e2.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("IpProfitLossFragment", "setupRecyclerView에서 심각한 오류 발생: ${e.message}")
        }
    }

    private fun setupSortButton() {
        // !! 중요: 레이아웃 XML에 ImageView ID 'iconSortArrow' 확인 !!
        updateSortIcon() // 아이콘 초기 상태 설정
        binding.iconSortArrow.setOnClickListener {
            toggleSortOrder()
        }
    }

    // --- 데이터 로딩 및 업데이트 ---

    private fun selectProfitTab(isAccum: Boolean) {
        isAccumulated = isAccum
        
        // 나중에 탭 UI가 구현되면 여기에서 UI 상태 업데이트
        // 지금은 내부 상태만 변경하고 데이터 로드
        
        // 차트 데이터 다시 불러오기
        loadDataForProfitType(currentProfitType)
    }

    // 요약 정보 및 차트 데이터 로드 트리거
    private fun loadDataForProfitType(type: String) {
        // 우선 차트를 초기화하여 기존 데이터 삭제
        binding.lineChartProfit.clear()
        binding.lineChartProfit.invalidate()
        
        // 로그인 여부 확인
        val isLoggedIn = PreferenceUtil.getMemberInfo() != null
        
        if (!isLoggedIn) {
            // 로그인되지 않은 상태일 때 처리
            showNotLoggedInState()
            return
        }
        
        // 로그인 상태이면 차트에 로딩 메세지 표시
        binding.lineChartProfit.setNoDataText("데이터를 불러오는 중...") 
        binding.lineChartProfit.invalidate()
        
        // 요약 정보 API 호출
        loadProfitSummary(type)
        
        // 차트 데이터 API 호출
        loadProfitChartData(type)
    }

    // 더미 데이터 생성 메서드 제거 - 실제 API만 사용
    
    // 실제 API에서 가져온 차트 데이터로 업데이트
    private fun updateChartWithRealData(chartData: ProfitLossChartData) {
        val chart = binding.lineChartProfit
        chart.clear()

        if (chartData.dateLabels.isEmpty() || 
            (isAccumulated && chartData.cumulativeValues.isEmpty()) ||
            (!isAccumulated && chartData.dailyValues.isEmpty())) {
            chart.setNoDataText("차트 데이터가 없습니다.")
            chart.invalidate()
            return
        }
        
        // API에서 받은 데이터로 엔트리 생성
        val entries = mutableListOf<Entry>()
        val values = if (isAccumulated) chartData.cumulativeValues else chartData.dailyValues
        
        // x축은 1부터 시작하는 값 사용
        values.forEachIndexed { index, value ->
            entries.add(Entry((index + 1).toFloat(), value.toFloat()))
        }
        
        setupChartDataSet(entries)
    }
    
    // 차트 데이터 셋 초기화 및 설정
    private fun setupChartDataSet(entries: List<Entry>) {
        val chart = binding.lineChartProfit
        
        val dataSet = LineDataSet(entries, if (isAccumulated) "누적 수익률" else "일별 손익").apply {
            val lineColor = ContextCompat.getColor(requireContext(), 
                if (isAccumulated) R.color.main_point else R.color.color_rise_green)
            color = lineColor
            valueTextSize = 10f
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true) // 그라데이션 채우기 활성화
            
            // 필드 색상 설정
            fillColor = lineColor
            fillAlpha = 30 // 투명도 설정
            
            // 리소스가 있는 경우에는 다음을 사용할 수 있음
            // fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.chart_gradient_fill)
            highLightColor = ContextCompat.getColor(requireContext(), android.R.color.black)
        }

        val lineData = LineData(dataSet)
        chart.data = lineData

        // 차트 외형 및 축 설정
        chart.description.isEnabled = false
        chart.legend.isEnabled = true

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.granularity = 1f
        xAxis.setLabelCount(5, true) // 라벨은 5개만 표시

        val yAxisLeft = chart.axisLeft
        yAxisLeft.setDrawGridLines(true)
        yAxisLeft.gridColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
        yAxisLeft.setDrawAxisLine(true)

        chart.axisRight.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        chart.animateX(500)
        chart.invalidate()
    }

    // RecyclerView 데이터 로드 및 정렬 - 실제 API 호출 기반
    private fun loadProfitListData() {
        // 로그인 여부 확인
        val isLoggedIn = PreferenceUtil.getMemberInfo() != null
        
        if (!isLoggedIn) {
            profitAdapter.submitList(emptyList())
            return
        }
        
        // 로딩 표시 제거됨
        
        // 수익률 타입 가져오기
        val profitType = when(currentProfitType) {
            getString(R.string.profit_type_weighted) -> "weighted"
            getString(R.string.profit_type_time) -> "timeWeighted"
            getString(R.string.profit_type_simple) -> "simple"
            else -> "weighted" // 기본값
        }
        
        // API 호출 실행
        IpTransactionService.getProfitLossItems(selectedYear, selectedMonth, profitType) { profitLossItems, error ->
            if (error != null) {
                Log.e("IpProfitLossFragment", "Error loading profit loss items: ${error.message}")
                activity?.runOnUiThread {
                    // progressBar 참조 제거됨
                    // Error loading profit loss data
                }
                return@getProfitLossItems
            }
            
            activity?.runOnUiThread {
                
                if (profitLossItems != null && profitLossItems.isNotEmpty()) {
                    // API 데이터를 ProfitItem 모델로 변환
                    val profitItems = profitLossItems.map { convertToProfitItem(it) }
                    
                    // 정렬 로직 적용
                    val sortedList = if (isDescending) {
                        profitItems.sortedByDescending { it.dateRaw }
                    } else {
                        profitItems.sortedBy { it.dateRaw } 
                    }
                    
                    // RecyclerView 업데이트
                    profitAdapter.submitList(sortedList)
                } else {
                    // 데이터가 없는 경우
                    profitAdapter.submitList(emptyList())
                    // No profit loss data for this period
                }
            }
        }
    }
    
    // API에서 가져온 ProfitLossItem을 UI에 표시할 ProfitItem으로 변환
    private fun convertToProfitItem(apiItem: ProfitLossItem): ProfitItem {
        // 날짜 형식 변환
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("yy.MM.dd", Locale.getDefault())
        
        val date = try {
            dateFormat.parse(apiItem.date) ?: apiItem.dateRaw
        } catch (e: Exception) {
            apiItem.dateRaw
        }
        
        // 금액 형식화
        val dailyProfitFormatted = String.format("%+,.2f", apiItem.dailyProfit)
        val cumulativeProfitFormatted = String.format("%+,.2f", apiItem.cumulativeProfit)
        val dailyRateFormatted = String.format("%+.2f%%", apiItem.dailyRate)
        val cumulativeRateFormatted = String.format("%+.2f%%", apiItem.cumulativeRate)
        val endingAssetFormatted = String.format("₩%,.2f", apiItem.endingAsset)
        val startingAssetFormatted = String.format("₩%,.2f", apiItem.startingAsset)
        val depositFormatted = String.format("₩%,.2f", apiItem.deposit)
        val withdrawalFormatted = String.format("₩%,.2f", apiItem.withdrawal)
        
        return ProfitItem(
            date = displayFormat.format(date),
            dateRaw = date,
            dailyProfit = dailyProfitFormatted,
            cumulativeProfit = cumulativeProfitFormatted,
            dailyRate = dailyRateFormatted,
            cumulativeRate = cumulativeRateFormatted,
            endingAsset = endingAssetFormatted,
            startingAsset = startingAssetFormatted,
            deposit = depositFormatted,
            withdrawal = withdrawalFormatted
        )
    }

    // 빈 줄

    // --- 로딩 상태 표시 및 비로그인 상태 처리 ---
    
    // 로딩 상태 표시 부분 삭제
    
    private fun showNotLoggedInState() {
        // 차트 비우기
        binding.lineChartProfit.clear()
        binding.lineChartProfit.setNoDataText("로그인이 필요합니다.")
        binding.lineChartProfit.invalidate()
        
        // RecyclerView 비우기
        profitAdapter.submitList(emptyList())
        
        // 금액 0으로 표시
        binding.valuationProfitText.text = "₩0"
        binding.profitRateText.text = "0.0%"
        binding.textAvgValue.text = "₩0"
        
        // 로그인 필요 메시지 표시
        // Login required
    }
    
    // --- API 호출을 통한 실제 데이터 로딩 ---
    
    private fun loadProfitSummary(type: String) {
        val profitType = when(type) {
            getString(R.string.profit_type_weighted) -> "weighted" 
            getString(R.string.profit_type_time) -> "timeWeighted"
            getString(R.string.profit_type_simple) -> "simple"
            else -> "weighted" // 기본값
        }
        
        IpTransactionService.getProfitLossSummary(selectedYear, selectedMonth, profitType) { summary, error ->
            if (error != null) {
                Log.e("IpProfitLossFragment", "Error loading profit summary: ${error.message}")
                activity?.runOnUiThread {
                    // Failed to load rate information
                }
                return@getProfitLossSummary
            }
            
            activity?.runOnUiThread {
                if (summary != null) {
                    // UI 업데이트
                    val totalProfitFormatted = String.format("%,.2f", summary.totalProfit)
                    val profitRateFormatted = String.format("%.2f%%", summary.profitRate)
                    val avgAssetFormatted = String.format("₩%,.2f", summary.averageAsset)
                    
                    binding.valuationProfitText.text = totalProfitFormatted
                    binding.profitRateText.text = profitRateFormatted
                    binding.textAvgValue.text = avgAssetFormatted
                    
                    // 이익/손실에 따른 색상 적용
                    applyProfitColor(totalProfitFormatted)
                }
            }
        }
    }
    
    private fun loadProfitChartData(type: String) {
        val profitType = when(type) {
            getString(R.string.profit_type_weighted) -> "weighted" 
            getString(R.string.profit_type_time) -> "timeWeighted"
            getString(R.string.profit_type_simple) -> "simple"
            else -> "weighted" // 기본값
        }
        
        IpTransactionService.getProfitLossChartData(selectedYear, selectedMonth, profitType) { chartData, error ->
            if (error != null) {
                Log.e("IpProfitLossFragment", "Error loading chart data: ${error.message}")
                return@getProfitLossChartData
            }
            
            activity?.runOnUiThread {
                if (chartData != null) {
                    updateChartWithRealData(chartData)
                }
            }
        }
    }

    // 상단 날짜 TextView 업데이트
    private fun updateDateTextViews() {
        val calendar = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth - 1, 1)
        }
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val monthFormat = getString(R.string.date_month_format, selectedYear, selectedMonth)
        val periodFormat = getString(R.string.date_range_format, monthFormat, monthFormat, lastDay)

        binding.textProfitMonth.text = monthFormat
        binding.textProfitPeriod.text = periodFormat
    }

    private fun getDateRangeForSelectedMonth(): Pair<String, String> {
        val start = String.format("%04d-%02d-01", selectedYear, selectedMonth)
        val calendar = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth - 1, 1)
        }
        val endDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val end = String.format("%04d-%02d-%02d", selectedYear, selectedMonth, endDay)
        return Pair(start, end)
    }


    // 선택된 월의 일 수 반환
    private fun selectedDayCount(): Int {
        val cal = Calendar.getInstance().apply { set(selectedYear, selectedMonth - 1, 1) }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // 수익/손실에 따라 텍스트 색상 적용
    private fun applyProfitColor(value: String) {
        // !! 중요: color_fall, color_rise color 리소스 필요 !!
        val isLoss = value.trim().startsWith("-")
        val colorRes = if (isLoss) R.color.color_fall else R.color.color_rise
        val color = ContextCompat.getColor(requireContext(), colorRes)
        binding.valuationProfitText.setTextColor(color)
        binding.profitRateText.setTextColor(color)
    }

    // 정렬 순서 변경
    private fun toggleSortOrder() {
        isDescending = !isDescending // 상태 반전
        updateSortIcon() // 아이콘 업데이트
        loadProfitListData() // 리스트 새로고침 (정렬 적용)
    }

    // 정렬 아이콘 업데이트
    // 정렬 아이콘 색상만 업데이트 (아이콘 모양은 유지)
    private fun updateSortIcon() {
        try {
            val colorRes = if (isDescending) R.color.main_point else R.color.trade_header_text
            binding.iconSortArrow?.apply {
                // 아이콘 리소스는 그대로, 색상만 변경
                setColorFilter(ContextCompat.getColor(requireContext(), colorRes), PorterDuff.Mode.SRC_IN)
            }
        } catch (e: Exception) {
            Log.e("IpProfitLossFragment", "Error updating sort icon color. Check resources.", e)
        }
    }




    // --- ScrollableToTop 인터페이스 구현 ---
    // 차트 초기화 메서드
    private fun initializeChart() {
        val chart = binding.lineChartProfit
        
        // 차트 기본 외형 설정
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        
        // 업데이트 아이콘 비활성화
        chart.isAutoScaleMinMaxEnabled = true
        
        // X축 설정
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.granularity = 1f
        xAxis.setLabelCount(5, true) // 라벨은 5개만 표시
        
        // Y축 설정
        val yAxisLeft = chart.axisLeft
        yAxisLeft.setDrawGridLines(true)
        yAxisLeft.gridColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
        yAxisLeft.setDrawAxisLine(true)
        
        // 우측 Y축 비활성화
        chart.axisRight.isEnabled = false
        
        // 로그인 여부에 따라 차트 초기 상태 설정
        if (PreferenceUtil.getMemberInfo() == null) {
            chart.setNoDataText("로그인이 필요합니다.")
        } else {
            chart.setNoDataText("차트 데이터가 없습니다.")
        }
        
        // 차트 리프레시
        chart.invalidate()
    }
    
    override fun scrollToTop() {
        // !! 중요: 레이아웃 XML에 ScrollView ID 'scrollViewProfit' 확인 !!
        binding.scrollViewProfit?.smoothScrollTo(0, 0) // Null 체크 추가
    }

    // --- 생명주기: View 파괴 시 ---
    override fun onDestroyView() {
        super.onDestroyView()
        binding.lineChartProfit.clear() // 차트 리소스 정리
        _binding = null // 메모리 누수 방지를 위해 바인딩 참조 해제 (매우 중요)
    }

    // --- Companion Object (Fragment 인스턴스 생성) ---
    companion object {
        @JvmStatic
        fun newInstance(): IpProfitLossFragment = IpProfitLossFragment()
    }
}