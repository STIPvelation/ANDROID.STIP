package com.stip.stip.iptransaction.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.stip.stip.R
import com.stip.stip.ScrollableToTop
import com.stip.stip.iptransaction.adapter.IpInvestmentAdapter
import com.stip.stip.databinding.FragmentIpInvestmentBinding
import com.stip.stip.iptransaction.api.IpTransactionService
import com.stip.stip.iptransaction.model.IpInvestmentItem
import com.stip.stip.signup.utils.PreferenceUtil
import com.stip.stip.iptransaction.fragment.TickerSelectionDialogFragment
import com.stip.api.repository.WalletHistoryRepository
import com.stip.api.model.WalletHistoryRecord
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import java.text.NumberFormat
import java.util.Locale
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.stip.stip.api.RetrofitClient
import com.stip.stip.api.service.MarketPairsService
import java.text.DecimalFormat

class IpInvestmentFragment : Fragment(), ScrollableToTop, TickerSelectionDialogFragment.TickerSelectionListener {

    private lateinit var mainViewModel: com.stip.stip.MainViewModel

    private var _binding: FragmentIpInvestmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var ipInvestmentAdapter: IpInvestmentAdapter
    private val walletHistoryRepository = WalletHistoryRepository()
    
    // 현재 선택된 날짜 범위를 저장하기 위한 변수
    private var currentStartDate: String = ""
    private var currentEndDate: String = ""
    
    // 현재 선택된 필터들을 저장하기 위한 변수들
    private var currentTickerFilter: String? = null
    private var currentTransactionTypeFilters: List<String> = emptyList()
    
    // 필터 결과 처리 중인지 확인하는 플래그
    private var isFilterResultPending = false

    private var marketPairMap: Map<String, String> = emptyMap()

    companion object {
        private const val PREF_TICKER_FILTER = "ticker_filter"
        @JvmStatic
        fun newInstance() = IpInvestmentFragment()
    }

    init {
        // 초기 날짜 범위를 1개월로 설정
        val endDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val startDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(
            java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -1) }.time
        )
        currentStartDate = startDate
        currentEndDate = endDate
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIpInvestmentBinding.inflate(inflater, container, false)
        // 초기 UI 라벨 설정
        binding.textViewFilterLabel.text = "1개월 내역보기"
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mainViewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[com.stip.stip.MainViewModel::class.java]
        mainViewModel.memberInfo.observe(viewLifecycleOwner) { memberInfo ->
            // TODO: 회원정보를 UI에 반영하는 코드 작성
        }

        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupScrollSync()
        
        // 저장된 티커 필터 복원
        restoreTickerFilter()
        
        // 마켓 페어 매핑 먼저 불러온 후 거래내역 로드 (1개월 기간으로 초기 로드)
        viewLifecycleOwner.lifecycleScope.launch {
            marketPairMap = fetchMarketPairs()
            // 1개월 기간으로 초기 데이터 로드
            val endDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val startDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(
                java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -1) }.time
            )
            // 초기 로드 시에는 1개월 기간으로만 데이터 조회
            loadAllData(null, startDate, endDate)
        }

        binding.imageViewFilterIcon.setOnClickListener {
            isFilterResultPending = true  // 필터 다이얼로그 시작 시 플래그 설정
            navigateToInvestmentFilter()
        }

        binding.buttonIpSearch.setOnClickListener {
            showTickerSelectionDialog()
        }

        parentFragmentManager.setFragmentResultListener("investmentFilterResult", viewLifecycleOwner) { _, bundle ->
            val types = bundle.getStringArrayList("filterTypes") ?: arrayListOf()
            val startDate = bundle.getString("filterStartDate")
            val endDate = bundle.getString("filterEndDate")
            val periodLabel = bundle.getString("filterPeriodLabel") ?: ""
            binding.textViewFilterLabel.text = "$periodLabel 내역보기"
            
            // 트랜잭션 타입 필터 저장 (기존 티커 필터는 유지)
            currentTransactionTypeFilters = types.toList()
            currentStartDate = startDate ?: ""
            currentEndDate = endDate ?: ""
            
            
            viewLifecycleOwner.lifecycleScope.launch {
                marketPairMap = fetchMarketPairs()
                applyCombinedFilters()
                isFilterResultPending = false  // 필터 결과 처리 완료 후 플래그 해제
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 필터 결과가 처리 중이 아닐 때만 저장된 필터 로드
        if (!isFilterResultPending) {
            Log.d("IpInvestmentFragment", "onResume: 필터 결과 처리 중이 아님, 저장된 필터 로드")
            // 여기서 저장된 필터 상태를 복원하는 로직을 추가할 수 있음
        } else {
            Log.d("IpInvestmentFragment", "onResume: 필터 결과 처리 중, 저장된 필터 로드 건너뜀")
        }
    }

    private fun setupRecyclerView() {
        ipInvestmentAdapter = IpInvestmentAdapter()
        binding.recyclerViewInvestmentList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ipInvestmentAdapter
            val divider = DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
            ContextCompat.getDrawable(requireContext(), R.drawable.recycler_divider)?.let {
                divider.setDrawable(it)
                addItemDecoration(divider)
            }
        }
    }

    private fun setupScrollSync() {
        binding.headerScrollView.bindSyncTarget(binding.scrollableContent)
        binding.scrollableContent.bindSyncTarget(binding.headerScrollView)
    }
    
    /**
     * 조합된 필터를 적용하는 함수
     */
    private fun applyCombinedFilters() {
        // 모든 필터 조건을 조합하여 데이터 로드
        loadAllData(
            filterTypes = combineFilters(),
            startDate = currentStartDate,
            endDate = currentEndDate
        )
    }
    
    /**
     * 현재 선택된 모든 필터를 조합하는 함수
     */
    private fun combineFilters(): List<String> {
        val combinedFilters = mutableListOf<String>()
        
        // 트랜잭션 타입 필터 추가
        combinedFilters.addAll(currentTransactionTypeFilters)
        
        // 티커 필터 추가
        currentTickerFilter?.let { ticker ->
            combinedFilters.add(ticker)
        }
        
        return combinedFilters
    }
    
    /**
     * 티커 필터를 SharedPreferences에 저장
     */
    private fun saveTickerFilter(ticker: String) {
        val sharedPrefs = requireActivity().getSharedPreferences("investment_filter_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putString(PREF_TICKER_FILTER, ticker).apply()
    }
    
    /**
     * 저장된 티커 필터를 복원
     */
    private fun restoreTickerFilter() {
        val sharedPrefs = requireActivity().getSharedPreferences("investment_filter_prefs", android.content.Context.MODE_PRIVATE)
        val savedTicker = sharedPrefs.getString(PREF_TICKER_FILTER, "전체")
        
        // 버튼 텍스트 업데이트
        binding.buttonIpSearch.text = savedTicker
        
        // 티커 필터 변수 설정
        currentTickerFilter = if (savedTicker == "전체") null else savedTicker
    }

    private fun loadAllData(
        filterTypes: List<String>? = null,
        startDate: String? = null,
        endDate: String? = null
    ) {
        // 로그인 여부 확인
        if (PreferenceUtil.getToken().isNullOrEmpty()) {
            showEmptyState("로그인이 필요합니다.")
            return
        }
        
        // 로딩 상태 표시
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewInvestmentList.visibility = View.GONE
        binding.noDataContainer.visibility = View.GONE
        
        // 2초 후 자동으로 ProgressBar 숨기기
        Handler(Looper.getMainLooper()).postDelayed({
            if (_binding != null) {
                binding.progressBar.visibility = View.GONE
            }
        }, 2000)
        
        // 입출금 내역과 매수/매도 내역을 모두 로드
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val memberId = PreferenceUtil.getUserId()
                if (memberId.isNullOrBlank()) {
                    showEmptyState("사용자 정보를 찾을 수 없습니다.")
                    return@launch
                }
                
                // 매수/매도 내역 로드 (기간 필터 전달)
                val buySellItems = loadBuySellData(memberId, startDate, endDate)
                
                // 필터 타입에 따른 데이터 필터링
                val filteredItems = if (!filterTypes.isNullOrEmpty()) {
                    var filteredItems = buySellItems
                    
                    // 트랜잭션 타입 필터와 티커 필터를 분리
                    val transactionTypeFilters = filterTypes.filter { it == "매수" || it == "매도" }
                    val tickerFilters = filterTypes.filter { it != "매수" && it != "매도" }
                    
                    // 트랜잭션 타입 필터 적용
                    if (transactionTypeFilters.isNotEmpty()) {
                        filteredItems = filteredItems.filter { item ->
                            transactionTypeFilters.contains(item.type)
                        }
                    }
                    
                    // 티커 필터 적용
                    if (tickerFilters.isNotEmpty()) {
                        filteredItems = filteredItems.filter { item ->
                            tickerFilters.contains(item.name)
                        }
                    }
                    
                    filteredItems
                } else {
                    // 필터가 없는 경우 모든 데이터
                    buySellItems
                }
                
                // 날짜순으로 정렬 (최신순)
                val sortedItems = filteredItems.sortedByDescending { item ->
                    try {
                        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        dateFormat.parse(item.orderTime)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                }
                
                // Fragment가 아직 연결되어 있는지 확인 후 UI 업데이트
                if (isAdded) {
                    binding.progressBar.visibility = View.GONE
                    if (sortedItems.isNotEmpty()) {
                        showInvestmentData(sortedItems)
                    } else {
                        showEmptyState("해당 기간의 거래내역이 없습니다.")
                    }
                }
            } catch (e: Exception) {
                Log.e("IpInvestmentFragment", "데이터 로드 실패", e)
                if (isAdded) {
                    binding.progressBar.visibility = View.GONE
                    showEmptyState("거래내역을 불러오는 중 오류가 발생했습니다.")
                }
            }
        }
    }
    

    
    private suspend fun loadBuySellData(memberId: String, startDate: String? = null, endDate: String? = null): List<IpInvestmentItem> {
        return try {
            // 전달받은 날짜 또는 현재 저장된 날짜 사용
            val effectiveStartDate = startDate ?: currentStartDate
            val effectiveEndDate = endDate ?: currentEndDate
            
            // 새로운 trades API 사용
            val trades = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val allTrades = mutableListOf<com.stip.stip.iptransaction.model.TradeResponse>()
                var error: Throwable? = null
                
                if (effectiveStartDate.isNotEmpty() && effectiveEndDate.isNotEmpty() && marketPairMap.isNotEmpty()) {
                    // 필터링할 마켓 페어 결정
                    val targetMarketPairs = if (currentTickerFilter != null) {
                        // 특정 티커가 선택된 경우 해당 마켓 페어만
                        marketPairMap.filter { (_, symbol) -> 
                            symbol.substringBefore("/") == currentTickerFilter 
                        }
                    } else {
                        // 전체 선택된 경우 모든 마켓 페어
                        marketPairMap
                    }
                    
                    if (targetMarketPairs.isNotEmpty()) {
                        val latch = java.util.concurrent.CountDownLatch(targetMarketPairs.size)
                        
                        // 선택된 마켓 페어에 대해서만 trades API 호출
                        targetMarketPairs.forEach { (marketPairId, symbol) ->
                            IpTransactionService.getTrades(marketPairId, effectiveStartDate, effectiveEndDate) { tradeListResponse, err ->
                                if (err != null) {
                                    Log.e("IpInvestmentFragment", "Trades API 호출 실패: $marketPairId", err)
                                    error = err
                                } else {
                                    tradeListResponse?.data?.let { trades ->
                                        allTrades.addAll(trades)
                                        Log.d("IpInvestmentFragment", "마켓 페어 $symbol: ${trades.size}개 거래")
                                    }
                                }
                                latch.countDown()
                            }
                        }
                        
                        // 모든 API 호출이 완료될 때까지 대기
                        latch.await()
                    } else {
                        Log.d("IpInvestmentFragment", "필터링된 마켓 페어가 없음")
                    }
                } else {
                    if (marketPairMap.isEmpty()) {
                        Log.d("IpInvestmentFragment", "마켓 페어 정보가 없음")
                    } else {
                        Log.d("IpInvestmentFragment", "날짜 범위가 설정되지 않음")
                    }
                }
                
                if (error != null) throw error!!
                
                // 중복 제거
                val uniqueTrades = allTrades.distinctBy { "${it.id}_${it.isSell}" }
                uniqueTrades
            }
            
            Log.d("IpInvestmentFragment", "전체 거래 내역: ${trades.size}개")
            
            // 시간순으로 정렬 (최신순)
            val sortedTrades = trades.sortedByDescending { trade ->
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
                    inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    inputFormat.parse(trade.timestamp)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }
            
            // API 응답을 IpInvestmentItem으로 변환
            sortedTrades.map { trade ->
                val type = if (trade.isSell) "매도" else "매수"
                val orderTime = formatDateTime(trade.orderDateTime)
                val executionTime = formatDateTime(trade.timestamp)
                val symbol = trade.symbol
                val baseAsset = symbol.substringBefore("/")
                
                // 매수/매도에 따른 수수료와 정산 금액 단위 처리
                val (feeText, settlementText) = if (!trade.isSell) {
                    // 매수: 해당 티커 단위로 표시
                    val feeFormatted = DecimalFormat("#,##0.0000").apply { roundingMode = java.math.RoundingMode.DOWN }.format(trade.feeValue).trimEnd('0').trimEnd('.')
                    val settlementFormatted = trade.realAmount.stripTrailingZeros().toPlainString()
                    Pair("$feeFormatted $baseAsset", "$settlementFormatted $baseAsset")
                } else {
                    // 매도: USD 단위로 표시
                    val feeFormatted = DecimalFormat("#,##0.0000").apply { roundingMode = java.math.RoundingMode.DOWN }.format(trade.feeValue).trimEnd('0').trimEnd('.')
                    val settlementFormatted = trade.realAmount.stripTrailingZeros().toPlainString()
                    Pair("$$feeFormatted", "$$settlementFormatted")
                }
                
                IpInvestmentItem(
                    type = type,
                    name = baseAsset,
                    quantity = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }.format(trade.quantity),
                    unitPrice = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }.format(trade.price),
                    amount = DecimalFormat("#,##0.0000").apply { roundingMode = java.math.RoundingMode.DOWN }.format(trade.tradeAmount),
                    fee = feeText,
                    settlement = settlementText,
                    orderTime = orderTime,
                    executionTime = executionTime
                )
            }
        } catch (e: Exception) {
            Log.e("IpInvestmentFragment", "매수/매도 내역 로드 실패", e)
            emptyList()
        }
    }
    
    private fun showEmptyState(message: String = "해당 기간의 거래내역이 없습니다.") {
        binding.recyclerViewInvestmentList.visibility = View.GONE
        binding.noDataContainer.visibility = View.VISIBLE
        binding.noDataText.text = message
        binding.periodText.visibility = View.VISIBLE
        
        // 필터에서 선택한 기간에 따라 표시
        if (currentStartDate.isNotEmpty() && currentEndDate.isNotEmpty()) {
            binding.periodText.text = "$currentStartDate ~ $currentEndDate"
        } else {
            // 선택된 필터가 없을 경우 기본값 표시
            binding.periodText.text = binding.textViewFilterLabel.text.toString()
        }
        
        ipInvestmentAdapter.updateData(emptyList())
    }
    
    private fun showInvestmentData(data: List<IpInvestmentItem>) {
        if (data.isNotEmpty()) {
            binding.recyclerViewInvestmentList.visibility = View.VISIBLE
            binding.noDataContainer.visibility = View.GONE
            ipInvestmentAdapter.updateData(data)
        } else {
            showEmptyState()
        }
    }

    private fun navigateToInvestmentFilter() {
        val filterFragment = InvestmentFilterFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, filterFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun scrollToTop() {
        binding.recyclerViewInvestmentList.scrollToPosition(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    /**
     * 티커 선택 다이얼로그를 표시하는 메소드
     */
    private fun showTickerSelectionDialog() {
        val dialogFragment = TickerSelectionDialogFragment.newInstance(this)
        dialogFragment.show(parentFragmentManager, "TickerSelectionDialogFragment")
    }
    
    /**
     * 티커 선택 리스너 구현
     */
    override fun onTickerSelected(ticker: String) {
        // 선택된 티커로 버튼 텍스트 업데이트
        binding.buttonIpSearch.text = ticker
        
        // 티커 필터 저장
        currentTickerFilter = if (ticker == "전체") null else ticker
        
        // 티커 필터를 SharedPreferences에 저장
        saveTickerFilter(ticker)
        
        // 조합된 필터 적용
        applyCombinedFilters()
    }
    

    
    /**
     * 수수료 포맷팅 함수 - 소수점 뒤 의미있는 숫자만 표시
     */
    private fun formatFee(fee: Double): String {
        return when {
            fee == 0.0 -> "0"
            fee == fee.toInt().toDouble() -> fee.toInt().toString()
            else -> String.format("%.2f", fee).trimEnd('0').trimEnd('.')
        }
    }
    
    private fun parseIsoTimestampToMillis(iso: String): Long {
        return try {
            val fixedIso = iso.replace(Regex("\\.(\\d{3})\\d{3}"), ".$1")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            dateFormat.parse(fixedIso)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("IpInvestmentFragment", "timestamp 파싱 실패: $iso", e)
            System.currentTimeMillis()
        }
    }
    
    private fun formatDateTime(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            
            val outputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            outputFormat.timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
            
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: java.util.Date())
        } catch (e: Exception) {
            Log.w("IpInvestmentFragment", "Failed to parse date: $dateString", e)
            "00:00:00"
        }
    }

    // 마켓 페어 매핑 불러오기
    private suspend fun fetchMarketPairs(): Map<String, String> {
        return try {
            val api = RetrofitClient.createTapiService(MarketPairsService::class.java)
            val response = api.getMarketPairs()
            response.associate { it.id to it.symbol }
        } catch (e: Exception) {
            Log.e("IpInvestmentFragment", "마켓 페어 매핑 불러오기 실패", e)
            emptyMap()
        }
    }
}