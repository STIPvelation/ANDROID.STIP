package com.stip.stip.iphome.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.WebChromeClient
import android.view.MotionEvent
import android.view.GestureDetector
import android.view.ScaleGestureDetector
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.stip.stip.databinding.FragmentTradingChartBinding
import com.stip.stip.api.service.TapiHourlyDataService
import com.stip.stip.api.service.TapiDailyDataService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar
import javax.inject.Inject
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal

// OHLCV 데이터 클래스
data class OHLCVData(
    val date: String,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Float
)

// API 응답 데이터 클래스
data class TickerData(
    val timestamp: String,
    val price: BigDecimal,
    val amount: BigDecimal
)

// 시간 필터 열거형
enum class TimeFilter {
    SECONDS, MINUTES, HOURS, DAYS, WEEKS, MONTHS, YEARS
}

// 분 단위 필터 열거형
enum class MinuteFilter {
    MIN_1, MIN_5, MIN_15
}

@AndroidEntryPoint
class TradingChartFragment : Fragment() {

    private var _binding: FragmentTradingChartBinding? = null
    private val binding get() = _binding!!

    private var ticker: String? = null
    private var ohlcvData: List<OHLCVData> = emptyList()
    private var pollingJob: kotlinx.coroutines.Job? = null
    private var isPollingActive = false

    // 시간 필터 관련 변수
    private var currentTimeFilter: TimeFilter = TimeFilter.HOURS
    private var currentMinuteFilter: MinuteFilter = MinuteFilter.MIN_1
    private var isMinuteSubFilterVisible = false
    
    // 터치 이벤트 처리 변수
    private var gestureDetector: GestureDetector? = null
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var isTouchActive = false
    private var isScaling = false

    @Inject
    lateinit var tapiHourlyDataService: TapiHourlyDataService

    @Inject
    lateinit var tapiDailyDataService: TapiDailyDataService

    companion object {
        private const val ARG_TICKER = "ticker"
        private const val TAG = "TradingChart"
        private const val API_BASE_URL = "https://tapi.sharetheip.com"

        fun newInstance(ticker: String?): TradingChartFragment {
            return TradingChartFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TICKER, ticker)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ticker = arguments?.getString(ARG_TICKER)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTradingChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWebView()
        setupTimeFilters()
        loadTradeData(showLoading = true)
    }

    private fun setupWebView() {
        binding.chartWebView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_NO_CACHE
                
                // WebView 자체 줌 및 스크롤 완전 비활성화
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                
                // 추가 설정
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = false
                
                // 터치 성능 최적화
                setRenderPriority(WebSettings.RenderPriority.HIGH)
                setEnableSmoothTransition(true)
            }
            
            // 터치 이벤트 처리 개선 - 차트 라이브러리 전용
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
            
            // 모든 WebView 스크롤 비활성화
            isHorizontalScrollBarEnabled = false
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            
            // 터치 이벤트 최적화
            isClickable = true
            isLongClickable = false
            
            // 터치 지연 제거
            isHapticFeedbackEnabled = false
            isSoundEffectsEnabled = false
            
            // 네이티브 터치 이벤트 처리 설정
            setupTouchHandling()

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "WebView 페이지 로드 완료")
                    // 페이지 로드 완료 후 약간의 지연을 두고 차트 데이터 설정
                    view?.postDelayed({
                        if (ohlcvData.isNotEmpty()) {
                            updateChartData()
                        }
                    }, 500) // 0.5초 지연
                }

                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.e(TAG, "WebView 오류: $errorCode - $description")
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                    consoleMessage?.let {
                        val level = when (it.messageLevel()) {
                            android.webkit.ConsoleMessage.MessageLevel.ERROR -> "ERROR"
                            android.webkit.ConsoleMessage.MessageLevel.WARNING -> "WARNING"
                            android.webkit.ConsoleMessage.MessageLevel.DEBUG -> "DEBUG"
                            else -> "LOG"
                        }
                        Log.d(TAG, "WebView [$level]: ${it.message()} (line: ${it.lineNumber()}, source: ${it.sourceId()})")
                    }
                    return true
                }
            }


        }
    }
    
    // 네이티브 터치 이벤트 처리 설정
    private fun setupTouchHandling() {
        // 제스처 감지기 설정
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                isTouchActive = true
                isScaling = false // 스케일 상태 초기화
                Log.d(TAG, "터치 시작: (${e.x}, ${e.y}) - 상태 리셋")
                
                // JavaScript로 터치 시작 이벤트 전달
                binding.chartWebView.evaluateJavascript("""
                    if (window.handleNativeTouch) {
                        window.handleNativeTouch('start', ${e.x}, ${e.y});
                    }
                """.trimIndent(), null)
                
                return true
            }
            
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (isTouchActive && !isScaling) {
                    Log.d(TAG, "스크롤: dx=$distanceX, dy=$distanceY, active=$isTouchActive, scaling=$isScaling")
                    
                    // JavaScript로 스크롤 이벤트 전달
                    binding.chartWebView.evaluateJavascript("""
                        if (window.handleNativeTouch) {
                            window.handleNativeTouch('scroll', ${e2.x}, ${e2.y}, $distanceX, $distanceY);
                        }
                    """.trimIndent()) { result ->
                        if (result != "null") {
                            Log.d(TAG, "JavaScript 스크롤 결과: $result")
                        }
                    }
                }
                return true
            }
            
            override fun onLongPress(e: MotionEvent) {
                Log.d(TAG, "롱프레스 감지")
                
                // JavaScript로 롱프레스 이벤트 전달
                binding.chartWebView.evaluateJavascript("""
                    if (window.handleNativeTouch) {
                        window.handleNativeTouch('longpress', ${e.x}, ${e.y});
                    }
                """.trimIndent(), null)
            }
        })
        
        // 스케일 제스처 감지기 설정
        scaleGestureDetector = ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                isScaling = true
                Log.d(TAG, "핀치 줌 시작")
                
                // JavaScript로 스케일 시작 이벤트 전달
                binding.chartWebView.evaluateJavascript("""
                    if (window.handleNativeTouch) {
                        window.handleNativeTouch('scalestart', ${detector.focusX}, ${detector.focusY}, ${detector.scaleFactor});
                    }
                """.trimIndent(), null)
                
                return true
            }
            
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                Log.d(TAG, "핀치 줌: scale=$scaleFactor, span=${detector.currentSpan}, previous=${detector.previousSpan}")
                
                // 스케일 팩터가 1에 너무 가까우면 무시 (노이즈 제거)
                if (Math.abs(scaleFactor - 1.0f) > 0.01f) {
                    // JavaScript로 스케일 이벤트 전달
                    binding.chartWebView.evaluateJavascript("""
                        if (window.handleNativeTouch) {
                            window.handleNativeTouch('scale', ${detector.focusX}, ${detector.focusY}, $scaleFactor);
                        }
                    """.trimIndent(), null)
                }
                
                return true
            }
            
            override fun onScaleEnd(detector: ScaleGestureDetector) {
                isScaling = false
                Log.d(TAG, "핀치 줌 종료")
                
                // JavaScript로 스케일 종료 이벤트 전달
                binding.chartWebView.evaluateJavascript("""
                    if (window.handleNativeTouch) {
                        window.handleNativeTouch('scaleend', ${detector.focusX}, ${detector.focusY});
                    }
                """.trimIndent(), null)
            }
        })
        
        // WebView에 터치 리스너 설정
        binding.chartWebView.setOnTouchListener { _, event ->
            var handled = false
            
            // 스케일 제스처 처리
            scaleGestureDetector?.onTouchEvent(event)?.let { handled = it || handled }
            
            // 일반 제스처 처리 (스케일 중이 아닐 때만)
            if (!isScaling) {
                gestureDetector?.onTouchEvent(event)?.let { handled = it || handled }
            }
            
            // 터치 종료 처리
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    isTouchActive = false
                    isScaling = false
                    Log.d(TAG, "터치 종료 (UP)")
                    
                    // JavaScript로 터치 종료 이벤트 전달
                    binding.chartWebView.evaluateJavascript("""
                        if (window.handleNativeTouch) {
                            window.handleNativeTouch('end', ${event.x}, ${event.y});
                        }
                    """.trimIndent(), null)
                }
                MotionEvent.ACTION_CANCEL -> {
                    isTouchActive = false
                    isScaling = false
                    Log.d(TAG, "터치 취소 (CANCEL)")
                    
                    // JavaScript로 터치 취소 이벤트 전달
                    binding.chartWebView.evaluateJavascript("""
                        if (window.handleNativeTouch) {
                            window.handleNativeTouch('cancel', ${event.x}, ${event.y});
                        }
                    """.trimIndent(), null)
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    // 멀티터치에서 손가락 하나가 떨어질 때
                    if (event.pointerCount <= 2) {
                        isScaling = false
                        Log.d(TAG, "포인터 UP - 스케일 종료")
                    }
                }
            }
            
            handled
        }
    }

    private fun setupTimeFilters() {
        // 메인 시간 필터 버튼들 설정
        binding.btnSeconds.setOnClickListener { selectTimeFilter(TimeFilter.SECONDS) }
        binding.btnMinutes.setOnClickListener { selectTimeFilter(TimeFilter.MINUTES) }
        binding.btnHours.setOnClickListener { selectTimeFilter(TimeFilter.HOURS) }
        binding.btnDays.setOnClickListener { selectTimeFilter(TimeFilter.DAYS) }
        binding.btnWeeks.setOnClickListener { selectTimeFilter(TimeFilter.WEEKS) }
        binding.btnMonths.setOnClickListener { selectTimeFilter(TimeFilter.MONTHS) }
        binding.btnYears.setOnClickListener { selectTimeFilter(TimeFilter.YEARS) }

        // 분 단위 서브 필터 버튼들 설정
        binding.btn1min.setOnClickListener { selectMinuteFilter(MinuteFilter.MIN_1) }
        binding.btn5min.setOnClickListener { selectMinuteFilter(MinuteFilter.MIN_5) }
        binding.btn15min.setOnClickListener { selectMinuteFilter(MinuteFilter.MIN_15) }

        // 초기 상태 설정 (시간 단위 선택)
        updateTimeFilterUI()
    }

    private fun selectTimeFilter(filter: TimeFilter) {
        currentTimeFilter = filter

        // 분 단위 선택 시 서브 필터 표시
        if (filter == TimeFilter.MINUTES) {
            if (!isMinuteSubFilterVisible) {
                binding.minuteSubFilters.visibility = View.VISIBLE
                isMinuteSubFilterVisible = true
            }
        } else {
            // 다른 필터 선택 시 서브 필터 숨김
            if (isMinuteSubFilterVisible) {
                binding.minuteSubFilters.visibility = View.GONE
                isMinuteSubFilterVisible = false
            }
            // 다른 필터 선택 시에만 데이터 로드 (블링크 방지를 위해 로딩 표시 최소화)
            updateTimeFilterUI()
            loadTradeData(showLoading = false)
        }

        // 분 단위 선택 시에는 UI만 업데이트하고 데이터 로드는 하지 않음
        if (filter == TimeFilter.MINUTES) {
            updateTimeFilterUI()
        }
    }

    private fun selectMinuteFilter(filter: MinuteFilter) {
        currentMinuteFilter = filter
        updateMinuteFilterUI()

        // 서브 필터 숨김
        binding.minuteSubFilters.visibility = View.GONE
        isMinuteSubFilterVisible = false

        // 블링크 방지를 위해 로딩 표시 최소화
        loadTradeData(showLoading = false)
    }

    private fun updateTimeFilterUI() {
        // 모든 메인 필터 버튼 선택 상태 해제
        binding.btnSeconds.isSelected = false
        binding.btnMinutes.isSelected = false
        binding.btnHours.isSelected = false
        binding.btnDays.isSelected = false
        binding.btnWeeks.isSelected = false
        binding.btnMonths.isSelected = false
        binding.btnYears.isSelected = false

        // 현재 선택된 필터 버튼만 선택 상태로 설정
        when (currentTimeFilter) {
            TimeFilter.SECONDS -> binding.btnSeconds.isSelected = true
            TimeFilter.MINUTES -> binding.btnMinutes.isSelected = true
            TimeFilter.HOURS -> binding.btnHours.isSelected = true
            TimeFilter.DAYS -> binding.btnDays.isSelected = true
            TimeFilter.WEEKS -> binding.btnWeeks.isSelected = true
            TimeFilter.MONTHS -> binding.btnMonths.isSelected = true
            TimeFilter.YEARS -> binding.btnYears.isSelected = true
        }
    }

    private fun updateMinuteFilterUI() {
        // 모든 분 단위 필터 버튼 선택 상태 해제
        binding.btn1min.isSelected = false
        binding.btn5min.isSelected = false
        binding.btn15min.isSelected = false

        // 현재 선택된 분 단위 필터 버튼만 선택 상태로 설정
        when (currentMinuteFilter) {
            MinuteFilter.MIN_1 -> binding.btn1min.isSelected = true
            MinuteFilter.MIN_5 -> binding.btn5min.isSelected = true
            MinuteFilter.MIN_15 -> binding.btn15min.isSelected = true
        }
    }

    private fun loadTradeData(showLoading: Boolean = true) {
        // Fragment가 destroy된 경우 binding이 null일 수 있으므로 체크
        if (_binding == null) {
            Log.w(TAG, "loadTradeData: binding이 null입니다. Fragment가 destroy되었을 수 있습니다.")
            return
        }

        // 로딩 상태 표시 (폴링 시에는 표시하지 않음)
        if (showLoading) {
            try {
                binding.loadingIndicator.visibility = View.VISIBLE
                // 차트를 숨기지 않고 로딩 오버레이만 표시하여 블링크 방지
                binding.chartWebView.visibility = View.VISIBLE
                binding.emptyStateContainer.visibility = View.GONE
            } catch (e: Exception) {
                Log.e(TAG, "로딩 상태 설정 중 오류: ${e.message}", e)
                return
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Fragment가 destroy된 경우 binding이 null일 수 있으므로 체크
            if (_binding == null) {
                Log.w(TAG, "loadTradeData 코루틴: binding이 null입니다. Fragment가 destroy되었을 수 있습니다.")
                return@launch
            }

            val currentTicker = ticker
            if (currentTicker.isNullOrBlank()) {
                Log.e(TAG, "티커 코드가 없습니다. ticker: $currentTicker")
                showEmptyState()
                return@launch
            }

            try {
                Log.d(TAG, "API 데이터 로드 시작 - ticker: $currentTicker, filter: $currentTimeFilter")

                // 1. 먼저 market/pairs에서 pairId 찾기
                val pairId = getPairIdForTicker(currentTicker)
                if (pairId == null) {
                    Log.e(TAG, "티커에 해당하는 pairId를 찾을 수 없습니다: $currentTicker")
                    showEmptyState()
                    return@launch
                }

                // 2. 모든 필터에서 hourly API 사용 (데이터를 그룹화하여 처리)
                val tickerData = fetchHourlyTickerData(pairId)

                if (tickerData.isEmpty()) {
                    Log.w(TAG, "API 데이터가 비어있습니다. 거래 데이터가 없습니다.")
                    showEmptyState()
                    return@launch
                } else {
                    Log.d(TAG, "API 데이터 로드 완료: ${tickerData.size}개")
                    convertTickerDataToOHLCV(tickerData)
                }

            } catch (e: Exception) {
                Log.e(TAG, "API 데이터 로드 중 오류 발생: ${e.message}", e)
                showEmptyState()
            }
        }
    }

    private suspend fun getPairIdForTicker(ticker: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$API_BASE_URL/api/market/pairs")
            val connection = url.openConnection()
            val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
            val response = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()

            val jsonArray = JSONArray(response.toString())
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val baseAsset = item.getString("baseAsset")
                if (baseAsset == ticker) {
                    return@withContext item.getString("id")
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "pairId 조회 실패: ${e.message}")
            null
        }
    }

    private suspend fun fetchHourlyTickerData(pairId: String): List<TickerData> = withContext(Dispatchers.IO) {
        try {
            // 현재 날짜 기준으로 3개월 전부터 현재까지 데이터 요청
            val calendar = Calendar.getInstance()
            val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            calendar.add(Calendar.MONTH, -3)
            val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            val url = URL("$API_BASE_URL/api/tickers/hourly?marketPairId=$pairId&from=$startDate&to=$endDate")
            val connection = url.openConnection()
            val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
            val response = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()

            val jsonArray = JSONArray(response.toString())
            val tickerDataList = mutableListOf<TickerData>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                tickerDataList.add(
                    TickerData(
                        timestamp = item.getString("timestamp"),
                        price = BigDecimal(item.getString("price")),
                        amount = BigDecimal(item.getString("amount"))
                    )
                )
            }

            // 시간순으로 정렬 (과거순)
            return@withContext tickerDataList.sortedBy { it.timestamp }

        } catch (e: Exception) {
            Log.e(TAG, "hourly ticker 데이터 조회 실패: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchDailyTickerData(pairId: String): List<TickerData> = withContext(Dispatchers.IO) {
        try {
            // 현재 날짜 기준으로 1년 전부터 현재까지 데이터 요청
            val calendar = Calendar.getInstance()
            val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            calendar.add(Calendar.YEAR, -1)
            val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            val url = URL("$API_BASE_URL/api/tickers/daily?marketPairId=$pairId&from=$startDate&to=$endDate")
            val connection = url.openConnection()
            val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
            val response = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()

            val jsonArray = JSONArray(response.toString())
            val tickerDataList = mutableListOf<TickerData>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                tickerDataList.add(
                    TickerData(
                        timestamp = item.getString("timestamp"),
                        price = BigDecimal(item.getString("price")),
                        amount = BigDecimal(item.getString("amount"))
                    )
                )
            }

            // 시간순으로 정렬 (과거순)
            return@withContext tickerDataList.sortedBy { it.timestamp }

        } catch (e: Exception) {
            Log.e(TAG, "daily ticker 데이터 조회 실패: ${e.message}")
            emptyList()
        }
    }

    private fun convertTickerDataToOHLCV(tickerData: List<TickerData>) {
        if (tickerData.isEmpty()) {
            showEmptyState()
            return
        }

        // 선택된 시간 필터에 따라 그룹화 방식 결정
        val groupedData = when (currentTimeFilter) {
            TimeFilter.SECONDS -> groupBySeconds(tickerData)
            TimeFilter.MINUTES -> groupByMinutes(tickerData)
            TimeFilter.HOURS -> groupByHours(tickerData)
            TimeFilter.DAYS -> groupByDays(tickerData)
            TimeFilter.WEEKS -> groupByWeeks(tickerData)
            TimeFilter.MONTHS -> groupByMonths(tickerData)
            TimeFilter.YEARS -> groupByYears(tickerData)
        }

        val ohlcvList = mutableListOf<OHLCVData>()

        groupedData.forEach { (timeKey, dataList) ->
            if (dataList.isNotEmpty()) {
                val sortedData = dataList.sortedBy { it.timestamp }
                val prices = sortedData.map { it.price.toFloat() }
                val volumes = sortedData.map { it.amount.toFloat() }

                val open = prices.first()
                val close = prices.last()
                val high = prices.maxOrNull() ?: open
                val low = prices.minOrNull() ?: open
                val volume = volumes.sum()

                Log.d(TAG, "OHLCV 생성: $timeKey - O:$open, H:$high, L:$low, C:$close, V:$volume")

                ohlcvList.add(
                    OHLCVData(
                        date = timeKey,
                        open = open,
                        high = high,
                        low = low,
                        close = close,
                        volume = volume
                    )
                )
            }
        }

        // 시간순으로 정렬 (과거순 - 왼쪽에서 오른쪽으로 최신순)
        ohlcvData = ohlcvList.sortedBy { it.date }

        Log.d(TAG, "OHLCV 데이터 변환 완료: ${ohlcvData.size}개 (필터: $currentTimeFilter)")

        activity?.runOnUiThread {
            updateChart()
        }
    }

    private fun groupBySeconds(tickerData: List<TickerData>): Map<String, List<TickerData>> {
        return tickerData.groupBy { data ->
            val timestamp = data.timestamp
            val utcDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            utcDateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val utcDate = utcDateFormat.parse(timestamp)

            val kstTimeZone = TimeZone.getTimeZone("Asia/Seoul")
            val calendar = Calendar.getInstance(kstTimeZone)
            calendar.time = utcDate
            calendar.set(Calendar.MILLISECOND, 0)

            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(calendar.time)
        }
    }

    private fun groupByMinutes(tickerData: List<TickerData>): Map<String, List<TickerData>> {
        return tickerData.groupBy { data ->
            val timestamp = data.timestamp
            val utcDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            utcDateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val utcDate = utcDateFormat.parse(timestamp)

            val kstTimeZone = TimeZone.getTimeZone("Asia/Seoul")
            val calendar = Calendar.getInstance(kstTimeZone)
            calendar.time = utcDate

            // 분 단위 필터에 따라 그룹화
            when (currentMinuteFilter) {
                MinuteFilter.MIN_1 -> {
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(calendar.time)
                }
                MinuteFilter.MIN_5 -> {
                    val minute = calendar.get(Calendar.MINUTE)
                    val adjustedMinute = (minute / 5) * 5
                    calendar.set(Calendar.MINUTE, adjustedMinute)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(calendar.time)
                }
                MinuteFilter.MIN_15 -> {
                    val minute = calendar.get(Calendar.MINUTE)
                    val adjustedMinute = (minute / 15) * 15
                    calendar.set(Calendar.MINUTE, adjustedMinute)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(calendar.time)
                }
            }
        }
    }

    private fun groupByHours(tickerData: List<TickerData>): Map<String, List<TickerData>> {
        return tickerData.groupBy { data ->
            val timestamp = data.timestamp
            val utcDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            utcDateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val utcDate = utcDateFormat.parse(timestamp)

            val kstTimeZone = TimeZone.getTimeZone("Asia/Seoul")
            val calendar = Calendar.getInstance(kstTimeZone)
            calendar.time = utcDate
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            SimpleDateFormat("yyyy-MM-dd HH:00", Locale.getDefault()).format(calendar.time)
        }
    }

    private fun groupByDays(tickerData: List<TickerData>): Map<String, List<TickerData>> {
        return tickerData.groupBy { data ->
            val timestamp = data.timestamp
            val utcDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            utcDateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val utcDate = utcDateFormat.parse(timestamp)

            val kstTimeZone = TimeZone.getTimeZone("Asia/Seoul")
            val calendar = Calendar.getInstance(kstTimeZone)
            calendar.time = utcDate
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        }
    }

    private fun groupByWeeks(tickerData: List<TickerData>): Map<String, List<TickerData>> {
        return tickerData.groupBy { data ->
            val timestamp = data.timestamp
            val utcDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            utcDateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val utcDate = utcDateFormat.parse(timestamp)

            val kstTimeZone = TimeZone.getTimeZone("Asia/Seoul")
            val calendar = Calendar.getInstance(kstTimeZone)
            calendar.time = utcDate
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        }
    }

    private fun groupByMonths(tickerData: List<TickerData>): Map<String, List<TickerData>> {
        return tickerData.groupBy { data ->
            val timestamp = data.timestamp
            val utcDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            utcDateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val utcDate = utcDateFormat.parse(timestamp)

            val kstTimeZone = TimeZone.getTimeZone("Asia/Seoul")
            val calendar = Calendar.getInstance(kstTimeZone)
            calendar.time = utcDate
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
        }
    }

    private fun groupByYears(tickerData: List<TickerData>): Map<String, List<TickerData>> {
        return tickerData.groupBy { data ->
            val timestamp = data.timestamp
            val utcDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            utcDateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val utcDate = utcDateFormat.parse(timestamp)

            val kstTimeZone = TimeZone.getTimeZone("Asia/Seoul")
            val calendar = Calendar.getInstance(kstTimeZone)
            calendar.time = utcDate
            calendar.set(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            SimpleDateFormat("yyyy", Locale.getDefault()).format(calendar.time)
        }
    }

    private fun updateChart() {
        binding.loadingIndicator.visibility = View.GONE
        binding.chartWebView.visibility = View.VISIBLE
        binding.emptyStateContainer.visibility = View.GONE

        // HTML 차트 페이지 로드
        loadChartHTML()
    }

    private fun loadChartHTML() {
        val htmlContent = createChartHTML()
        binding.chartWebView.loadDataWithBaseURL(
            null,
            htmlContent,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun createChartHTML(): String {
        // 시간 필터에 따른 시간 표시 설정
        val timeVisible = when (currentTimeFilter) {
            TimeFilter.SECONDS -> "true"
            TimeFilter.MINUTES -> "true"
            TimeFilter.HOURS -> "true"
            TimeFilter.DAYS -> "true"
            TimeFilter.WEEKS -> "true"
            TimeFilter.MONTHS -> "true"
            TimeFilter.YEARS -> "true"
        }

        val secondsVisible = when (currentTimeFilter) {
            TimeFilter.SECONDS -> "true"
            else -> "false"
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Trading Chart</title>
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    background-color: #FFFFFF;
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    overflow: hidden;
                    width: 100%;
                    height: 100%;
                    touch-action: none;
                    -ms-touch-action: none;
                    -webkit-touch-callout: none;
                    -webkit-user-select: none;
                    user-select: none;
                    overflow: hidden;
                }
                #chart {
                    width: 100%;
                    height: 100%;
                    min-height: 400px;
                    background-color: #FFFFFF;
                    position: relative;
                    display: block;
                    margin: 0;
                    padding: 0;
                    touch-action: none;
                    -webkit-touch-callout: none;
                    -webkit-user-select: none;
                    user-select: none;
                    pointer-events: auto;
                    overflow: hidden;
                    border: 1px solid #E1E5E9;
                    border-radius: 4px;
                }
            </style>
        </head>
        <body>
            <div id="chart"></div>
            
            <script>
                let chart, candlestickSeries, volumeSeries;
                let libraryLoaded = false;
                
                // 라이브러리 로딩 함수
                function loadLibrary() {
                    return new Promise((resolve, reject) => {
                        const script = document.createElement('script');
                        script.src = 'https://unpkg.com/lightweight-charts@4.1.3/dist/lightweight-charts.standalone.production.js';
                        script.onload = () => {
                            libraryLoaded = true;
                            resolve();
                        };
                        script.onerror = () => {
                            reject(new Error('라이브러리 로드 실패'));
                        };
                        document.head.appendChild(script);
                    });
                }
                
                function initChart() {
                    try {
                        console.log('차트 초기화 시작');
                        
                        if (!libraryLoaded) {
                            console.log('라이브러리가 로드되지 않음');
                            return;
                        }
                        
                        // LightweightCharts 객체 확인
                        if (typeof LightweightCharts === 'undefined') {
                            console.log('LightweightCharts 객체가 정의되지 않음');
                            return;
                        }
                        
                        const container = document.getElementById('chart');
                        if (!container) {
                            console.log('차트 컨테이너를 찾을 수 없음');
                            return;
                        }
                        
                        console.log('컨테이너 크기:', container.clientWidth, 'x', container.clientHeight);
                        
                        // 기존 차트가 있으면 제거
                        if (chart) {
                            try {
                                chart.remove();
                            } catch (e) {
                                console.log('기존 차트 제거 중 오류:', e);
                            }
                            chart = null;
                        }
                        
                        // 차트 생성 - 기본 설정만 사용
                        console.log('LightweightCharts.createChart 호출');
                        // 터치 기능을 포함한 차트 설정 (블링크 방지 설정 추가)
                        chart = LightweightCharts.createChart(container, {
                            width: container.clientWidth || 350,
                            height: container.clientHeight || 500,
                            layout: {
                                background: { color: '#FFFFFF' },
                                textColor: '#333333'
                            },
                            grid: {
                                vertLines: { color: '#E1E5E9' },
                                horzLines: { color: '#E1E5E9' }
                            },
                            rightPriceScale: {
                                borderColor: '#E1E5E9',
                                textColor: '#333333',
                                scaleMargins: {
                                    top: 0.05,  // 상단 여백
                                    bottom: 0.25  // 하단 여백 (거래량 차트 공간 확보)
                                }
                            },
                            // 블링크 방지를 위한 애니메이션 설정
                            animation: false,
                            timeScale: {
                                borderColor: '#E1E5E9',
                                timeVisible: true,
                                secondsVisible: false,
                                rightOffset: 0,
                                fixLeftEdge: true,
                                fixRightEdge: true,
                                lockVisibleTimeRangeOnResize: true,
                                rightBarStaysOnScroll: false,
                                // 한국 시간대 설정
                                timeUnit: 'second',
                                tickMarkFormatter: function(time) {
                                    // Unix timestamp는 UTC 기준이므로, UTC로 파싱 후 KST로 변환
                                    var utcDate = new Date(time * 1000);
                                    var kstDate = new Date(utcDate.getUTCFullYear(), utcDate.getUTCMonth(), utcDate.getUTCDate(), 
                                                         utcDate.getUTCHours(), utcDate.getUTCMinutes(), utcDate.getUTCSeconds());
                                    
                                    // KST로 변환 (UTC+9)
                                    kstDate.setHours(kstDate.getHours() + 9);
                                    
                                    // 시간 필터에 따른 포맷 설정
                                    var timeFilter = '$currentTimeFilter';
                                    var format = '';
                                    
                                    if (timeFilter === 'SECONDS') {
                                        format = 'MM-dd HH:mm:ss';
                                    } else if (timeFilter === 'MINUTES') {
                                        format = 'MM-dd HH:mm';
                                    } else if (timeFilter === 'HOURS') {
                                        format = 'MM-dd HH:00';
                                    } else if (timeFilter === 'DAYS') {
                                        format = 'MM-dd';
                                    } else if (timeFilter === 'WEEKS') {
                                        format = 'MM-dd';
                                    } else if (timeFilter === 'MONTHS') {
                                        format = 'yyyy-MM';
                                    } else if (timeFilter === 'YEARS') {
                                        format = 'yyyy';
                                    }
                                    
                                    return formatDate(kstDate, format);
                                }
                            },
                            crosshair: {
                                mode: LightweightCharts.CrosshairMode.Normal
                            },
                            // 모든 내장 터치 처리 비활성화 - 네이티브에서 처리
                            handleScroll: {
                                mouseWheel: false,
                                pressedMouseMove: false,
                                horzTouchDrag: false,
                                vertTouchDrag: false
                            },
                            handleScale: {
                                axisPressedMouseMove: false,
                                mouseWheel: false,
                                pinch: false,
                                axisDoubleClickReset: false
                            },
                            kineticScroll: {
                                touch: false,
                                mouse: false
                            }
                        });
                        
                        console.log('차트 생성 완료:', chart);



                        // 기본 캔들스틱 시리즈 생성 (블링크 방지 설정 추가)
                        candlestickSeries = chart.addCandlestickSeries({
                            upColor: '#26A69A',
                            downColor: '#EF5350',
                            borderDownColor: '#EF5350',
                            borderUpColor: '#26A69A',
                            wickDownColor: '#EF5350',
                            wickUpColor: '#26A69A',
                            // 블링크 방지를 위한 설정
                            lastValueVisible: false,
                            priceLineVisible: false
                        });
                        
                        console.log('캔들스틱 시리즈 생성 완료');

                        // 거래량 시리즈 생성 (하단에 별도 영역으로 분리)
                        volumeSeries = chart.addHistogramSeries({
                            color: 'rgba(76, 175, 80, 0.3)',
                            priceFormat: {
                                type: 'volume'
                            },
                            priceScaleId: 'volume',
                            scaleMargins: {
                                top: 0.8,  // 상단 80%는 캔들스틱 차트용
                                bottom: 0   // 하단 20%는 거래량 차트용
                            },
                            // 블링크 방지를 위한 설정
                            lastValueVisible: false
                        });
                        
                        // 거래량 스케일 설정
                        chart.priceScale('volume').applyOptions({
                            scaleMargins: {
                                top: 0.8,
                                bottom: 0
                            },
                            borderColor: '#E1E5E9',
                            textColor: '#333333',
                            visible: true,
                            autoScale: true
                        });
                        
                        // 시간축 설정 (캔들스틱과 거래량이 공유)
                        chart.timeScale().applyOptions({
                            scaleMargins: {
                                top: 0.05,
                                bottom: 0.05
                            }
                        });
                        
                        console.log('거래량 시리즈 생성 완료');

                        // 전역 변수로 노출
                        window.candlestickSeries = candlestickSeries;
                        window.volumeSeries = volumeSeries;
                        window.chart = chart;
                        
                        console.log('차트 초기화 완료');
                        
                        // 차트 초기화 완료 후 애니메이션 다시 활성화 (블링크 방지)
                        requestAnimationFrame(() => {
                            if (chart) {
                                chart.applyOptions({
                                    animation: true
                                });
                            }
                        });
                        
                        // 차트가 제대로 생성되었는지 확인
                        setTimeout(() => {
                            if (chart && container) {
                                console.log('차트 크기 확인:', chart.clientWidth, 'x', chart.clientHeight);
                                console.log('컨테이너 크기 재확인:', container.clientWidth, 'x', container.clientHeight);
                            }
                        }, 100);
                        
                    } catch (error) {
                        console.error('차트 초기화 오류:', error);
                    }
                }
                
                // 날짜 포맷 함수
                function formatDate(date, format) {
                    var year = date.getFullYear();
                    var month = String(date.getMonth() + 1).padStart(2, '0');
                    var day = String(date.getDate()).padStart(2, '0');
                    var hours = String(date.getHours()).padStart(2, '0');
                    var minutes = String(date.getMinutes()).padStart(2, '0');
                    var seconds = String(date.getSeconds()).padStart(2, '0');
                    
                    return format
                        .replace('yyyy', year)
                        .replace('MM', month)
                        .replace('dd', day)
                        .replace('HH', hours)
                        .replace('mm', minutes)
                        .replace('ss', seconds);
                }
                
                // 차트 데이터 설정 함수
                function setChartData(candlestickData) {
                    try {
                        if (!candlestickSeries || !volumeSeries) {
                            console.error('차트 시리즈가 초기화되지 않았습니다.');
                            return;
                        }
                        
                        console.log('차트 데이터 설정 시작:', candlestickData.length + '개');
                        
                        // 블링크 방지를 위해 애니메이션 일시 비활성화
                        chart.applyOptions({
                            animation: false
                        });
                        
                        // 캔들스틱 데이터 설정
                        candlestickSeries.setData(candlestickData);
                        
                        // 거래량 데이터 설정 (캔들스틱 색상과 일치, 더 명확한 구분)
                        var volumeData = candlestickData.map(function(item) {
                            var isUp = item.close >= item.open;
                            return {
                                time: item.time,
                                value: item.volume,
                                color: isUp ? 'rgba(38, 166, 154, 0.4)' : 'rgba(239, 83, 80, 0.4)'
                            };
                        });
                        volumeSeries.setData(volumeData);
                        
                        // 현재가 라인 색상 업데이트
                        updateCurrentPriceLineColor(candlestickData);
                        
                        // 차트 범위 설정 (즉시 실행하여 블링크 방지)
                        if (chart && container) {
                            // 논리적 범위로 초기 설정 (일부 데이터만 표시해서 스크롤 여유 공간 확보)
                            const visibleCount = Math.min(10, candlestickData.length); // 최대 10개 캔들만 표시
                            const startIndex = Math.max(0, candlestickData.length - visibleCount);
                            
                            chart.timeScale().setVisibleLogicalRange({
                                from: startIndex,
                                to: candlestickData.length - 1
                            });
                        }
                        
                        // 애니메이션 다시 활성화 (다음 프레임에서)
                        requestAnimationFrame(() => {
                            chart.applyOptions({
                                animation: true
                            });
                        });
                        
                        console.log('차트 데이터 설정 완료');
                        
                    } catch (error) {
                        console.error('차트 데이터 설정 오류:', error);
                    }
                }

                // 현재가 라인 색상 업데이트 함수
                function updateCurrentPriceLineColor(candlestickData) {
                    if (candlestickData && candlestickData.length > 0) {
                        const lastCandle = candlestickData[candlestickData.length - 1];
                        if (lastCandle && candlestickSeries) {
                            // 캔들 색상에 따라 현재가 라인 색상 결정
                            const isUp = lastCandle.close >= lastCandle.open;
                            const lineColor = isUp ? '#26A69A' : '#EF5350'; // 상승: 초록, 하락: 빨강
                            
                            candlestickSeries.applyOptions({
                                priceLineColor: lineColor,
                                priceLineWidth: 2,
                                priceLineStyle: 1 // 점선
                            });
                        }
                    }
                }
                
                // Android에서 호출할 수 있도록 전역 함수로 노출
                window.setChartData = setChartData;
                window.updateCurrentPriceLineColor = updateCurrentPriceLineColor;
                
                // 차트 초기화 (여러 시점에서 시도)
                function tryInitChart() {
                    console.log('차트 초기화 시도...');
                    if (typeof LightweightCharts !== 'undefined' && !chart) {
                        initChart();
                        return true;
                    }
                    return false;
                }
                
                // 라이브러리 로드 후 차트 초기화
                if (!libraryLoaded) {
                    loadLibrary().then(() => {
                        console.log('라이브러리 로드 완료');
                        setTimeout(() => {
                            tryInitChart();
                        }, 100);
                    }).catch((error) => {
                        console.error('라이브러리 로드 실패:', error);
                    });
                }
                
                // 페이지 로드 시에도 초기화 시도
                window.addEventListener('load', function() {
                    console.log('페이지 로드 완료');
                    setTimeout(() => {
                        tryInitChart();
                    }, 200);
                });
                
                // DOMContentLoaded 이벤트에서도 초기화 시도
                document.addEventListener('DOMContentLoaded', function() {
                    console.log('DOM 로드 완료');
                    setTimeout(() => {
                        tryInitChart();
                    }, 300);
                });
                
                // 백업용 타이머 (5초 후 강제 시도)
                setTimeout(() => {
                    if (!chart) {
                        console.log('백업 타이머로 차트 초기화 시도');
                        tryInitChart();
                    }
                }, 5000);
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    private fun updateChartData() {
        // Fragment가 destroy된 경우 binding이 null일 수 있으므로 체크
        if (_binding == null) {
            Log.w(TAG, "updateChartData: binding이 null입니다. Fragment가 destroy되었을 수 있습니다.")
            return
        }

        try {
            if (ohlcvData.isEmpty()) {
                Log.w(TAG, "OHLCV 데이터가 비어있습니다.")
                return
            }

            Log.d(TAG, "차트 데이터 업데이트 시작 - 필터: $currentTimeFilter, 데이터 개수: ${ohlcvData.size}")

            // OHLCV 데이터를 JavaScript 배열로 변환
            val candlestickData = ohlcvData.map { data ->
                // 날짜 문자열을 Unix timestamp로 변환
                val dateFormat = when (currentTimeFilter) {
                    TimeFilter.SECONDS -> SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    TimeFilter.MINUTES -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    TimeFilter.HOURS -> SimpleDateFormat("yyyy-MM-dd HH:00", Locale.getDefault())
                    TimeFilter.DAYS -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    TimeFilter.WEEKS -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    TimeFilter.MONTHS -> SimpleDateFormat("yyyy-MM", Locale.getDefault())
                    TimeFilter.YEARS -> SimpleDateFormat("yyyy", Locale.getDefault())
                }
                
                val date = dateFormat.parse(data.date)
                val timestamp = date?.time?.div(1000) ?: 0L

                // 캔들스틱 상승/하락 여부에 따라 거래량 색상 결정
                val isUp = data.close >= data.open
                val volumeColor = if (isUp) "rgba(38, 166, 154, 0.3)" else "rgba(239, 83, 80, 0.3)" // 녹색/빨간색 반투명

                mapOf(
                    "time" to timestamp,
                    "open" to data.open,
                    "high" to data.high,
                    "low" to data.low,
                    "close" to data.close,
                    "volume" to data.volume,
                    "volumeColor" to volumeColor
                )
            }
            


            val candlestickJson = org.json.JSONArray(candlestickData).toString()

            Log.d(TAG, "캔들스틱 데이터 JSON: $candlestickJson")

            // 차트가 준비되었는지 확인하는 JavaScript 코드
            val checkReadyCode = """
                (function() {
                    if (typeof chart !== 'undefined' && 
                        typeof candlestickSeries !== 'undefined' && 
                        typeof volumeSeries !== 'undefined' &&
                        typeof setChartData !== 'undefined') {
                        return 'ready';
                    } else {
                        return 'not_ready';
                    }
                })();
            """.trimIndent()

            binding.chartWebView.post {
                binding.chartWebView.evaluateJavascript(checkReadyCode) { result: String? ->
                    Log.d(TAG, "차트 준비 상태 확인: $result")
                    if (result == "\"ready\"") {
                        Log.d(TAG, "차트가 준비됨 - 데이터 설정 시작")
                        // 차트가 준비되었으면 데이터 설정
                        val jsCode = """
                            try {
                                console.log('차트 데이터 설정 시작');
                                if (window.setChartData) {
                                    console.log('setChartData 함수 호출');
                                    window.setChartData($candlestickJson);
                                    console.log('차트 데이터 설정 완료');
                                    
                                    // 현재가 라인 색상 업데이트
                                    if (window.updateCurrentPriceLineColor && $candlestickJson.length > 0) {
                                        window.updateCurrentPriceLineColor($candlestickJson);
                                    }
                                } else {
                                    console.error('setChartData 함수가 정의되지 않음');
                                }
                            } catch (error) {
                                console.error('차트 데이터 설정 오류:', error);
                            }
                        """.trimIndent()

                        binding.chartWebView.evaluateJavascript(jsCode) { jsResult: String? ->
                            Log.d(TAG, "JavaScript 실행 완료: $jsResult")
                        }
                    } else {
                        // 차트가 준비되지 않았으면 500ms 후 재시도
                        Log.d(TAG, "차트가 준비되지 않음, 500ms 후 재시도")
                        binding.chartWebView.postDelayed({
                            // Fragment가 destroy된 경우 binding이 null일 수 있으므로 체크
                            if (_binding != null) {
                                updateChartData()
                            } else {
                                Log.w(TAG, "postDelayed: binding이 null입니다. Fragment가 destroy되었을 수 있ㅇㅡㅁ.")
                            }
                        }, 500)
                    }
                }
            }

            Log.d(TAG, "차트 데이터 업데이트 완료 - 필터: $currentTimeFilter")

        } catch (e: Exception) {
            Log.e(TAG, "차트 데이터 업데이트 중 오류 발생: ${e.message}", e)
        }
    }

    // 폴링 시작
    private fun startPolling() {
        if (isPollingActive) return

        isPollingActive = true
        pollingJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isPollingActive) {
                try {
                    delay(5000) // 5초 대기
                    if (isPollingActive && ticker != null) {
                        Log.d(TAG, "폴링: 데이터 새로고침 시작")
                        loadTradeData(showLoading = false) // 폴링 시에는 로딩 표시하지 않음
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "폴링 중 오류 발생: ${e.message}")
                }
            }
        }
        Log.d(TAG, "폴링 시작됨 - 5초 간격")
    }

    // 폴링 중지
    private fun stopPolling() {
        isPollingActive = false
        pollingJob?.cancel()
        pollingJob = null
        Log.d(TAG, "폴링 중지됨")
    }

    // 폴링 재시작
    private fun restartPolling() {
        stopPolling()
        startPolling()
    }

    // 티커 업데이트 메서드
    fun updateTicker(newTicker: String?) {
        if (newTicker != ticker) {
            ticker = newTicker
            loadTradeData(showLoading = true)
            restartPolling() // 새로운 티커로 폴링 재시작
        }
    }

    private fun showEmptyState() {
        // Fragment가 destroy된 경우 binding이 null일 수 있으므로 체크
        if (_binding == null) {
            Log.w(TAG, "showEmptyState: binding이 null입니다. Fragment가 destroy되었을 수 있습니다.")
            return
        }

        try {
            binding.loadingIndicator.visibility = View.GONE
            binding.chartWebView.visibility = View.GONE
            binding.emptyStateContainer.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e(TAG, "showEmptyState 실행 중 오류: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 폴링 중지 및 리소스 정리
        stopPolling()
        
        // 터치 이벤트 리소스 정리
        gestureDetector = null
        scaleGestureDetector = null
        
        // WebView 정리
        try {
            _binding?.chartWebView?.setOnTouchListener(null)
            _binding?.chartWebView?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "WebView 정리 중 오류: ${e.message}", e)
        }
        _binding = null
    }
}