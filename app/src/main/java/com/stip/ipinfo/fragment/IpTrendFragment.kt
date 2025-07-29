package com.stip.stip.ipinfo.fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.transition.AutoTransition
import android.transition.TransitionManager
// import android.view.View 제거 - 이미 다른 View import가 있음
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.stip.stip.ipinfo.IpTrendMiniChartAdapter
import com.stip.stip.R
import com.stip.stip.databinding.FragmentIpTrendBinding
import com.stip.stip.ipinfo.adapter.RiseIpAdapter
import com.stip.stip.ipinfo.adapter.StipIndexAdapter
import com.stip.stip.ipinfo.FxRateDataHolder
import com.stip.stip.ipinfo.model.FxRateItem
import com.stip.stip.ipinfo.model.IpTrendMiniChartItem
import com.stip.stip.ipinfo.model.RiseIpItem
import com.stip.stip.ipinfo.model.StipIndexItem
import com.stip.stip.ipinfo.api.IpInfoApiService
import com.stip.stip.ipinfo.model.FxRateData
import com.stip.stip.ipinfo.model.StipIndexData
import com.stip.stip.iphome.TradingDataHolder
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random
import java.math.BigDecimal
import java.math.RoundingMode

// --- Fragment Class Declaration ---
class IpTrendFragment : Fragment() {

    private lateinit var mainViewModel: com.stip.stip.MainViewModel


    private var _binding: FragmentIpTrendBinding? = null
    private val binding get() = _binding!!
    private var stipAngle = 0.0
    private var etipAngle = 0.0

    // Current selected time period for the rising IP list
    private var currentPeriod = "1W" // Default to 1 week

    // Coroutine Jobs
    private var stipUpdateJob: Job? = null
    private var etipUpdateJob: Job? = null
    private var indexUpdateJob: Job? = null // STIP 지수 업데이트 Job
    // ViewPager2용 미니차트 아이템 리스트와 어댑터
    private lateinit var miniChartItems: MutableList<IpTrendMiniChartItem>
    private lateinit var miniChartAdapter: IpTrendMiniChartAdapter


    // Adapters & Data Lists
    private lateinit var riseIpAdapter: RiseIpAdapter // RiseIpAdapter가 정의되어 있다고 가정
    private lateinit var stipIndexAdapter: StipIndexAdapter // StipIndexAdapter가 정의되어 있다고 가정
    private val stipIndexList = mutableListOf<StipIndexItem>() // STIP 지수 데이터 리스트 (Mutable)

    // Chart Data
    private val stipEntries = mutableListOf<Entry>()
    private var stipX = 0f
    private val stipBasePrice = 1000f
    private var stipLastPrice = stipBasePrice

    private val etipEntries = mutableListOf<Entry>()
    private var etipX = 0f
    private val etipBasePrice = 500f
    private var etipLastPrice = etipBasePrice

    // Dot Indicator
    private var currentDotIndex = 0
    private val dots by lazy { // Lazy initialization
        listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
    }

    // --- Constants ---
    companion object {
        private const val STIP_UPDATE_INTERVAL_MS = 1000L // STIP/ETIP 가격 업데이트 간격
        private const val ETIP_UPDATE_INTERVAL_MS = 2000L // STIP/ETIP 가격 업데이트 간격
        private const val INDEX_UPDATE_INTERVAL_MS = 5000L // STIP 지수 업데이트 간격 (예: 3초)
        private const val MAX_CHART_ENTRIES = 50 // 차트 데이터 최대 개수
        private const val CHART_VISIBLE_X_RANGE = 30f // 차트 X축 가시 범위
        private const val WEBSITE_URL = "http://stipvelation.com"

        @JvmStatic
        fun newInstance(): IpTrendFragment = IpTrendFragment()
    }
    // --- Constants End ---

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIpTrendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[com.stip.stip.MainViewModel::class.java]
        mainViewModel.memberInfo.observe(viewLifecycleOwner) { memberInfo ->
            // TODO: 회원정보를 UI에 반영하는 코드 작성
        }

        super.onViewCreated(view, savedInstanceState)

        // 1. 데이터 초기화
        initializeStipIndexData() // STIP 지수 리스트 데이터 채우기

        // ✅ 여기에 미니차트 ViewPager2 초기화 추가
        setupMiniChartViewPager()

        // 2. RecyclerView 설정
        setupRiseIpRecyclerView()      // 고정된 IP 순위 리사이클러뷰 설정
        setupStipIndexRecyclerView()   // 동적 STIP 지수 리사이클러뷰 설정

        // 3. 기타 UI 설정
        setupLogoClickListener()
        setupTimeRangeButtons()
        setupDateButtons()
        setupViewPagerListener()
        setupExpandCollapseButton() // 상승률 상위 IP 접기/펼치기 버튼 설정
        setupStipIndexExpandButton() // STIP Index 접기/펼치기 버튼 설정
        
        // 환율 데이터 로드
        loadFxRateData()

        // 4. 업데이트 루프 시작
        startPriceUpdates() // STIP/ETIP 가격 & 차트 업데이트 시작
        startIndexUpdates() // STIP 지수 업데이트 시작


    }



    // --- Initialization Functions ---

    private fun setupLogoClickListener() {
        try {
            binding.stipMainLogo.setOnClickListener {
                openWebsite(WEBSITE_URL)
            }
        } catch (e: NullPointerException) {
            Log.e("IpTrendFragment", "ID 'stip_main_logo' not found in layout.", e)
        }
    }



    private fun setupDotClickListeners() {
        dots.forEachIndexed { index, dot ->
            dot.setOnClickListener { onDotClick(index) }
        }
        updateDotState(currentDotIndex)

    }

    private fun initializeStipIndexData() {
        stipIndexList.clear() // 기존 데이터 삭제
        
        // 빈 상태로 시작
        val context = requireContext()
        
        // API에서 실제 데이터 가져오기
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = IpInfoApiService.create()
                val response = withContext(Dispatchers.IO) {
                    apiService.getStipIndices()
                }
                
                if (response.isSuccessful && response.body() != null) {
                    val indexDataList = response.body()!!.data
                    if (indexDataList.isNotEmpty()) {
                        updateStipIndexWithApiData(indexDataList)
                    } else {
                        // API 결과가 비어있으면 표준 인덱스만 표시
                        activity?.runOnUiThread {
                            setEmptyStipIndexData()
                        }
                    }
                } else {
                    // API 호출 실패 시 표준 인덱스만 표시
                    Log.e("IpTrendFragment", "STIP 인덱스 데이터 가져오기 실패: ${response.code()}")
                    activity?.runOnUiThread {
                        setEmptyStipIndexData()
                    }
                }
            } catch (e: Exception) {
                // 예외 발생 시 표준 인덱스만 표시
                Log.e("IpTrendFragment", "STIP 인덱스 API 호출 오류", e)
                activity?.runOnUiThread {
                    setEmptyStipIndexData()
                }
            }
        }
    }
    
    // 빈 STIP 인덱스 데이터 설정 (실패 시)
    private fun setEmptyStipIndexData() {
        val context = requireContext()
        stipIndexList.clear() // 기존 데이터 삭제
        
        // 인덱스 이름만 추가하고 값은 모두 0.00%로 설정
        stipIndexList.add(
            StipIndexItem(context.getString(R.string.stip_index_total), "0.00%", 0f, 0f)
        )
        stipIndexList.add(
            StipIndexItem(context.getString(R.string.stip_index_ai), "0.00%", 0f, 0f)
        )
        stipIndexList.add(
            StipIndexItem(context.getString(R.string.stip_index_global), "0.00%", 0f, 0f)
        )
        stipIndexList.add(
            StipIndexItem(context.getString(R.string.stip_index_entertainment), "0.00%", 0f, 0f)
        )
        stipIndexList.add(
            StipIndexItem(context.getString(R.string.stip_index_franchise), "0.00%", 0f, 0f)
        )
        
        if (::stipIndexAdapter.isInitialized) {
            stipIndexAdapter.notifyDataSetChanged()
        }
    }
    
    private fun updateStipIndexWithApiData(indexDataList: List<StipIndexData>) {
        if (indexDataList.size < stipIndexList.size) return
        
        for (i in indexDataList.indices) {
            if (i < stipIndexList.size) {
                stipIndexList[i] = StipIndexItem(
                    title = stipIndexList[i].title,
                    percentageString = indexDataList[i].changePercent,
                    currentValue = indexDataList[i].currentValue,
                    baseValue = indexDataList[i].baseValue
                )
            }
        }
        
        activity?.runOnUiThread {
            if (::stipIndexAdapter.isInitialized) {
                stipIndexAdapter.notifyDataSetChanged()
            }
        }
    }
    
    // 퍼센트 계산 및 문자열 업데이트 헬퍼 함수
    private fun updateItemPercentage(item: StipIndexItem) {
        val percentage = if (item.baseValue != 0f) {
            ((item.currentValue - item.baseValue) / item.baseValue) * 100f
        } else {
            0f // 기준값이 0이면 0% 처리
        }
        // String.format에서 Locale.US를 사용하여 소수점 '.' 보장
        item.percentageString = String.Companion.format(Locale.US, "%+.2f%%", percentage)
    }

    // --- Coroutine Price Updates (STIP/ETIP) ---

    private fun startPriceUpdates() {
        stipUpdateJob?.cancel()
        etipUpdateJob?.cancel()

        stipUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                updateStipData()
                delay(STIP_UPDATE_INTERVAL_MS)
            }
        }

        etipUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                updateEtipData()
                delay(ETIP_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun updateStipData() {
        if (!::miniChartItems.isInitialized || !::miniChartAdapter.isInitialized) return

        // API 데이터가 있는 경우 해당 데이터 사용
        try {
            val apiService = IpInfoApiService.create()
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // 실제 API 호출로 데이터 가져오기
                    // 예시: 데이터가 있으면 업데이트
                    if (TradingDataHolder.ipListingItems.isNotEmpty()) {
                        activity?.runOnUiThread {
                            // 실제 구현에서는 API 응답 데이터로 차트 업데이트
                            // 데이터가 있으면 여기서 miniChartItems 업데이트
                            // 데이터 변경 후 어댑터에 알림
                            miniChartAdapter.notifyDataSetChanged()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("IpTrendFragment", "STIP 데이터 업데이트 실패", e)
                }
            }
        } catch (e: Exception) {
            Log.e("IpTrendFragment", "STIP 데이터 업데이트 오류", e)
        }
    }

    private fun updateEtipData() {
        if (!::miniChartItems.isInitialized || !::miniChartAdapter.isInitialized) return

        val currentIndex = binding.priceChartViewpager.currentItem
        if (currentIndex >= miniChartItems.size) return

        // API 데이터가 있는 경우 해당 데이터 사용
        try {
            val apiService = IpInfoApiService.create()
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // 실제 API 호출로 데이터 가져오기
                    // 예시: 데이터가 있으면 업데이트
                    if (TradingDataHolder.ipListingItems.isNotEmpty()) {
                        activity?.runOnUiThread {
                            // 실제 구현에서는 API 응답 데이터로 차트 업데이트
                            // 데이터가 있으면 현재 선택된 아이템 업데이트
                            miniChartAdapter.notifyItemChanged(currentIndex)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("IpTrendFragment", "ETIP 데이터 업데이트 실패", e)
                }
            }
        } catch (e: Exception) {
            Log.e("IpTrendFragment", "ETIP 데이터 업데이트 오류", e)
        }
    }


    // --- Coroutine Index Updates (STIP Index List) ---

    private fun startIndexUpdates() {
        indexUpdateJob?.cancel() // 기존 Job 취소

        indexUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                try {
                    updateStipIndexData() // 지수 데이터 업데이트 함수 호출
                    loadTopRisingIps() // 상승률 상위 IP 데이터도 주기적으로 업데이트
                    loadFxRateData() // 환율 데이터도 주기적으로 업데이트
                    delay(INDEX_UPDATE_INTERVAL_MS) // 설정된 간격만큼 대기
                } catch (e: Exception) {
                    Log.e("StartIndexUpdates", "Error in index update loop", e)
                    break // 오류 발생 시 루프 중단 (안전 조치)
                }
            }
        }
    }

    // STIP 지수 데이터 업데이트 로직
    private fun updateStipIndexData() {
        // 어댑터가 초기화되었는지, binding이 null이 아닌지 확인
        if (!::stipIndexAdapter.isInitialized || _binding == null) return
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = IpInfoApiService.create()
                val response = withContext(Dispatchers.IO) {
                    apiService.getStipIndices()
                }
                
                if (response.isSuccessful && response.body() != null) {
                    val indexDataList = response.body()!!.data
                    if (indexDataList.isNotEmpty()) {
                        updateStipIndexWithApiData(indexDataList)
                    }
                } else {
                    Log.e("IpTrendFragment", "STIP 인덱스 데이터 업데이트 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("IpTrendFragment", "STIP 인덱스 API 업데이트 오류", e)
                
                // API 호출 실패 시 기존 데이터에 작은 변동 추가 (폴백)
                stipIndexList.forEachIndexed { index, item ->
                    val change = (Random.Default.nextFloat() - 0.5f) * (item.baseValue * 0.01f)
                    val previousPercentageString = item.percentageString
                    item.currentValue += change
                    updateItemPercentage(item)
                    
                    if (previousPercentageString != item.percentageString) {
                        activity?.runOnUiThread {
                            if (_binding != null && ::stipIndexAdapter.isInitialized && index < stipIndexAdapter.itemCount) {
                                try {
                                    stipIndexAdapter.notifyItemChanged(index)
                                } catch (e: Exception) {
                                    Log.e("UpdateIndexData", "Error notifying item change", e)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        /**
     * 접기/펼치기 버튼 설정 - 상승률 상위 IP 섹션
     */
    private fun setupExpandCollapseButton() {
        // 초기 상태는 펼쳐진 상태로 설정
        var isContentExpanded = true
        
        binding.expandCollapseButton.setOnClickListener {
            isContentExpanded = !isContentExpanded
            
            // 콘텐츠 접기/펼치기
            binding.collapsibleContent.visibility = if (isContentExpanded) android.view.View.VISIBLE else android.view.View.GONE
            
            // 버튼 아이콘 변경
            val iconResource = if (isContentExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            binding.expandCollapseButton.setImageResource(iconResource)
            
            // 애니메이션 효과 (선택적)
            val transition = AutoTransition()
            transition.duration = 300
            TransitionManager.beginDelayedTransition(binding.risingIpCard, transition)
        }
    }
    
    /**
     * 접기/펼치기 버튼 설정 - STIP Index 섹션
     */
    private fun setupStipIndexExpandButton() {
        // 초기 상태는 펼쳐진 상태로 설정
        var isContentExpanded = true
        
        binding.stipIndexExpandButton.setOnClickListener {
            isContentExpanded = !isContentExpanded
            
            // 콘텐츠 접기/펼치기
            binding.stipIndexCollapsibleContent.visibility = if (isContentExpanded) android.view.View.VISIBLE else android.view.View.GONE
            
            // 버튼 아이콘 변경
            val iconResource = if (isContentExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            binding.stipIndexExpandButton.setImageResource(iconResource)
            
            // 애니메이션 효과
            val transition = AutoTransition()
            transition.duration = 300
            TransitionManager.beginDelayedTransition(binding.stipIndexCard, transition)
        }
    }
    
    /**
     * 날짜 범위 버튼(1주, 1개월 등) 설정
     */
    private fun setupDateButtons() {
        val dateButtons = listOf(
            binding.date1month,
            binding.date3months,
            binding.date6months,
            binding.date1year
        )

        dateButtons.forEachIndexed { index, chip ->
            chip.setOnClickListener {
                selectDateButton(chip, dateButtons)
                
                // 모든 Chip의 텍스트 색상 재설정
                dateButtons.forEach { btn ->
                    // 선택된 Chip은 흰색, 아닌 것은 회색 텍스트
                    if (btn.isChecked) {
                        btn.setTextColor(Color.WHITE)
                    } else {
                        btn.setTextColor(Color.parseColor("#808080"))
                    }
                }
                
                // Update current period based on selected button
                currentPeriod = when(index) {
                    0 -> "1M"  // 1개월
                    1 -> "3M"  // 3개월
                    2 -> "6M"  // 6개월
                    3 -> "1Y"  // 1년
                    else -> "1M"
                }
                
                // Reload IP data with new period
                loadTopRisingIps(currentPeriod)
            }
            
            // 초기 텍스트 색상 설정
            if (chip.isChecked) {
                chip.setTextColor(Color.WHITE)
            } else {
                chip.setTextColor(Color.parseColor("#808080"))
            }
        }
        
        // 기본 선택된 버튼의 텍스트 색상을 흰색으로 설정
        dateButtons.find { it.isChecked }?.setTextColor(Color.WHITE)
        
        // 기본값으로 첫 번째 버튼(1개월) 선택
        selectDateButton(dateButtons[0], dateButtons)
        currentPeriod = "1M"
    }
    
    /**
     * 날짜 버튼 선택 상태 업데이트
     */
    private fun selectDateButton(selectedButton: TextView, allButtons: List<TextView>) {
        allButtons.forEach { button ->
            if (button == selectedButton) {
                button.setBackgroundResource(R.drawable.datebox_selected)
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_text_default)) // 검정색
                button.setTypeface(null, Typeface.BOLD)
            } else {
                button.setBackgroundResource(R.drawable.datebox_unselected)
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_disabled_grey)) // 흐린 회색
                button.setTypeface(null, Typeface.NORMAL)
            }
        }
    }
    
    /**
     * 시간 범위 버튼(1개월, 3개월, 6개월, 전체) 설정
     */
    private fun setupTimeRangeButtons() {
        val buttons = listOf(
            binding.monthText,
            binding.threeMonthsText,
            binding.sixMonthsText,
            binding.allText
        )

        // Chip에 텍스트 색상 설정 리스너 구현
        buttons.forEach { chip ->
            chip.setOnClickListener {
                selectTimeRangeButton(chip, buttons)
                
                // 모든 Chip의 텍스트 색상 재설정
                buttons.forEach { btn ->
                    // 선택된 Chip은 흰색, 아닌 것은 회색 텍스트
                    if (btn.isChecked) {
                        btn.setTextColor(Color.WHITE)
                    } else {
                        btn.setTextColor(Color.parseColor("#808080"))
                    }
                }
            }
            
            // 초기 텍스트 색상 설정
            if (chip.isChecked) {
                chip.setTextColor(Color.WHITE)
            } else {
                chip.setTextColor(Color.parseColor("#808080"))
            }
        }
        
        // 기본값으로 첫 번째 버튼 선택
        selectTimeRangeButton(buttons[0], buttons)
        buttons[0].setTextColor(Color.WHITE) // 첫 번째 버튼 텍스트 색상 흰색으로 설정
    }
    
    /**
     * 시간 범위 버튼 선택 상태 업데이트
     */
    private fun selectTimeRangeButton(selectedButton: TextView, allButtons: List<TextView>) {
        allButtons.forEach { button ->
            if (button == selectedButton) {
                button.setBackgroundResource(R.drawable.datebox_selected) // ✅ 선택 시 배경
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_text_default)) // ✅ 선택 시 글자 검정
                button.setTypeface(null, Typeface.BOLD) // ✅ 선택 시 글자 굵게
            } else {
                button.setBackgroundResource(R.drawable.datebox_unselected) // ✅ 비선택 시 배경
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_disabled_grey)) // ✅ 비선택 시 글자 회색
                button.setTypeface(null, Typeface.NORMAL) // ✅ 비선택 시 글자 보통
            }
        }
    }
    
    /**
     * 환율 데이터 API 로드
     */
    private fun loadFxRateData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = IpInfoApiService.create()
                val response = withContext(Dispatchers.IO) {
                    apiService.getFxRates()
                }
                
                if (response.isSuccessful && response.body() != null) {
                    val fxRateDataList = response.body()!!.data
                    if (fxRateDataList.isNotEmpty()) {
                        updateFxRateData(fxRateDataList)
                    }
                } else {
                    Log.e("IpTrendFragment", "환율 데이터 가져오기 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("IpTrendFragment", "환율 데이터 API 호출 오류", e)
            }
        }
    }
    
    /**
     * 환율 데이터 업데이트
     */
    private fun updateFxRateData(fxRateDataList: List<FxRateData>) {
        val fxRateItems = fxRateDataList.map { data ->
            FxRateItem(
                country = data.country,
                code = data.code,
                rate = data.rate,
                change = data.change,
                percent = data.percent
            )
        }
        
        // 전역 홀더에 데이터 저장
        FxRateDataHolder.fxRateItems = fxRateItems
        
        // 추후 환율 데이터 표시 UI가 필요하면 여기에 구현
        Log.d("IpTrendFragment", "환율 데이터 업데이트 완료: ${fxRateItems.size}개")
    }

    private fun setupMiniChartViewPager() {
        // 완전히 빈 데이터로 초기화
        miniChartItems = mutableListOf()
        
        // 이름만 추가하고 실제 값은 모두 0으로 초기화
        val emptyChart1 = IpTrendMiniChartItem(
            getString(R.string.mini_chart_title_stip_1000), 0f, 0f, 0f, emptyList(),
            getString(R.string.mini_chart_title_etip_500), 0f, 0f, 0f, emptyList()
        )
        val emptyChart2 = IpTrendMiniChartItem(
            getString(R.string.mini_chart_title_patent_100), 0f, 0f, 0f, emptyList(),
            getString(R.string.mini_chart_title_etip_movie), 0f, 0f, 0f, emptyList()
        )
        val emptyChart3 = IpTrendMiniChartItem(
            getString(R.string.mini_chart_title_stip_music), 0f, 0f, 0f, emptyList(),
            getString(R.string.mini_chart_title_etip_ai), 0f, 0f, 0f, emptyList()
        )
        
        miniChartItems.add(emptyChart1)
        miniChartItems.add(emptyChart2)
        miniChartItems.add(emptyChart3)

        miniChartAdapter = IpTrendMiniChartAdapter(miniChartItems)
        binding.priceChartViewpager.adapter = miniChartAdapter
        
        // 실제 데이터는 API에서 가져오기
        loadMiniChartData()
    }
    
    private fun loadMiniChartData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = IpInfoApiService.create()
                // API에서 차트 데이터 가져오기 (각 데이터별로 다른 API 엔드포인트를 사용할 수 있음)
                
                // 예시: TradingDataHolder.ipListingItems에서 필요한 데이터 추출
                if (TradingDataHolder.ipListingItems.isNotEmpty()) {
                    // 첫 번째 차트 데이터 업데이트 예시
                    // 실제 구현에서는 적절한 데이터 매핑이 필요함
                    
                    // 기존 코드 대신 실제 API 데이터 활용
                    Log.d("IpTrendFragment", "차트 데이터 업데이트 중: ${TradingDataHolder.ipListingItems.size}개 항목 사용")
                    
                    // 실제 구현 때는 이 부분에서 API 응답으로 미니 차트 데이터 채우기
                }
            } catch (e: Exception) {
                Log.e("IpTrendFragment", "미니 차트 데이터 로드 실패", e)
            }
        }
    }



    // 실제 API 데이터로 차트 엔트리 생성 함수
    private fun createEntriesFromApiData(apiData: List<Any>, baseValue: Float): List<Entry> {
        // 이 함수는 API 응답 구조에 따라 맞게 구현해야 함
        // 예시 구현:
        val entries = mutableListOf<Entry>()
        
        // API 데이터로부터 차트 포인트 생성 로직
        // 실제 구현에서는 API 응답 구조에 맞게 수정 필요
        
        return entries
    }





    // 이 함수는 더 이상 필요하지 않음 - API 데이터 사용






    private fun limitChartEntries(entries: MutableList<Entry>, maxSize: Int) {
        while (entries.size >= maxSize) {
            entries.removeAt(0)
        }
    }

    // 차트 초기 설정 (onCreateView에서 호출됨)
    private fun setupMiniChart(chart: LineChart, initialEntries: List<Entry>, isStip: Boolean, label: String)
    {
        val context = context ?: return

        // LineDataSet 생성 시 복사본 전달
        val dataSet = LineDataSet(initialEntries.toMutableList(), "").apply {
            // 초기 색상 설정 (updateChart에서 덮어씀)
            val base = if (isStip) stipBasePrice else etipBasePrice
            val initialY = initialEntries.lastOrNull()?.y ?: base
            color = ContextCompat.getColor(context, if (initialY >= base) R.color.color_rise else R.color.color_fall)

            setDrawFilled(true)
            fillDrawable = null // 초기에는 null 또는 기본값, updateChart에서 설정
            setDrawValues(false)
            setDrawCircles(false)
            setDrawCircleHole(false)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            isHighlightEnabled = false
        }

        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setPinchZoom(false)
            setScaleEnabled(false)
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawLabels(false)
                setDrawAxisLine(false)
            }
            // 초기 그리기 전에 updateChart를 호출하여 올바른 색상/그라데이션 적용
            if (initialEntries.isNotEmpty()) {
                updateChart(chart, initialEntries, initialEntries.last(), isStip, label)

            } else {
                invalidate() // 데이터 없으면 그냥 그리기
            }
        }
    }

    // 차트 동적 업데이트 (데이터 변경 시 호출됨)
    // 차트 동적 업데이트 (데이터 변경 시 호출됨)
    private fun updateChart(
        chart: LineChart,
        entries: List<Entry>,
        newEntry: Entry,
        isStip: Boolean,
        label: String 
    ) {
        val context = context ?: return
        val dataSet = chart.data?.getDataSetByIndex(0) as? LineDataSet

        if (dataSet != null) {
            dataSet.values = entries.toList()

            // API에서 가져온 기준값 사용
            val base = getBaseValueFromApi(label)

            val riseColor = ContextCompat.getColor(context, R.color.color_rise)
            val fallColor = ContextCompat.getColor(context, R.color.color_fall)
            val isRise = newEntry.y >= base
            val targetColor = if (isRise) riseColor else fallColor

            dataSet.color = targetColor

            val gradientColors = intArrayOf(targetColor, Color.TRANSPARENT)
            try {
                dataSet.fillDrawable = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    gradientColors
                )
            } catch (e: Exception) {
                Log.e("UpdateChart", "Failed to create/set gradient drawable", e)
                dataSet.fillDrawable = null
            }

            dataSet.setDrawFilled(true)

            chart.data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.setVisibleXRangeMaximum(CHART_VISIBLE_X_RANGE)
            chart.moveViewToX(dataSet.xMax)
        } else {
            Log.w("UpdateChart", "DataSet is null. Re-setting up chart.")
            setupMiniChart(chart, entries, isStip, label)
        }
    }
    
    // API에서 기준값을 가져오는 함수
    private fun getBaseValueFromApi(label: String): Float {
        // 실제로는 API에서 가져온 데이터를 사용해야 함
        // 임시적으로 하드코딩된 값 사용
        return when (label) {
            getString(R.string.mini_chart_title_stip_1000) -> 1000f
            getString(R.string.mini_chart_title_patent_100) -> 100f
            getString(R.string.mini_chart_title_stip_music) -> 200f
            getString(R.string.mini_chart_title_etip_500) -> 500f
            getString(R.string.mini_chart_title_etip_movie) -> 300f
            getString(R.string.mini_chart_title_etip_ai) -> 150f
            else -> 1000f
        }
    }


    // API에서 데이터를 가져오는 간격으로 대체






    private fun setupViewPagerListener() {
        binding.priceChartViewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDotState(position)
            }
        })
    }


    private fun updateDotState(selectedIndex: Int) {
        currentDotIndex = selectedIndex % dots.size
        dots.forEachIndexed { index, dot ->
            dot.setImageResource(
                if (index == currentDotIndex) R.drawable.ic_dot_active
                else R.drawable.ic_dot_inactive
            )
        }
    }




    // --- RecyclerView Setup ---


    // API 데이터 기반 상승률 상위 IP RecyclerView 설정
    private fun setupRiseIpRecyclerView() {
        // 초기에는 빈 리스트로 시작
        riseIpAdapter = RiseIpAdapter(emptyList()) { selectedItem ->
            goToOrderPage(selectedItem)
        }
        binding.recyclerViewIpRise.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = riseIpAdapter
            isNestedScrollingEnabled = false
        }
        
        loadTopRisingIps()
    }
    
    private fun loadTopRisingIps(period: String = "1W") {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 실제 API 데이터를 사용하도록 변경
                // API 직접 호출
                val apiService = IpInfoApiService.create()
                val response = withContext(Dispatchers.IO) {
                    apiService.getTopRisingIps(period)
                }
                
                if (response.isSuccessful && response.body() != null) {
                    val topRisingData = response.body()!!.data
                    if (topRisingData.isNotEmpty()) {
                        val topRisingIps = topRisingData.map { data ->
                            RiseIpItem(
                                ticker = data.ticker,
                                name = data.name,
                                percent = data.changePercent
                            )
                        }
                        
                        activity?.runOnUiThread {
                            if (::riseIpAdapter.isInitialized) {
                                riseIpAdapter = RiseIpAdapter(topRisingIps) { selectedItem ->
                                    goToOrderPage(selectedItem)
                                }
                                binding.recyclerViewIpRise.adapter = riseIpAdapter
                            }
                        }
                    } else {
                        setDefaultRiseIpData()
                    }
                } else {
                    Log.e("IpTrendFragment", "상승률 상위 IP 데이터 가져오기 실패: ${response.code()}")
                    setDefaultRiseIpData()
                }
            } catch (e: Exception) {
                Log.e("IpTrendFragment", "상승률 상위 IP 데이터 로드 오류", e)
                setDefaultRiseIpData()
            }
        }
    }
    
    // API 호출 실패 시 기본 데이터 표시 (인터페이스 유지 목적)
    private fun setDefaultRiseIpData() {
        activity?.runOnUiThread {
            if (::riseIpAdapter.isInitialized) {
                // 기본 더미 데이터로 설정
                val defaultItems = listOf(
                    RiseIpItem(ticker = "AXNO", name = "스포츠픽", percent = "0.00%"),
                    RiseIpItem(ticker = "JWV", name = "준원지비아이", percent = "0.00%"),
                    RiseIpItem(ticker = "MDM", name = "앤프티앤씨", percent = "0.00%"),
                    RiseIpItem(ticker = "WETALK", name = "두레", percent = "0.00%"),
                    RiseIpItem(ticker = "KCOT", name = "코이코어", percent = "0.00%")
                )
                
                riseIpAdapter = RiseIpAdapter(defaultItems) { selectedItem ->
                    goToOrderPage(selectedItem)
                }
                binding.recyclerViewIpRise.adapter = riseIpAdapter
            }
        }
    }



    // 동적 데이터용 STIP Index RecyclerView 설정
    private fun setupStipIndexRecyclerView() {
        // StipIndexAdapter 인스턴스 생성 (멤버 변수인 stipIndexList 사용)
        stipIndexAdapter = StipIndexAdapter(stipIndexList) // 클래스가 정의되어 있다고 가정

        binding.recyclerViewStipIndex.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = stipIndexAdapter // 생성된 어댑터 설정
            isNestedScrollingEnabled = false // 스크롤 중첩 방지
        }
    }



    // --- UI Update Functions ---

    private fun onDotClick(dotIndex: Int) {
        if (dotIndex == currentDotIndex) return
        currentDotIndex = dotIndex
        updateDotState(currentDotIndex)

        // 👉 ViewPager 페이지 전환 추가
        binding.priceChartViewpager.setCurrentItem(dotIndex, true)

        Log.d("IpTrendFragment", "Dot $dotIndex selected.")
    }





    // STIP/ETIP 가격 텍스트 업데이트
    private fun updatePriceText(
        price: Float,
        base: Float,
        priceView: TextView,
        changeView: TextView,
        percentView: TextView,
        iconView: ImageView
    ) {
        val context = context ?: return

        val change = price - base
        val percent = if (base != 0f) (change / base) * 100f else 0f
        val isZero = abs(change) < 0.005f // 0.00으로 표시될 정도의 매우 작은 값
        val isRise = change > 0f

        // 0.00이면 검정색, 그 외에는 상승/하락 색상
        val colorRes = when {
            isZero -> android.R.color.black // 0.00일 때 검정색
            isRise -> R.color.color_rise  // 상승 시 빨간색
            else -> R.color.color_fall    // 하락 시 파란색
        }
        
        // 0.00일 때 화살표 숨김, 그 외에는 상승/하락 화살표 표시
        val iconRes = if (isRise) R.drawable.ic_arrow_up_red else R.drawable.ic_arrow_down_blue

        try {
            val color = ContextCompat.getColor(context, colorRes)

            priceView.text = String.Companion.format(Locale.US, "%.10f", price).trimEnd('0').trimEnd('.')
            priceView.setTextColor(color)

            val truncatedChange = BigDecimal(abs(change).toDouble()).setScale(2, RoundingMode.DOWN).toPlainString()
            changeView.text = truncatedChange
            changeView.setTextColor(color)

            val truncatedPercent = (if (percent >= 0) "+" else "") + BigDecimal(percent.toDouble()).setScale(2, RoundingMode.DOWN).toPlainString() + "%"
            percentView.text = truncatedPercent // 부호 포함
            percentView.setTextColor(color)

            // 0.00일 때는 화살표 숨김
            if (isZero) {
                iconView.visibility = View.INVISIBLE
            } else {
                iconView.setImageResource(iconRes)
                iconView.visibility = View.VISIBLE
            }

        } catch (e: Exception) {
            Log.e("UpdatePriceText", "Failed to update price text UI", e)
        }
    }

    // --- Utility Functions ---

    private fun openWebsite(url: String) {
        val context = context ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e("LogoClick", "Web browser not found.", e)
            Toast.makeText(context, "웹 페이지를 열 수 있는 앱이 없습니다.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("LogoClick", "Error opening URL", e)
            Toast.makeText(context, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }


    // --- Lifecycle Management ---

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null // View Binding 참조 해제 (메모리 누수 방지)
        Log.d("IpTrendFragment", "onDestroyView called, binding set to null")
    }




    // 주문 페이지로 이동
    private fun goToOrderPage(selectedItem: com.stip.stip.ipinfo.model.RiseIpItem) {
    // 클릭 시 아무 동작도 하지 않음
}


} // End of IpTrendFragment class