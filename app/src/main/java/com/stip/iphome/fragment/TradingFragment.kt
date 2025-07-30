package com.stip.stip.iphome.fragment

import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.stip.stip.iphome.listener.InfoTabListener
import com.stip.stip.R
import com.stip.stip.databinding.FragmentTradingDetailBinding
import com.stip.stip.iphome.TradingDataHolder
import com.stip.stip.iphome.model.IpListingItem
import java.text.DecimalFormat
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

// --- ✅ 1. InfoTabListener 인터페이스 구현 추가 ---
class TradingFragment : Fragment(), InfoTabListener {

    private var _binding: FragmentTradingDetailBinding? = null // ❗️ 실제 바인딩 클래스 이름 확인
    private val binding get() = _binding!!
    private var currentSelectedMenuId: Int = R.id.menu_item_order // 기본은 '주문' 메뉴


    private lateinit var marqueeHandler: Handler
    private lateinit var marqueeRunnable: Runnable
    private var marqueeIndex = 0
    private val marqueeInterval = 5000L

    // 주기적 가격 업데이트를 위한 변수들
    private lateinit var priceUpdateHandler: Handler
    private lateinit var priceUpdateRunnable: Runnable
    private val priceUpdateInterval = 5000L
    
    // 마켓페어 정보 폴링을 위한 변수들
    private lateinit var marketInfoHandler: Handler
    private lateinit var marketInfoRunnable: Runnable
    private val marketInfoInterval = 5000L // 5초 폴링

    private var currentTicker: String? = null
    private var companyName: String? = null

    private val fixedTwoDecimalFormatter = DecimalFormat("#,##0.00").apply {
        decimalFormatSymbols =
            decimalFormatSymbols.apply { groupingSeparator = ','; decimalSeparator = '.' }
        minimumFractionDigits = 2; maximumFractionDigits = 2
        roundingMode = java.math.RoundingMode.DOWN
    }
    private val numberParseFormat = DecimalFormat.getNumberInstance(Locale.US) as DecimalFormat

    companion object {
        private const val ARG_TICKER = "ticker"
        private const val ARG_COMPANY_NAME = "companyName"
        private const val TAG = "TradingFragment"

        fun newInstance(ticker: String, companyName: String): TradingFragment {
            return TradingFragment().apply {
                val bundle = arguments ?: Bundle()
                bundle.putString(ARG_TICKER, ticker)
                bundle.putString(ARG_COMPANY_NAME, companyName)
                arguments = bundle
            }
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentTicker = it.getString(ARG_TICKER)
            companyName = it.getString(ARG_COMPANY_NAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentTradingDetailBinding.inflate(inflater, container, false) // ❗️ 실제 바인딩 클래스 사용
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // IP홈 헤더 텍스트 제거를 위해 액션바 완전히 숨김
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        
        // MainActivity의 headerLayout도 숨김
        activity?.findViewById<View>(R.id.headerLayout)?.visibility = View.GONE

        val marqueeViews = listOf(
            binding.marqueeText1,
            binding.marqueeText2,
            binding.marqueeText3
        )

        marqueeViews.forEach { textView ->
            textView.viewTreeObserver.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        val height = textView.height.toFloat()
                        val anim = TranslateAnimation(0f, 0f, height, -height).apply {
                            duration = 5000
                            repeatCount = Animation.INFINITE
                            repeatMode = Animation.RESTART
                        }
                        textView.startAnimation(anim)
                        textView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            )
        }

        // ✅ 마르퀴 텍스트 순환 시작
        setupMarqueeText()

        // TradingDataHolder에 데이터가 로드될 때까지 대기
        if (TradingDataHolder.ipListingItems.isEmpty()) {
            // 1초 후에 다시 시도
            Handler(Looper.getMainLooper()).postDelayed({
                setupTopInfoAndPriceData()
            }, 1000)
        } else {
            setupTopInfoAndPriceData()
        }
        
        setupMenuClickListeners()

        // 주기적 가격 업데이트 시작
        startPriceUpdateTimer()
        
        // 마켓페어 정보 폴링 시작
        startMarketInfoPolling()

        if (savedInstanceState == null) {
            val selectedMenuId =
                arguments?.getInt("selectedMenuId", R.id.menu_item_order) ?: R.id.menu_item_order

            updateMenuSelection(selectedMenuId)

            when (selectedMenuId) {
                R.id.menu_item_order -> replaceFragment(
                    OrderContentViewFragment.newInstance(
                        currentTicker
                    )
                )

                R.id.menu_item_chart -> replaceFragment(TradingChartFragment.newInstance(currentTicker))
                R.id.menu_item_quotes -> replaceFragment(
                    IpHomeQuotesFragment.newInstance(
                        currentTicker
                    )
                )

                R.id.menu_item_info -> replaceFragment(IpHomeInfoFragment.newInstance(currentTicker))
            }
        }
    }

    /**
     * 주기적 가격 업데이트 타이머 시작
     */
    private fun startPriceUpdateTimer() {
        priceUpdateHandler = Handler(Looper.getMainLooper())
        priceUpdateRunnable = object : Runnable {
            override fun run() {
                refreshPriceData()
                priceUpdateHandler.postDelayed(this, priceUpdateInterval)
            }
        }
        priceUpdateHandler.post(priceUpdateRunnable)
    }
    
    /**
     * 마켓페어 정보 폴링 시작
     */
    private fun startMarketInfoPolling() {
        marketInfoHandler = Handler(Looper.getMainLooper())
        marketInfoRunnable = object : Runnable {
            override fun run() {
                refreshMarketInfo()
                marketInfoHandler.postDelayed(this, marketInfoInterval)
            }
        }
        marketInfoHandler.post(marketInfoRunnable)
        Log.d(TAG, "마켓페어 정보 폴링 시작 - 5초 간격")
    }
    
    /**
     * 마켓페어 정보 새로고침 (실시간 시세 갱신용)
     */
    private fun refreshMarketInfo() {
        if (_binding == null || !isAdded || currentTicker == null) return
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "마켓페어 정보 새로고침 시작: $currentTicker")
                
                // 티커로부터 marketPairId 가져오기
                val ipListingRepository = com.stip.stip.api.repository.IpListingRepository()
                val marketPairId = ipListingRepository.getPairIdForTicker(currentTicker)
                
                if (marketPairId != null) {
                    // Market API 호출
                    val marketRepository = com.stip.stip.api.repository.MarketRepository()
                    val marketResponse = marketRepository.getMarket(marketPairId)
                    
                    if (marketResponse != null) {
                        Log.d(TAG, "마켓페어 정보 업데이트: lastPrice=${marketResponse.lastPrice}, volume=${marketResponse.volume}")
                        
                        // TradingDataHolder 업데이트 (실시간 시세 동기화)
                        updateTradingDataHolder(marketResponse)
                        
                        // UI 업데이트
                        updateMarketDetailInfo(marketResponse)
                        
                        // 현재가 업데이트
                        updateCurrentPriceFromMarketResponse(marketResponse)
                        
                    } else {
                        Log.w(TAG, "마켓페어 정보 응답이 null")
                    }
                } else {
                    Log.w(TAG, "marketPairId를 찾을 수 없음: $currentTicker")
                }
            } catch (e: Exception) {
                Log.e(TAG, "마켓페어 정보 새로고침 실패", e)
            }
        }
    }
    
    /**
     * TradingDataHolder 업데이트 (실시간 시세 동기화)
     */
    private fun updateTradingDataHolder(marketResponse: com.stip.stip.api.model.MarketResponse) {
        try {
            val currentItem = TradingDataHolder.ipListingItems.firstOrNull { it.ticker == currentTicker }
            if (currentItem != null) {
                val lastPrice = marketResponse.lastPrice?.toDouble() ?: 0.0
                val priceChange = marketResponse.priceChange?.toDouble() ?: 0.0
                val changeRate = marketResponse.changeRate ?: 0.0
                val volume = marketResponse.volume?.toDouble() ?: 0.0
                val high = marketResponse.highTicker?.toDouble() ?: 0.0
                val low = marketResponse.lowTicker?.toDouble() ?: 0.0
                
                // 포맷팅
                val formatter = DecimalFormat("#,##0.00").apply { roundingMode = RoundingMode.DOWN }
                val changePercentFormatted = if (changeRate >= 0) "+${formatter.format(changeRate)}%" else "${formatter.format(changeRate)}%"
                val changeAbsoluteFormatted = if (priceChange >= 0) "+${formatter.format(priceChange)}" else formatter.format(priceChange)
                
                val updatedItem = currentItem.copy(
                    currentPrice = formatter.format(lastPrice),
                    changePercent = changePercentFormatted,
                    changeAbsolute = changeAbsoluteFormatted,
                    volume = String.format("%,.0f USD", volume),
                    high24h = formatter.format(high),
                    low24h = formatter.format(low)
                )
                
                val index = TradingDataHolder.ipListingItems.indexOf(currentItem)
                if (index != -1) {
                    val updatedList = TradingDataHolder.ipListingItems.toMutableList()
                    updatedList[index] = updatedItem
                    TradingDataHolder.ipListingItems = updatedList
                    
                    Log.d(TAG, "TradingDataHolder 업데이트 완료: ${updatedItem.currentPrice}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TradingDataHolder 업데이트 실패", e)
        }
    }
    
    /**
     * Market API 응답으로부터 현재가 업데이트
     */
    private fun updateCurrentPriceFromMarketResponse(marketResponse: com.stip.stip.api.model.MarketResponse) {
        try {
            val lastPrice = marketResponse.lastPrice?.toFloat() ?: 0f
            if (lastPrice > 0) {
                // 현재가 텍스트 업데이트
                binding.currentPriceText.text = formatPrice(lastPrice.toString())
                
                // OrderDataCoordinator 업데이트 (주문창 동기화)
                val globalCoordinator = OrderContentViewFragment.getGlobalOrderDataCoordinator()
                globalCoordinator?.updateCurrentPrice(lastPrice)
                
                Log.d(TAG, "현재가 업데이트: $lastPrice")
            }
        } catch (e: Exception) {
            Log.e(TAG, "현재가 업데이트 실패", e)
        }
    }

    /**
     * 가격 데이터 새로고침
     */
    private fun refreshPriceData() {
        if (_binding == null || !isAdded || currentTicker == null) return
        
        try {
            Log.d(TAG, "가격 데이터 새로고침 시작: $currentTicker")
            
            // TradingDataHolder에서 최신 데이터 가져오기
            val currentItemData = TradingDataHolder.ipListingItems.firstOrNull { it.ticker == currentTicker }
            
            if (currentItemData != null) {
                // UI 업데이트
                displayPriceInfo(currentItemData)
                
                // 개별 마켓 상세 정보도 새로고침
                loadMarketDetailInfo(currentTicker)
                
                Log.d(TAG, "가격 데이터 새로고침 완료: ${currentItemData.currentPrice}")
            } else {
                Log.w(TAG, "현재 티커에 대한 데이터를 찾을 수 없음: $currentTicker")
            }
        } catch (e: Exception) {
            Log.e(TAG, "가격 데이터 새로고침 중 오류 발생", e)
        }
    }

    /**
     * 강제로 가격 데이터 새로고침 (주문 체결 후 호출)
     */
    fun forceRefreshPriceData() {
        Log.d(TAG, "강제 가격 데이터 새로고침 요청")
        if (_binding != null && isAdded) {
            refreshPriceData()
        }
    }

    private fun setupMarqueeText() {
        val ipList = TradingDataHolder.ipListingItems
        if (ipList.isEmpty()) return

        val textViews = listOf(
            binding.marqueeText1,
            binding.marqueeText2,
            binding.marqueeText3
        )

        marqueeHandler = Handler(Looper.getMainLooper())

        marqueeRunnable = object : Runnable {
            override fun run() {
                for (i in textViews.indices) {
                    val itemIndex = (marqueeIndex + i) % ipList.size
                    val item = ipList[itemIndex]

                    val price = item.currentPrice.toDoubleOrNull() ?: 0.0
                    val formattedPrice = String.format("$%,.2f", price)

                    // ✅ 티커 부분만 Bold 처리
                    val fullText = "${item.ticker} $formattedPrice"
                    val spannable = SpannableString(fullText)
                    spannable.setSpan(
                        StyleSpan(Typeface.BOLD),
                        0,
                        item.ticker.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    textViews[i].text = spannable
                }

                marqueeIndex = (marqueeIndex + 1) % ipList.size
                marqueeHandler.postDelayed(this, marqueeInterval)
            }
        }

        marqueeHandler.post(marqueeRunnable)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - 화면 복귀 시 가격 데이터 새로고침")
        
        // IP홈 헤더 텍스트 제거를 위해 액션바 숨김 (onResume에서도 확실히 적용)
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        
        // MainActivity의 headerLayout도 숨김 (onResume에서도 확실히 적용)
        activity?.findViewById<View>(R.id.headerLayout)?.visibility = View.GONE
        
        // 화면으로 돌아올 때마다 가격 데이터 새로고침
        refreshPriceData()
        
        // 주기적 업데이트 재시작
        startPriceUpdateTimer()
        startMarketInfoPolling()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause - 주기적 업데이트 중지")
        
        // 주기적 업데이트 중지
        if (::priceUpdateHandler.isInitialized) {
            priceUpdateHandler.removeCallbacks(priceUpdateRunnable)
        }
        if (::marketInfoHandler.isInitialized) {
            marketInfoHandler.removeCallbacks(marketInfoRunnable)
        }
    }

    private fun setupTopInfoAndPriceData() {
        // 티커/USD 형식으로 간단하게 표시
        val displayText = if (currentTicker.isNullOrBlank()) {
            ""
        } else {
            "$currentTicker/USD"
        }
        
        binding.textCompanyName.text = displayText
        binding.icArrowIcon.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        // TODO: more_options_icon 리스너

        val currentItemData = TradingDataHolder.ipListingItems.firstOrNull { it.ticker == currentTicker }
        if (currentItemData != null) {
            displayPriceInfo(currentItemData)
            // 개별 마켓 정보도 가져와서 업데이트
            loadMarketDetailInfo(currentTicker)
        } else {
            setDefaultPriceInfo()
        }

        binding.prevItemIndicator.setOnClickListener { navigateToSiblingTicker(-1) }
        binding.nextItemIndicator.setOnClickListener { navigateToSiblingTicker(1) }
    }

    /**
     * 개별 마켓 상세 정보를 가져와서 UI 업데이트
     */
    private fun loadMarketDetailInfo(ticker: String?) {
        if (ticker == null) return
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 티커로부터 marketPairId 가져오기
                val ipListingRepository = com.stip.stip.api.repository.IpListingRepository()
                val marketPairId = ipListingRepository.getPairIdForTicker(ticker)
                
                if (marketPairId != null) {
                    Log.d("TradingFragment", "티커 상세 정보 조회 로딩: $ticker, marketPairId: $marketPairId")
                    
                    // Market API 호출
                    val marketRepository = com.stip.stip.api.repository.MarketRepository()
                    val marketResponse = marketRepository.getMarket(marketPairId)
                    
                    if (marketResponse != null) {
                        Log.d("TradingFragment", "티커 상세 조회: volume=${marketResponse.volume}, highTicker=${marketResponse.highTicker}, lowTicker=${marketResponse.lowTicker}")
                        
                        // UI 업데이트
                        updateMarketDetailInfo(marketResponse)
                    } else {
                        Log.e("TradingFragment", "상세 조회 NULL")
                    }
                } else { Log.e("TradingFragment", "티커 id 못찾음: $ticker")
                }
            } catch (e: Exception) {
                Log.e("TradingFragment", "Failed to load market detail", e)
            }
        }
    }

    /**
     * 마켓 상세 정보로 UI 업데이트
     */
    private fun updateMarketDetailInfo(marketResponse: com.stip.stip.api.model.MarketResponse) {
        if (_binding == null || !isAdded) return
        
        try {
            // Volume 업데이트
            val volumeFormatted = formatVolume(marketResponse.volume?.toString() ?: "0")
            binding.textVolumeValue24h.text = volumeFormatted
            
            // High/Low 업데이트
            val highFormatted = formatPrice(marketResponse.highTicker?.toString() ?: "0")
            val lowFormatted = formatPrice(marketResponse.lowTicker?.toString() ?: "0")
            
            binding.textHighValue24h.text = highFormatted
            binding.textLowValue24h.text = lowFormatted
            
            Log.d("TradingFragment", "티커 상세 정보: volume=$volumeFormatted, high=$highFormatted, low=$lowFormatted")
            
        } catch (e: Exception) {
            Log.e("TradingFragment", "티커 상세 정보 에러", e)
        }
    }

    private fun displayPriceInfo(item: IpListingItem) {
        if (_binding == null || !isAdded) return
        val ctx = context ?: return
        try {
            binding.currentPriceText.text = formatPrice(item.currentPrice)

            // changePercent 안전하게 처리
            val changePercentValue = try {
                item.changePercent.replace("%", "").replace("+", "").toDoubleOrNull() ?: 0.0
            } catch (e: Exception) {
                0.0
            }
            
            val truncatedPercent = (if (changePercentValue >= 0) "+" else "") + BigDecimal(changePercentValue).setScale(2, RoundingMode.DOWN).toPlainString() + "%"
            binding.percentageChangeText.text = truncatedPercent
            
            // changeAbsolute 안전하게 처리
            val changeAbsoluteValue = try {
                item.changeAbsolute.replace("+", "").toDoubleOrNull() ?: 0.0
            } catch (e: Exception) {
                0.0
            }
            binding.absoluteChangeText.text = BigDecimal(changeAbsoluteValue).setScale(2, RoundingMode.DOWN).toPlainString()
            
            binding.textVolumeValue24h.text = formatVolume(item.volume)
            binding.textHighValue24h.text = formatPrice(item.high24h)
            binding.textLowValue24h.text = formatPrice(item.low24h)

            val color = when {
                item.changePercent.startsWith("+") -> ContextCompat.getColor(
                    ctx,
                    R.color.percentage_positive_red
                )

                item.changePercent.startsWith("-") -> ContextCompat.getColor(
                    ctx,
                    R.color.percentage_negative_blue
                )

                else -> ContextCompat.getColor(ctx, R.color.text_primary)
            }
            binding.currentPriceText.setTextColor(color)
            binding.percentageChangeText.setTextColor(color)
            binding.absoluteChangeText.setTextColor(color)
            binding.textHighValue24h.setTextColor(
                ContextCompat.getColor(
                    ctx,
                    R.color.percentage_positive_red
                )
            )
            binding.textLowValue24h.setTextColor(
                ContextCompat.getColor(
                    ctx,
                    R.color.percentage_negative_blue
                )
            )

            val iconRes = when {
                item.changePercent.startsWith("+") -> R.drawable.ic_arrow_up_red
                item.changePercent.startsWith("-") -> R.drawable.ic_arrow_down_blue
                else -> 0
            }
            binding.changeIndicatorIcon.visibility = if (iconRes != 0) {
                binding.changeIndicatorIcon.setImageResource(iconRes); View.VISIBLE
            } else View.GONE

        } catch (e: Exception) {
            Log.e("TradingFragment", "상세 정보 디스플레이 에러", e)
            setDefaultPriceInfo()
        }
    }

    private fun setDefaultPriceInfo() {
        if (_binding == null || !isAdded) return
        binding.currentPriceText.text = "0.00"
        binding.percentageChangeText.text = "0.00%"
        binding.absoluteChangeText.text = "0.00"
        binding.textVolumeValue24h.text = "0.00"
        binding.textHighValue24h.text = "0.00"
        binding.textLowValue24h.text = "0.00"
        binding.changeIndicatorIcon.visibility = View.GONE
        context?.let {
            val defaultColor = ContextCompat.getColor(it, R.color.text_primary)
            binding.currentPriceText.setTextColor(defaultColor)
            binding.percentageChangeText.setTextColor(defaultColor)
            binding.absoluteChangeText.setTextColor(defaultColor)
            binding.textHighValue24h.setTextColor(defaultColor)
            binding.textLowValue24h.setTextColor(defaultColor)
        }
    }

    private fun formatPrice(priceString: String?): String {
        return try {
            if (priceString.isNullOrBlank()) return "0"
            
            // 숫자와 소수점, 부호만 추출
            val cleanPrice = priceString.replace(Regex("[^\\d.-]"), "")
            if (cleanPrice.isBlank()) return "0"
            
            val number = cleanPrice.toDoubleOrNull() ?: 0.0
            if (number == 0.0) return "0"
            
            // DecimalFormat으로 포맷팅 (올림/반올림 없이)
            val formatter = DecimalFormat("#,##0.00").apply {
                roundingMode = RoundingMode.DOWN
                isGroupingUsed = true
            }
            
            val formatted = formatter.format(Math.abs(number))
            val prefix = if (priceString.startsWith("-")) "-" else if (priceString.startsWith("+")) "+" else ""
            
            prefix + formatted
        } catch (e: Exception) {
            "0"
        }
    }

    private fun formatVolume(volumeString: String?): String {
        return try {
            if (volumeString.isNullOrBlank()) return "0.00"
            // 소수점을 포함한 숫자 처리
            val numberPart = volumeString.replace(Regex("[^\\d.]"), "")
            val number = numberPart.toDoubleOrNull() ?: 0.0
            fixedTwoDecimalFormatter.format(number)
        } catch (e: Exception) {
            "0.00"
        }
    }

    private fun navigateToSiblingTicker(offset: Int) {
        val fullIpList = TradingDataHolder.ipListingItems
        if (fullIpList.isEmpty()) return

        val currentIndex = fullIpList.indexOfFirst { it.ticker == currentTicker }
        if (currentIndex == -1) return

        val targetIndex = (currentIndex + offset + fullIpList.size) % fullIpList.size
        val targetItem = fullIpList[targetIndex]

        // 현재 TradingFragment 상태 유지한 채, 헤더와 내부 Fragment만 업데이트
        currentTicker = targetItem.ticker
        companyName = targetItem.companyName

        // 헤더 정보 업데이트 - 티커/USD 형식으로 간단하게 표시
        binding.textCompanyName.text = if (currentTicker.isNullOrBlank()) "" else "$currentTicker/USD"
        displayPriceInfo(targetItem)

        // 현재 childFragment에 티커 업데이트 전달
        val currentChild = childFragmentManager.findFragmentById(R.id.trading_content_container)
        when (currentChild) {
            is OrderContentViewFragment -> currentChild.updateTicker(currentTicker)
            is TradingChartFragment -> currentChild.updateTicker(currentTicker)
            is IpHomeQuotesFragment -> currentChild.updateTicker(currentTicker)
            is IpHomeInfoFragment -> currentChild.updateTicker(currentTicker)
            is IpHomeInfoDetailFragment -> {
                // IpHomeInfoDetailFragment도 있다면 처리
                val item =
                    TradingDataHolder.ipListingItems.firstOrNull { it.ticker == currentTicker }
                item?.let { detailItem ->
                    val detailFragment = IpHomeInfoDetailFragment.newInstance(detailItem)
                    childFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace(R.id.trading_content_container, detailFragment, "info_detail")
                    }
                }
            }
        }
    }


    private fun setupMenuClickListeners() {
        binding.menuItemOrder.setOnClickListener {
            replaceFragment(OrderContentViewFragment.newInstance(currentTicker))
            updateMenuSelection(it.id)
        }
        binding.menuItemChart.setOnClickListener {
            replaceFragment(TradingChartFragment.newInstance(currentTicker))
            updateMenuSelection(it.id)
        }
        binding.menuItemQuotes.setOnClickListener {
            replaceFragment(IpHomeQuotesFragment.newInstance(currentTicker))
            updateMenuSelection(it.id)
        }
        binding.menuItemInfo.setOnClickListener {
            // '정보' 메뉴 클릭 시 IpHomeInfoFragment 로드
            replaceFragment(IpHomeInfoFragment.newInstance(currentTicker))
            updateMenuSelection(it.id)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        // 이 TradingFragment 내부의 컨테이너 내용을 교체
        childFragmentManager.beginTransaction()
            .replace(
                R.id.trading_content_container,
                fragment
            ) // ❗️ 이 ID가 fragment_trading_detail.xml의 컨테이너 ID인지 확인
            .commit()
    }


    private fun updateMenuSelection(selectedMenuId: Int) {
        // ✅ 현재 선택된 메뉴 ID를 저장 (티커 이동 시에도 사용)
        currentSelectedMenuId = selectedMenuId

        // 🔹 모든 메뉴를 비활성화 스타일로 초기화
        binding.menuItemOrder.setTextAppearance(R.style.DefaultTextStyle_trading_inactive_14)
        binding.menuItemChart.setTextAppearance(R.style.DefaultTextStyle_trading_inactive_14)
        binding.menuItemQuotes.setTextAppearance(R.style.DefaultTextStyle_trading_inactive_14)
        binding.menuItemInfo.setTextAppearance(R.style.DefaultTextStyle_trading_inactive_14)

        // 🔹 선택된 메뉴만 활성화 스타일 적용
        val activeStyle = R.style.DefaultTextStyle_white_14
        when (selectedMenuId) {
            R.id.menu_item_order -> binding.menuItemOrder.setTextAppearance(activeStyle)
            R.id.menu_item_chart -> binding.menuItemChart.setTextAppearance(activeStyle)
            R.id.menu_item_quotes -> binding.menuItemQuotes.setTextAppearance(activeStyle)
            R.id.menu_item_info -> binding.menuItemInfo.setTextAppearance(activeStyle)
        }
    }

    override fun onDetailsTabSelected(ticker: String?) {
        if (ticker == null) return

        val item = TradingDataHolder.ipListingItems.firstOrNull { it.ticker == ticker }
        if (item != null) {
            val detailFragment = IpHomeInfoDetailFragment.newInstance(item)
            childFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.trading_content_container, detailFragment, "info_detail")
                addToBackStack("info_detail")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 마르퀴 타이머 정리
        if (::marqueeHandler.isInitialized) {
            marqueeHandler.removeCallbacks(marqueeRunnable)
        }
        
        // 가격 업데이트 타이머 정리
        if (::priceUpdateHandler.isInitialized) {
            priceUpdateHandler.removeCallbacks(priceUpdateRunnable)
        }
        
        // 마켓페어 정보 폴링 정리
        if (::marketInfoHandler.isInitialized) {
            marketInfoHandler.removeCallbacks(marketInfoRunnable)
        }
        
        _binding = null
    }
}