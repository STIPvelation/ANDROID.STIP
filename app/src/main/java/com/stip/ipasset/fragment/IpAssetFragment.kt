package com.stip.ipasset.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.stip.stip.MainActivity
import com.stip.dummy.AssetDummyData
import com.stip.ipasset.ticker.fragment.TickerTransactionFragment
import com.stip.ipasset.usd.fragment.USDDepositFragment
import com.stip.ipasset.usd.manager.USDAssetManager
import com.stip.stip.R
import com.stip.stip.databinding.FragmentIpAssetBinding
import com.stip.stip.databinding.ItemTickerAssetBinding
import com.stip.stip.databinding.ItemUsdAssetBinding
import com.stip.api.repository.PortfolioRepository
import com.stip.api.model.PortfolioAsset
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch
import com.stip.stip.signup.utils.PreferenceUtil
import com.stip.stip.signup.Constants
import com.stip.api.repository.WalletHistoryRepository
import com.stip.api.model.WalletHistoryRecord

@AndroidEntryPoint
class IpAssetFragment : Fragment() {

    private var _binding: FragmentIpAssetBinding? = null
    private val binding get() = _binding!!
    
    // USD 자산 매니저
    private val assetManager = USDAssetManager.getInstance()
    
    // 포트폴리오 Repository
    private val portfolioRepository = PortfolioRepository()
    
    // 입출금 내역 Repository
    private val walletHistoryRepository = WalletHistoryRepository()
    
    private lateinit var adapter: IpAssetsAdapter
    private val assetsList = mutableListOf<IpAssetItem>()
    private val filteredList = mutableListOf<IpAssetItem>()
    private var currentFilter = FilterType.ALL
    
    // USD 업데이트 디바운싱을 위한 변수들
    private var lastUsdBalance: Double = 0.0
    private var usdUpdateJob: kotlinx.coroutines.Job? = null
    
    // 블링크 방지를 위한 상태 관리
    private var isDataLoading = false
    private var isInitialDataLoaded = false
    private var lastAppliedFilter = ""
    private var isResuming = false
    
    // 자산 데이터 캐시 키
    private val ASSET_CACHE_KEY = "cached_asset_data"
    private val ASSET_CACHE_TIMESTAMP_KEY = "cached_asset_timestamp"
    private val CACHE_VALIDITY_DURATION = 1 * 60 * 1000L // 1분
    
    // 필터 타입 정의
    enum class FilterType {
        ALL, HOLDING
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIpAssetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 전화 사기 경고 다이얼로그 표시
        showPhoneFraudAlertDialogIfNeeded()
        
        // RecyclerView 및 기타 UI 컴포넌트 설정 (adapter 초기화)
        setupRecyclerView()
        setupSearchAndFilter()
        setupSwipeRefresh()
        
        // USD 데이터 변경 관찰
        observeUsdData()
        
        // 즉시 UI 초기화
        initializeUI()
        
        // 백그라운드에서 포트폴리오 API 데이터 로드
        loadBalancesFromHistory()
        
        // KRW 입금 버튼 클릭 시 USDDepositFragment로 이동
        binding.buttonKrwDeposit.setOnClickListener {
            try {
                // 프래그먼트 트랜잭션을 사용하여 USDDepositFragment로 교체
                val fragmentManager = requireActivity().supportFragmentManager
                val fragmentTransaction = fragmentManager.beginTransaction()
                
                // 애니메이션 추가
                fragmentTransaction.setCustomAnimations(
                    R.anim.slide_in_right,  // 들어오는 애니메이션 (없으면 생성 필요)
                    R.anim.slide_out_left,   // 나가는 애니메이션 (없으면 생성 필요)
                    R.anim.slide_in_left,    // 들어오는 애니메이션 (백스택에서 돌아올 때)
                    R.anim.slide_out_right   // 나가는 애니메이션 (백스택에서 돌아올 때)
                )
                
                // 새로운 USDDepositFragment 인스턴스 생성
                val usdDepositFragment = USDDepositFragment()
                
                // 프래그먼트 교체 및 백스택에 추가
                fragmentTransaction.replace(R.id.fragment_container, usdDepositFragment)
                fragmentTransaction.addToBackStack(null)
                fragmentTransaction.commit()
                
            } catch (e: Exception) {
                // 오류 발생 시 메시지 표시
                Toast.makeText(requireContext(), "화면 전환 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * UI 즉시 초기화
     */
    private fun initializeUI() {
        // 기존 데이터가 있으면 유지하고 업데이트만 수행
        if (assetsList.isNotEmpty()) {
            updateTotalAssetsDisplay()
            applyFilteringEfficiently()
            return
        }
        
        // 캐시된 자산 데이터가 있으면 먼저 표시
        val cachedAssets = loadCachedAssets()
        if (cachedAssets.isNotEmpty()) {
            assetsList.clear()
            assetsList.addAll(cachedAssets)
            
            // 상단 총 보유자산 표시
            updateTotalAssetsDisplay()
            
            // 즉시 필터링 적용하여 UI 표시
            applyFilteringEfficiently()
            
            return
        }
        
        // 캐시된 데이터가 없으면 기본 USD 항목으로 초기화
        val defaultUsdBalance = assetManager.balance.value ?: 0.0
        val defaultUsdItem = IpAssetItem(
            currencyCode = "USD",
            symbol = "USD",
            amount = defaultUsdBalance,
            usdEquivalent = defaultUsdBalance,
            krwEquivalent = com.stip.utils.ExchangeRateManager.convertUsdToKrw(defaultUsdBalance),
            isUsd = true
        )
        
        // 기본 자산 리스트 설정
        assetsList.clear() // 기존 데이터 클리어
        assetsList.add(defaultUsdItem)
        
        // 상단 총 보유자산 표시
        updateTotalAssetsDisplay()
        
        // 즉시 필터링 적용하여 UI 표시
        applyFilteringEfficiently()
    }
    
    /**
     * 상단 총 보유자산 표시 업데이트
     */
    private fun updateTotalAssetsDisplay() {
        val totalUsdBalance = assetsList.firstOrNull { it.isUsd }?.amount ?: 0.0
        val formatter = java.text.DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }
        binding.totalIpAssets.text = "$${formatter.format(totalUsdBalance)} USD"
        
        // KRW 환산액 표시
        val krwAmount = com.stip.utils.ExchangeRateManager.convertUsdToKrw(totalUsdBalance)
        val krwFormatter = NumberFormat.getNumberInstance(Locale.US).apply {
            roundingMode = java.math.RoundingMode.DOWN
        }
        binding.totalIpAssetsKrw.text = "≈ ${krwFormatter.format(krwAmount.toInt())} KRW"
    }
    
    /**
     * 캐시된 자산 데이터 로드
     */
    private fun loadCachedAssets(): List<IpAssetItem> {
        try {
            val sharedPrefs = requireActivity().getSharedPreferences("asset_cache", android.content.Context.MODE_PRIVATE)
            val cacheTimestamp = sharedPrefs.getLong(ASSET_CACHE_TIMESTAMP_KEY, 0)
            val currentTime = System.currentTimeMillis()
            
            // 캐시가 유효한지 확인 (5분 이내)
            if (currentTime - cacheTimestamp > CACHE_VALIDITY_DURATION) {
                android.util.Log.d("IpAssetFragment", "캐시가 만료됨")
                return emptyList()
            }
            
            val cachedJson = sharedPrefs.getString(ASSET_CACHE_KEY, null)
            if (cachedJson.isNullOrEmpty()) {
                return emptyList()
            }
            
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<IpAssetItem>>() {}.type
            val cachedAssets = gson.fromJson<List<IpAssetItem>>(cachedJson, type)
            
            android.util.Log.d("IpAssetFragment", "캐시된 자산 데이터 로드 완료: ${cachedAssets.size}개")
            return cachedAssets ?: emptyList()
            
        } catch (e: Exception) {
            android.util.Log.e("IpAssetFragment", "캐시된 자산 데이터 로드 실패: ${e.message}", e)
            return emptyList()
        }
    }
    
    /**
     * 자산 데이터를 캐시에 저장
     */
    private fun saveAssetsToCache(assets: List<IpAssetItem>) {
        try {
            val sharedPrefs = requireActivity().getSharedPreferences("asset_cache", android.content.Context.MODE_PRIVATE)
            val gson = com.google.gson.Gson()
            val assetsJson = gson.toJson(assets)
            
            sharedPrefs.edit()
                .putString(ASSET_CACHE_KEY, assetsJson)
                .putLong(ASSET_CACHE_TIMESTAMP_KEY, System.currentTimeMillis())
                .apply()
            
            Log.d("IpAssetFragment", "자산 데이터 캐시 저장 완료: ${assets.size}개")
        } catch (e: Exception) {
            Log.e("IpAssetFragment", "자산 데이터 캐시 저장 실패: ${e.message}", e)
        }
    }
    
    private fun setupRecyclerView() {
        adapter = IpAssetsAdapter(this)
        binding.ipAssets.layoutManager = LinearLayoutManager(requireContext())
        binding.ipAssets.adapter = adapter
    }
    
    /**
     * USDAssetManager로부터 USD 데이터 변화 관찰
     */
    private fun observeUsdData() {
        assetManager.balance.observe(viewLifecycleOwner) { balance ->
            // 이전 USD 잔고와 비교하여 실제 변경이 있을 때만 업데이트
            if (kotlin.math.abs(balance - lastUsdBalance) > 0.001) {
                lastUsdBalance = balance
                
                // 이전 업데이트 작업 취소
                usdUpdateJob?.cancel()
                
                // 디바운싱 적용 (300ms 지연)
                usdUpdateJob = lifecycleScope.launch {
                    kotlinx.coroutines.delay(300)
                    updateUsdAssetItem(balance)
                }
            }
        }
    }
    
    /**
     * USD 자산 업데이트
     */
    private fun updateUsdAssetItem(balance: Double) {
        // USD 데이터 업데이트
        val existingUsdIndex = assetsList.indexOfFirst { it.isUsd }

        val usdAssetItem = IpAssetItem(
            currencyCode = "USD",
            symbol = "USD",
            amount = balance,
            usdEquivalent = balance,
            krwEquivalent = com.stip.utils.ExchangeRateManager.convertUsdToKrw(balance),
            isUsd = true
        )

        if (existingUsdIndex >= 0) {
            // 기존 USD 항목 업데이트
            assetsList[existingUsdIndex] = usdAssetItem
        } else {
            // 없으면 추가 (리스트 맨 앞에)
            assetsList.add(0, usdAssetItem)
        }

        // 데이터 변경 알림 (UI 스레드에서 실행)
        if (isAdded && _binding != null) {
            applyFiltering()
        }
    }
    
    /**
     * 포트폴리오 API에서 잔고 데이터 로드
     */
    private fun loadBalancesFromHistory() {
        if (isDataLoading) return // 이미 로딩 중이면 중복 호출 방지
        
        isDataLoading = true
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val memberId = PreferenceUtil.getUserId()
            if (memberId.isNullOrBlank()) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(requireContext(), "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                    isDataLoading = false
                }
                return@launch
            }
            
            try {
                // 포트폴리오 전체 응답 조회
                val portfolioResponse = portfolioRepository.getPortfolioResponse(memberId)
                
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (portfolioResponse != null) {
                        // 상단 총 보유자산 표시
                        val formatter = java.text.DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }
                        val usdBalance = portfolioResponse.usdBalance?.toDouble() ?: 0.0
                        val totalAssets = portfolioResponse.totalAmount?.toDouble() ?: usdBalance
                        binding.totalIpAssets.text = "$${formatter.format(totalAssets)} USD"
                        
                        // KRW 환산액 표시 (API 기반 환율 변환
                        lifecycleScope.launch {
                            try {
                                val krwAmount = com.stip.utils.ExchangeRateManager.convertUsdToKrwWithApi(totalAssets)
                                if (isAdded && _binding != null) {
                                    val krwFormatter = NumberFormat.getNumberInstance(Locale.US).apply {
                                        roundingMode = java.math.RoundingMode.DOWN
                                    }
                                    binding.totalIpAssetsKrw.text = "≈ ${krwFormatter.format(krwAmount.toInt())} KRW"
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("IpAssetFragment", "KRW 환산액 계산 실패: ${e.message}", e)
                            }
                        }
                        
                        // 효율적인 자산 리스트 업데이트
                        updateAssetsListEfficiently(portfolioResponse, usdBalance)
                        
                        android.util.Log.d("IpAssetFragment", "새로운 DTO 구조로 자산 로드 완료: ${portfolioResponse.wallets.size}개 자산")
                    } else {
                        // API 응답이 null인 경우 빈 상태 표시
                        showEmptyState()
                    }
                }
            } catch (e: Exception) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.util.Log.e("IpAssetFragment", "포트폴리오 API 호출 실패: ${e.message}", e)
                    showEmptyState()
                }
            } finally {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isDataLoading = false
                }
            }
        }
    }
    
    /**
     * 효율적인 자산 리스트 업데이트 (블링크 방지)
     */
    private fun updateAssetsListEfficiently(portfolioResponse: com.stip.api.model.PortfolioResponse, usdBalance: Double) {
        val newAssetsList = mutableListOf<IpAssetItem>()
        
        // USD 항목 추가
        newAssetsList.add(
            IpAssetItem(
                currencyCode = "USD",
                symbol = "USD",
                amount = usdBalance,
                usdEquivalent = usdBalance,
                krwEquivalent = com.stip.utils.ExchangeRateManager.convertUsdToKrw(usdBalance),
                isUsd = true
            )
        )
        
        // USDAssetManager에 USD 잔액 설정
        assetManager.setUsdBalance(usdBalance)
        
        // 다른 자산들 추가 (USD 제외)
        portfolioResponse.wallets
            .filter { it.symbol != "USD" }
            .forEach { wallet ->
                // symbol 정보 생성 (예: IJECT/USD)
                val symbol = "${wallet.symbol}/USD"
                newAssetsList.add(
                    IpAssetItem(
                        currencyCode = wallet.symbol,
                        symbol = symbol,
                        amount = wallet.balance?.toDouble() ?: 0.0,
                        usdEquivalent = wallet.evalAmount?.toDouble() ?: 0.0,
                        krwEquivalent = com.stip.utils.ExchangeRateManager.convertUsdToKrw(wallet.evalAmount?.toDouble() ?: 0.0),
                        isUsd = false
                    )
                )
            }
        
        // 기존 리스트와 비교하여 실제 변경이 있을 때만 업데이트
        if (!isListsEqual(assetsList, newAssetsList)) {
            assetsList.clear()
            assetsList.addAll(newAssetsList)
            
            // 새로운 자산 데이터를 캐시에 저장
            saveAssetsToCache(newAssetsList)
            
            // 필터링된 리스트만 업데이트 (전체 리스트 클리어 방지)
            applyFilteringEfficiently()
        }
        
        isInitialDataLoaded = true
    }
    
    /**
     * 두 자산 리스트가 동일한지 비교
     */
    private fun isListsEqual(list1: List<IpAssetItem>, list2: List<IpAssetItem>): Boolean {
        if (list1.size != list2.size) return false
        
        return list1.zip(list2).all { (item1, item2) ->
            item1.currencyCode == item2.currencyCode &&
            kotlin.math.abs(item1.amount - item2.amount) < 0.001 &&
            kotlin.math.abs(item1.usdEquivalent - item2.usdEquivalent) < 0.001
        }
    }

    /**
     * 빈 상태 표시
     */
    private fun showEmptyState() {
        if (_binding == null) return
        assetsList.clear()
        applyFiltering()
        binding.totalIpAssets.text = "$0.00 USD"
        android.util.Log.d("IpAssetFragment", "빈 상태로 자산 표시")
    }
    
    private fun setupSearchAndFilter() {
        // 검색 기능 - TextWatcher 추가
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                applySearchFilter(s?.toString() ?: "")
            }
        })
        
        // 검색 - 키보드 엔터 이벤트
        binding.searchEditText.setOnEditorActionListener { textView, _, _ ->
            applySearchFilter(textView.text.toString())
            false
        }
        
        // 전체 필터
        binding.filterAll.setOnClickListener {
            currentFilter = FilterType.ALL
            updateFilterButtonUI()
            applyFiltering(binding.searchEditText.text.toString())
        }
        
        // 보유중 필터
        binding.filterHeld.setOnClickListener {
            currentFilter = FilterType.HOLDING
            updateFilterButtonUI()
            applyFiltering(binding.searchEditText.text.toString())
        }
        
        // 초기 필터 UI 상태 설정 (전체 필터가 기본값)
        updateFilterButtonUI()
    }
    
    private fun updateFilterButtonUI() {
        if (currentFilter == FilterType.ALL) {
            binding.filterAll.setBackgroundResource(R.drawable.bg_filter_active)
            binding.filterAll.setTextColor(requireContext().getColor(R.color.sky_30C6E8_100))
            binding.filterHeld.setBackgroundResource(R.drawable.bg_filter_inactive)
            binding.filterHeld.setTextColor(requireContext().getColor(android.R.color.darker_gray))
        } else {
            binding.filterHeld.setBackgroundResource(R.drawable.bg_filter_active)
            binding.filterHeld.setTextColor(requireContext().getColor(R.color.sky_30C6E8_100))
            binding.filterAll.setBackgroundResource(R.drawable.bg_filter_inactive)
            binding.filterAll.setTextColor(requireContext().getColor(android.R.color.darker_gray))
        }
    }
    
    private fun applySearchFilter(query: String) {
        applyFiltering(query)
    }
    
    private fun applyFiltering(searchQuery: String = "") {
        // 효율적인 필터링 적용
        applyFilteringEfficiently(searchQuery)
    }
    
    private fun applyFilteringEfficiently(searchQuery: String = "") {
        // onResume에서 호출될 때는 항상 업데이트하도록 수정
        val currentFilterKey = "${currentFilter}_$searchQuery"
        val shouldSkip = lastAppliedFilter == currentFilterKey && isInitialDataLoaded && !isResuming
        
        if (shouldSkip) {
            Log.d("IpAssetFragment", "필터링 건너뛰기: $currentFilterKey")
            return
        }
        
        Log.d("IpAssetFragment", "필터링 적용: $currentFilterKey, assetsList.size=${assetsList.size}")
        
        // 1. 먼저 USD 항상 포함
        val newFilteredList = mutableListOf<IpAssetItem>()
        val usd = assetsList.firstOrNull { it.isUsd }
        
        if (usd != null && (currentFilter == FilterType.ALL || usd.amount > 0)) {
            newFilteredList.add(usd)
        }
        
        // 2. 검색어와 필터에 맞는 티커들 추가
        val filteredTickers = assetsList
            .filter { !it.isUsd } // USD 아닌 것들만
            .filter { 
                // 검색어 필터링
                if (searchQuery.isNotEmpty()) {
                    it.currencyCode.contains(searchQuery, ignoreCase = true)
                } else true
            }
            .filter {
                // 보유중 필터링
                if (currentFilter == FilterType.HOLDING) it.amount > 0 else true 
            }
            .sortedBy { it.currencyCode } // 알파벳 순 정렬
        
        newFilteredList.addAll(filteredTickers)
        
        // 3. 결과 적용 (ListAdapter는 내부적으로 DiffUtil 사용)
        filteredList.clear()
        filteredList.addAll(newFilteredList)
        adapter.submitList(filteredList.toList())
        
        Log.d("IpAssetFragment", "필터링 완료: ${newFilteredList.size}개 항목 표시")
        
        // 필터 키 저장
        lastAppliedFilter = currentFilterKey
        isResuming = false // onResume 플래그 리셋
    }
    
    private fun setupSwipeRefresh() {
        // 스와이프 리프레시 설정
        binding.swipeRefreshLayout.setOnRefreshListener {
            // 이미 로딩 중이면 리프레시 무시
            if (isDataLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
                return@setOnRefreshListener
            }
            
            // 포트폴리오 데이터 새로고침
            loadBalancesFromHistory()
            
            // 1초 후 리프레시 종료
            Handler(Looper.getMainLooper()).postDelayed({
                if (binding.swipeRefreshLayout.isRefreshing) {
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }, 1000) // 1초 후 리프레시 인디케이터 중지
        }
        
        // 리프레시 색상 설정
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.sky_30C6E8_100
        )
    }
    
    override fun onResume() {
        super.onResume()
        // 입출금 헤더 텍스트 설정
        (activity as? MainActivity)?.setHeaderTitle("입출금")
        
        // 헤더 레이아웃 표시
        val headerLayout = requireActivity().findViewById<View>(R.id.headerLayout)
        headerLayout?.visibility = View.VISIBLE
        
        // 헤더 타이틀 표시
        val headerTitle = requireActivity().findViewById<TextView>(R.id.headerTitle)
        headerTitle?.visibility = View.VISIBLE
        
        // onResume 플래그 설정
        isResuming = true
        
        // 뒤로가기 후 돌아올 때 항상 데이터 복원 및 UI 업데이트
        if (isInitialDataLoaded) {
            // 1. 먼저 캐시에서 데이터 복원 시도
            val cachedAssets = loadCachedAssets()
            if (cachedAssets.isNotEmpty()) {
                assetsList.clear()
                assetsList.addAll(cachedAssets)
                updateTotalAssetsDisplay()
                applyFilteringEfficiently()
                Log.d("IpAssetFragment", "onResume: 캐시에서 ${cachedAssets.size}개 자산 복원")
            } else if (assetsList.isNotEmpty()) {
                // 2. 기존 데이터가 있으면 그대로 사용
                updateTotalAssetsDisplay()
                applyFilteringEfficiently()
                Log.d("IpAssetFragment", "onResume: 기존 ${assetsList.size}개 자산 유지")
            } else {
                // 3. 데이터가 없으면 기본값으로 초기화
                initializeUI()
                Log.d("IpAssetFragment", "onResume: 기본값으로 초기화")
            }
            
            // 4. 백그라운드에서 최신 데이터 로드
            loadBalancesFromHistory()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    /**
     * 전화 사기 경고 다이얼로그 표시
     * MainActivity의 shouldShowPhoneFraudAlert 로직을 확인 후 표시
     */
    private fun showPhoneFraudAlertDialogIfNeeded() {
        // 액티비티가 MainActivity인지 확인
        val mainActivity = activity as? MainActivity
        mainActivity?.let {
            // MainActivity의 shouldShowPhoneFraudAlert 로직 사용
            val sharedPreferences = requireContext().getSharedPreferences("phone_fraud_alert_prefs", android.content.Context.MODE_PRIVATE)
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val lastShownDate = sharedPreferences.getString("last_shown_date", "")
            val tempShownDate = sharedPreferences.getString("temp_shown_date", "")
            
            // 임시 플래그가 있으면 제거하고 다이얼로그 표시하지 않음
            if (tempShownDate == today) {
                sharedPreferences.edit().remove("temp_shown_date").apply()
                return
            }
            
            // 오늘 이미 표시하지 않았으면 다이얼로그 표시
            if (lastShownDate != today) {
                it.showPhoneFraudAlertDialog()
            }
        }
    }
    
    companion object {
        fun newInstance() = IpAssetFragment()
    }
}

// 자산 데이터 클래스
data class IpAssetItem(
    val currencyCode: String,
    val symbol: String = "", // symbol 정보 (예: IJECT/USD)
    val amount: Double,
    val usdEquivalent: Double,
    val krwEquivalent: Double,
    val isUsd: Boolean
)

// 리사이클러뷰 어댑터
class IpAssetsAdapter(private val fragment: Fragment) : ListAdapter<IpAssetItem, RecyclerView.ViewHolder>(AssetDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USD = 0
        private const val VIEW_TYPE_TICKER = 1
    }
    
    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isUsd) VIEW_TYPE_USD else VIEW_TYPE_TICKER
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USD -> {
                val binding = ItemUsdAssetBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                UsdAssetViewHolder(binding)
            }
            else -> {
                val binding = ItemTickerAssetBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                TickerViewHolder(binding)
            }
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is UsdAssetViewHolder -> holder.bind(item)
            is TickerViewHolder -> holder.bind(item)
        }
    }
    
    // USD 뷰홀더
    inner class UsdAssetViewHolder(private val binding: ItemUsdAssetBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: IpAssetItem) {
            binding.name.text = item.currencyCode
            val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
                roundingMode = java.math.RoundingMode.DOWN
            }
            binding.amount.text = formatter.format(item.amount)
            
            // USD 클릭 시 USDTransactionFragment로 이동
            binding.root.setOnClickListener {
                try {
                    // fragment 인스턴스를 사용
                    val fragmentManager = fragment.requireActivity().supportFragmentManager
                    val fragmentTransaction = fragmentManager.beginTransaction()
                    
                    // 애니메이션 추가
                    fragmentTransaction.setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                    )
                    
                    // 상위 액티비티의 헤더 숨기기
                    val headerLayout = fragment.requireActivity().findViewById<View>(R.id.headerLayout)
                    headerLayout?.visibility = View.GONE
                    
                    // 새 USDTransactionFragment 인스턴스 생성
                    val transactionFragment = com.stip.ipasset.usd.fragment.USDTransactionFragment()
                    
                    // Bundle에 USD 데이터 전달
                    val bundle = Bundle().apply {
                        putString("currencyCode", item.currencyCode)
                        putDouble("amount", item.amount)
                    }
                    transactionFragment.arguments = bundle
                    
                    // 프래그먼트 교체 및 백스택에 추가
                    fragmentTransaction.replace(R.id.fragment_container, transactionFragment)
                    fragmentTransaction.addToBackStack(null)
                    fragmentTransaction.commit()
                } catch (e: Exception) {
                    Toast.makeText(fragment.requireContext(), "화면 전환 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // 티커 뷰홀더
    inner class TickerViewHolder(private val binding: ItemTickerAssetBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: IpAssetItem) {
            // 티커 로고 설정
            val tickerInitials = item.currencyCode.take(2)
            binding.tokenLogoText.text = tickerInitials
            
            // 티커 색상 설정
            val context = binding.root.context
            val colorResId = when(item.currencyCode) {
                "JWV" -> R.color.token_jwv
                "MDM" -> R.color.token_mdm
                "CDM" -> R.color.token_cdm
                "IJECT" -> R.color.token_iject
                "WETALK" -> R.color.token_wetalk
                "SLEEP" -> R.color.token_sleep
                "KCOT" -> R.color.token_kcot
                "MSK" -> R.color.token_msk
                "SMT" -> R.color.token_smt
                "AXNO" -> R.color.token_axno
                "KATV" -> R.color.token_katv
                else -> R.color.token_usd
            }
            binding.tokenLogoBackground.backgroundTintList = context.getColorStateList(colorResId)
            
            // 티커 이름과 가격 설정 (symbol 표시)
            binding.name.text = item.symbol
            
            val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
                roundingMode = java.math.RoundingMode.DOWN
            }
            binding.amount.text = formatter.format(item.amount)
            
            val usdFormatter = NumberFormat.getNumberInstance(Locale.US).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
                roundingMode = java.math.RoundingMode.DOWN
            }
            binding.usdAmount.text = "$${usdFormatter.format(item.usdEquivalent)}"
            
            // 클릭 이벤트 처리 - 티커 트랜잭션 화면으로 이동
            binding.root.setOnClickListener {
                try {
                    // fragment 인스턴스를 사용
                    val fragmentManager = fragment.requireActivity().supportFragmentManager
                    val fragmentTransaction = fragmentManager.beginTransaction()
                    
                    // 애니메이션 추가
                    fragmentTransaction.setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                    )
                    
                    // 상위 액티비티의 헤더 숨기기
                    val headerLayout = fragment.requireActivity().findViewById<View>(R.id.headerLayout)
                    headerLayout?.visibility = View.GONE
                    
                    // 티커 트랜잭션 프래그먼트 생성
                    val tickerTransactionFragment = TickerTransactionFragment.newInstance(
                        tickerCode = item.currencyCode,
                        amount = item.amount,
                        usdEquivalent = item.usdEquivalent
                    )
                    
                    // 프래그먼트 교체 및 백스택에 추가
                    fragmentTransaction.replace(R.id.fragment_container, tickerTransactionFragment)
                    fragmentTransaction.addToBackStack(null)
                    fragmentTransaction.commit()
                } catch (e: Exception) {
                    Toast.makeText(fragment.requireContext(), "화면 전환 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

// DiffUtil
class AssetDiffCallback : DiffUtil.ItemCallback<IpAssetItem>() {
    override fun areItemsTheSame(oldItem: IpAssetItem, newItem: IpAssetItem): Boolean {
        return oldItem.currencyCode == newItem.currencyCode
    }
    
    override fun areContentsTheSame(oldItem: IpAssetItem, newItem: IpAssetItem): Boolean {
        return oldItem == newItem
    }
}
