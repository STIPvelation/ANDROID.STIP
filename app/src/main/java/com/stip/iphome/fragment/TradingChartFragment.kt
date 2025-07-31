package com.stip.stip.iphome.fragment

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.WebChromeClient
import android.view.MotionEvent
import android.view.GestureDetector
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import android.widget.FrameLayout
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
import kotlinx.coroutines.CancellationException
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

// TouchDelegate 인터페이스 정의
interface TouchDelegate {
    fun beforeTouchEvent(view: ViewGroup)
    fun onTouchEvent(view: ViewGroup, event: MotionEvent): Boolean
}

class ChartWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : WebView(context, attrs, defStyle) {

    private val touchDelegates: MutableList<TouchDelegate> = mutableListOf()

    init {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(0x00000000)
        
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        touchDelegates.forEach { it.beforeTouchEvent(this) }
        return touchDelegates.any { it.onTouchEvent(this, event) } || super.onTouchEvent(event)
    }

    fun addTouchDelegate(touchDelegate: TouchDelegate) {
        touchDelegates.add(touchDelegate)
    }

    fun removeTouchDelegate(touchDelegate: TouchDelegate) {
        touchDelegates.remove(touchDelegate)
    }
}

class ScaleTouchDelegate(private val context: Context) : TouchDelegate {
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var gestureDetector: GestureDetector? = null
    
    private var isScaling = false
    private var isLongPress = false
    private var isTouchActive = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    
    override fun beforeTouchEvent(view: ViewGroup) {
        if (scaleGestureDetector == null) {
            scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    isScaling = true
                    Log.d("ScaleTouchDelegate", "핀치 줌 시작: focusX=${detector.focusX}, focusY=${detector.focusY}")
                    
                    // JavaScript로 스케일 시작 이벤트 전달
                    if (view is WebView) {
                        view.evaluateJavascript("""
                            if (window.handleNativeTouch) {
                                window.handleNativeTouch('scalestart', ${detector.focusX}, ${detector.focusY}, ${detector.scaleFactor}, ${detector.currentSpan});
                            }
                        """.trimIndent(), null)
                    }
                    
                    return true
                }
                
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val scaleFactor = detector.scaleFactor
                    val currentSpan = detector.currentSpan
                    val previousSpan = detector.previousSpan
                    
                    Log.d("ScaleTouchDelegate", "핀치 줌: scale=$scaleFactor, currentSpan=$currentSpan, previousSpan=$previousSpan")
                    
                    // 스케일 팩터가 1에 너무 가까우면 무시 (노이즈 제거)
                    if (Math.abs(scaleFactor - 1.0f) > 0.01f) {
                        // JavaScript로 스케일 이벤트 전달
                        if (view is WebView) {
                            view.evaluateJavascript("""
                                if (window.handleNativeTouch) {
                                    window.handleNativeTouch('scale', ${detector.focusX}, ${detector.focusY}, $scaleFactor, $currentSpan, $previousSpan);
                                }
                            """.trimIndent(), null)
                        }
                    }
                    
                    return true
                }
                
                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    isScaling = false
                    Log.d("ScaleTouchDelegate", "핀치 줌 종료")
                    
                    // JavaScript로 스케일 종료 이벤트 전달
                    if (view is WebView) {
                        view.evaluateJavascript("""
                            if (window.handleNativeTouch) {
                                window.handleNativeTouch('scaleend', ${detector.focusX}, ${detector.focusY});
                            }
                        """.trimIndent(), null)
                    }
                }
            })
        }
        
        if (gestureDetector == null) {
            gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean {
                    isTouchActive = true
                    isLongPress = false
                    lastTouchX = e.x
                    lastTouchY = e.y
                    Log.d("ScaleTouchDelegate", "터치 다운: x=${e.x}, y=${e.y}")
                    
                    // JavaScript로 터치 다운 이벤트 전달
                    if (view is WebView) {
                        view.evaluateJavascript("""
                            if (window.handleNativeTouch) {
                                window.handleNativeTouch('down', ${e.x}, ${e.y}, ${e.pressure});
                            }
                        """.trimIndent(), null)
                    }
                    
                    return true
                }
                
                override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                    if (isTouchActive && !isScaling && !isLongPress) {
                        val deltaX = e2.x - lastTouchX
                        val deltaY = e2.y - lastTouchY
                        
                        Log.d("ScaleTouchDelegate", "스크롤: dx=$distanceX, dy=$distanceY, deltaX=$deltaX, deltaY=$deltaY")
                        
                        // JavaScript로 스크롤 이벤트 전달
                        if (view is WebView) {
                            view.evaluateJavascript("""
                                if (window.handleNativeTouch) {
                                    window.handleNativeTouch('scroll', ${e2.x}, ${e2.y}, $deltaX, $deltaY, ${e2.pressure});
                                }
                            """.trimIndent()) { result ->
                                if (result != "null") {
                                    Log.d("ScaleTouchDelegate", "JavaScript 스크롤 결과: $result")
                                }
                            }
                        }
                        
                        lastTouchX = e2.x
                        lastTouchY = e2.y
                    }
                    return true
                }
                
                override fun onLongPress(e: MotionEvent) {
                    isLongPress = true
                    Log.d("ScaleTouchDelegate", "롱프레스 감지")
                    
                    // JavaScript로 롱프레스 이벤트 전달
                    if (view is WebView) {
                        view.evaluateJavascript("""
                            if (window.handleNativeTouch) {
                                window.handleNativeTouch('longpress', ${e.x}, ${e.y});
                            }
                        """.trimIndent(), null)
                    }
                }
                
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    Log.d("ScaleTouchDelegate", "더블탭 감지")
                    
                    // JavaScript로 더블탭 이벤트 전달
                    if (view is WebView) {
                        view.evaluateJavascript("""
                            if (window.handleNativeTouch) {
                                window.handleNativeTouch('doubletap', ${e.x}, ${e.y});
                            }
                        """.trimIndent(), null)
                    }
                    
                    return true
                }
                
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    Log.d("ScaleTouchDelegate", "싱글탭 확인")
                    
                    // JavaScript로 싱글탭 이벤트 전달
                    if (view is WebView) {
                        view.evaluateJavascript("""
                            if (window.handleNativeTouch) {
                                window.handleNativeTouch('singletap', ${e.x}, ${e.y});
                            }
                        """.trimIndent(), null)
                    }
                    
                    return true
                }
            })
        }
    }
    
    override fun onTouchEvent(view: ViewGroup, event: MotionEvent): Boolean {
        var handled = false
        
        // 중첩 스크롤 처리
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isTouchActive = true
                // 터치 시작 시 부모 뷰의 터치 인터셉트 허용
                view.parent?.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_MOVE -> {
                if (isScaling || isLongPress) {
                    // 핀치 줌이나 롱프레스 중에는 부모 뷰의 터치 인터셉트 방지
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouchActive = false
                isLongPress = false
                // 터치 종료 시 부모 뷰의 터치 인터셉트 허용
                view.parent?.requestDisallowInterceptTouchEvent(false)
                
                // JavaScript로 터치 업 이벤트 전달
                if (view is WebView) {
                    view.evaluateJavascript("""
                        if (window.handleNativeTouch) {
                            window.handleNativeTouch('up', ${event.x}, ${event.y});
                        }
                    """.trimIndent(), null)
                }
            }
        }
        
        // 스케일 제스처 처리
        scaleGestureDetector?.onTouchEvent(event)?.let { handled = it || handled }
        
        // 일반 제스처 처리 (스케일 중이 아닐 때만)
        if (!isScaling) {
            gestureDetector?.onTouchEvent(event)?.let { handled = it || handled }
        }
        
        return handled
    }
}

@AndroidEntryPoint
class TradingChartFragment : Fragment() {

    private var _binding: FragmentTradingChartBinding? = null
    private val binding get() = _binding!!

    private var ticker: String? = null
    private var ohlcvData: List<OHLCVData> = emptyList()
    private var lastDataCount = 0
    private var lastTickerData: List<TickerData> = emptyList()
    private var pollingJob: kotlinx.coroutines.Job? = null
    private var isPollingActive = false

    // 시간 필터 관련 변수
    private var currentTimeFilter: TimeFilter = TimeFilter.HOURS
    private var currentMinuteFilter: MinuteFilter = MinuteFilter.MIN_1
    private var isMinuteSubFilterVisible = false

    private var scaleTouchDelegate: ScaleTouchDelegate? = null

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

    private fun setupTouchHandling() {
        // ScaleTouchDelegate 생성 및 WebView에 추가
        scaleTouchDelegate = ScaleTouchDelegate(requireContext())
        
        // 기존 WebView에 터치 리스너로 TouchDelegate 패턴 적용
        binding.chartWebView.setOnTouchListener { _, event ->
            scaleTouchDelegate?.let { delegate ->
                delegate.beforeTouchEvent(binding.chartWebView)
                delegate.onTouchEvent(binding.chartWebView, event)
            } ?: false
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
            // 다른 필터 선택 시에만 데이터 로드 (타임프레임 변경용)
            updateTimeFilterUI()
            loadTradeDataForTimeFilter()
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

        // 타임프레임 변경용 데이터 로드
        loadTradeDataForTimeFilter()
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

    // 타임프레임 필터 변경 시 전용 데이터 로드 함수 (상태 유지)
    private fun loadTradeDataForTimeFilter() {
        // Fragment가 destroy된 경우 binding이 null일 수 있으므로 체크
        if (_binding == null) {
            Log.w(TAG, "loadTradeDataForTimeFilter: binding이 null입니다.")
            return
        }

        val currentTicker = ticker
        if (currentTicker.isNullOrBlank()) {
            Log.e(TAG, "티커 코드가 없습니다. ticker: $currentTicker")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "타임프레임 변경 - 데이터 재변환 시작: $currentTimeFilter")

                // 기존 데이터가 있으면 재사용, 없으면 새로 로드
                val tickerData = if (lastTickerData.isNotEmpty()) {
                    Log.d(TAG, "기존 데이터 재사용: ${lastTickerData.size}개")
                    lastTickerData
                } else {
                    Log.d(TAG, "새로운 데이터 로드")
                    val pairId = getPairIdForTicker(currentTicker)
                    if (pairId == null) {
                        Log.e(TAG, "티커에 해당하는 pairId를 찾을 수 없습니다: $currentTicker")
                        return@launch
                    }
                    fetchHourlyTickerData(pairId)
                }

                if (tickerData.isNotEmpty()) {
                    // 타임프레임 변경 시 상태 유지하면서 데이터만 재변환
                    convertTickerDataToOHLCV(tickerData, preserveState = true)
                    lastTickerData = tickerData
                    lastDataCount = tickerData.size
                    Log.d(TAG, "타임프레임 변경 완료 - 상태 유지됨")
                } else {
                    Log.w(TAG, "타임프레임 변경: 데이터가 비어있습니다.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "타임프레임 변경 중 오류: ${e.message}")
            }
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
                    
                    if (showLoading) {
                        // 초기 로드: 전체 차트 생성
                        Log.d(TAG, "초기 로드 - 전체 차트 생성")
                        convertTickerDataToOHLCV(tickerData)
                        lastDataCount = tickerData.size
                        lastTickerData = tickerData
                    } else {
                        // 폴링 업데이트: 스마트 업데이트 (깜빡임 없음)
                        updateChartSmartly(tickerData)
                    }
                    
                    // 초기 로드 완료 후 폴링 시작
                    if (!isPollingActive) {
                        Log.d(TAG, "초기 데이터 로드 완료 - 폴링 시작")
                        startPolling()
                    }
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

    private fun convertTickerDataToOHLCV(tickerData: List<TickerData>, preserveState: Boolean = false) {
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
        val sortedOhlcvList = ohlcvList.sortedBy { it.date }
        
        // 거래가 없는 시간대의 빈 캔들 생성하여 차트 연속성 확보
        ohlcvData = fillMissingCandles(sortedOhlcvList)

        Log.d(TAG, "OHLCV 데이터 변환 완료: ${sortedOhlcvList.size}개 -> 빈 캔들 포함 ${ohlcvData.size}개 (필터: $currentTimeFilter, 상태 유지: $preserveState)")

        activity?.runOnUiThread {
            if (preserveState) {
                Log.d(TAG, "상태 유지 업데이트 - 차트 데이터만 갱신")
                updateChartData()
            } else {
                Log.d(TAG, "초기 로드 - 전체 차트 생성")
            updateChart()
            }
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

    // 거래가 없는 시간대의 빈 캔들을 생성하는 함수
    private fun fillMissingCandles(ohlcvList: List<OHLCVData>): List<OHLCVData> {
        if (ohlcvList.isEmpty()) return ohlcvList
        
        Log.d(TAG, "빈 캔들 생성 시작 - 원본 데이터: ${ohlcvList.size}개")
        
        val sortedList = ohlcvList.sortedBy { it.date }
        val filledList = mutableListOf<OHLCVData>()
        
        // 시간 간격 계산 함수
        fun getNextTimeKey(timeKey: String): String {
            val calendar = Calendar.getInstance()
            
            // 시간 필터에 따라 적절한 날짜 포맷 사용
            val dateFormat = when (currentTimeFilter) {
                TimeFilter.SECONDS -> SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                TimeFilter.MINUTES -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                TimeFilter.HOURS -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                TimeFilter.DAYS -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                TimeFilter.WEEKS -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                TimeFilter.MONTHS -> SimpleDateFormat("yyyy-MM", Locale.getDefault())
                TimeFilter.YEARS -> SimpleDateFormat("yyyy", Locale.getDefault())
            }
            
            val date = dateFormat.parse(timeKey) ?: return timeKey
            calendar.time = date
            
            when (currentTimeFilter) {
                TimeFilter.SECONDS -> calendar.add(Calendar.SECOND, 1)
                TimeFilter.MINUTES -> {
                    when (currentMinuteFilter) {
                        MinuteFilter.MIN_1 -> calendar.add(Calendar.MINUTE, 1)
                        MinuteFilter.MIN_5 -> calendar.add(Calendar.MINUTE, 5)
                        MinuteFilter.MIN_15 -> calendar.add(Calendar.MINUTE, 15)
                    }
                }
                TimeFilter.HOURS -> calendar.add(Calendar.HOUR_OF_DAY, 1)
                TimeFilter.DAYS -> calendar.add(Calendar.DAY_OF_MONTH, 1)
                TimeFilter.WEEKS -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                TimeFilter.MONTHS -> calendar.add(Calendar.MONTH, 1)
                TimeFilter.YEARS -> calendar.add(Calendar.YEAR, 1)
            }
            
            return dateFormat.format(calendar.time)
        }
        
        var lastClose = sortedList.first().close
        filledList.add(sortedList.first())
        
        for (i in 1 until sortedList.size) {
            val prevTimeKey = sortedList[i - 1].date
            val currentTimeKey = sortedList[i].date
            var nextTimeKey = getNextTimeKey(prevTimeKey)
            
            // 이전 캔들과 현재 캔들 사이의 빈 시간대를 채움
            var fillCount = 0
            val maxFillCount = 1000 // 무한 루프 방지를 위한 최대 생성 개수 제한
            
            while (nextTimeKey < currentTimeKey && fillCount < maxFillCount) {
                filledList.add(
                    OHLCVData(
                        date = nextTimeKey,
                        open = lastClose,
                        high = lastClose,
                        low = lastClose,
                        close = lastClose,
                        volume = 0f
                    )
                )
                nextTimeKey = getNextTimeKey(nextTimeKey)
                fillCount++
            }
            
            if (fillCount >= maxFillCount) {
                Log.w(TAG, "빈 캔들 생성 개수 제한 도달: ${prevTimeKey} -> ${currentTimeKey}")
            }
            
            filledList.add(sortedList[i])
            lastClose = sortedList[i].close
        }
        
        // 마지막 데이터부터 현재 시간까지의 빈 캔들 생성
        if (sortedList.isNotEmpty()) {
            val lastTimeKey = sortedList.last().date
            val currentTime = Calendar.getInstance()
            
            // 현재 시간을 필터에 맞는 형식으로 변환
            val dateFormat = when (currentTimeFilter) {
                TimeFilter.SECONDS -> SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                TimeFilter.MINUTES -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                TimeFilter.HOURS -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                TimeFilter.DAYS -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                TimeFilter.WEEKS -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                TimeFilter.MONTHS -> SimpleDateFormat("yyyy-MM", Locale.getDefault())
                TimeFilter.YEARS -> SimpleDateFormat("yyyy", Locale.getDefault())
            }
            
            val currentTimeKey = dateFormat.format(currentTime.time)
            var nextTimeKey = getNextTimeKey(lastTimeKey)
            var fillCount = 0
            val maxRecentFillCount = when (currentTimeFilter) {
                TimeFilter.SECONDS -> 300 // 최대 5분 (300초)
                TimeFilter.MINUTES -> when (currentMinuteFilter) {
                    MinuteFilter.MIN_1 -> 60   // 최대 1시간 (60분)
                    MinuteFilter.MIN_5 -> 12   // 최대 1시간 (12 * 5분)
                    MinuteFilter.MIN_15 -> 4   // 최대 1시간 (4 * 15분)
                }
                TimeFilter.HOURS -> 24      // 최대 1일 (24시간)
                TimeFilter.DAYS -> 30       // 최대 1달 (30일)
                TimeFilter.WEEKS -> 4       // 최대 1달 (4주)
                TimeFilter.MONTHS -> 12     // 최대 1년 (12달)
                TimeFilter.YEARS -> 5       // 최대 5년
            }
            
            Log.d(TAG, "현재 시간까지 빈 캔들 생성: ${lastTimeKey} -> ${currentTimeKey}")
            
            // 마지막 데이터부터 현재 시간까지 빈 캔들 생성 (제한 적용)
            while (nextTimeKey <= currentTimeKey && fillCount < maxRecentFillCount) {
                filledList.add(
                    OHLCVData(
                        date = nextTimeKey,
                        open = lastClose,
                        high = lastClose,
                        low = lastClose,
                        close = lastClose,
                        volume = 0f
                    )
                )
                nextTimeKey = getNextTimeKey(nextTimeKey)
                fillCount++
            }
            
            if (fillCount >= maxRecentFillCount) {
                Log.w(TAG, "현재 시간까지 빈 캔들 생성 개수 제한 도달: ${fillCount}개")
            } else {
                Log.d(TAG, "현재 시간까지 빈 캔들 생성 완료: ${fillCount}개")
            }
        }
        
        Log.d(TAG, "빈 캔들 생성 완료 - 결과 데이터: ${filledList.size}개 (추가된 빈 캔들: ${filledList.size - sortedList.size}개)")
        return filledList
    }

    // 시간 기반 업데이트가 필요한지 확인
    private fun needsTimeBasedUpdate(): Boolean {
        if (ohlcvData.isEmpty()) return false
        
        val lastCandle = ohlcvData.last()
        val lastTime = try {
            val dateFormat = when (currentTimeFilter) {
                TimeFilter.SECONDS -> SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                TimeFilter.MINUTES -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                TimeFilter.HOURS -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                else -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            }
            dateFormat.parse(lastCandle.date)
        } catch (e: Exception) {
            Log.w(TAG, "날짜 파싱 실패: ${lastCandle.date}")
            return false
        }
        
        if (lastTime == null) return false
        
        val currentTime = Calendar.getInstance()
        val timeDiff = currentTime.timeInMillis - lastTime.time
        
        // 시간 필터에 따라 새 캔들이 필요한 간격 확인
        val requiredInterval = when (currentTimeFilter) {
            TimeFilter.SECONDS -> 1000L // 1초
            TimeFilter.MINUTES -> when (currentMinuteFilter) {
                MinuteFilter.MIN_1 -> 60000L  // 1분
                MinuteFilter.MIN_5 -> 300000L // 5분
                MinuteFilter.MIN_15 -> 900000L // 15분
            }
            TimeFilter.HOURS -> 3600000L // 1시간
            else -> 60000L // 기본 1분
        }
        
        return timeDiff >= requiredInterval
    }

    // 실시간 빈 캔들 업데이트 (차트 깜빡임 없이)
    private fun updateRealtimeEmptyCandles() {
        if (ohlcvData.isEmpty()) {
            Log.d(TAG, "빈 캔들 업데이트 건너뜀 - ohlcvData가 비어있음")
            return
        }
        
        val lastCandle = ohlcvData.last()
        val currentTime = Calendar.getInstance()
        
        // 현재 시간을 필터에 맞는 형식으로 변환
        val dateFormat = when (currentTimeFilter) {
            TimeFilter.SECONDS -> SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            TimeFilter.MINUTES -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            TimeFilter.HOURS -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            else -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        }
        
        val currentTimeKey = dateFormat.format(currentTime.time)
        
        Log.d(TAG, "빈 캔들 확인 - 현재시간: $currentTimeKey, 마지막캔들: ${lastCandle.date}")
        
        // 새로운 빈 캔들이 필요한지 확인
        if (currentTimeKey > lastCandle.date) {
            Log.d(TAG, "실시간 빈 캔들 추가: ${lastCandle.date} -> $currentTimeKey")
            
            // 기존 데이터에 빈 캔들 추가 (전체 재생성 없이)
            val newCandleData = OHLCVData(
                date = currentTimeKey,
                open = lastCandle.close,
                high = lastCandle.close,
                low = lastCandle.close,
                close = lastCandle.close,
                volume = 0f
            )
            
            // 기존 데이터에 새 캔들 추가
            ohlcvData = ohlcvData + newCandleData
            
            // 차트에 새 캔들만 추가 (깜빡임 없이)
            activity?.runOnUiThread {
                updateChartWithNewCandle(newCandleData)
            }
        } else {
            Log.d(TAG, "빈 캔들 추가 불필요 - 시간 조건 미충족")
        }
    }

    // TickerData 리스트 비교 함수
    private fun isTickerDataEqual(list1: List<TickerData>, list2: List<TickerData>): Boolean {
        if (list1.size != list2.size) {
            Log.d(TAG, "데이터 크기 다름: ${list1.size} vs ${list2.size}")
            return false
        }
        
        for (i in list1.indices) {
            val data1 = list1[i]
            val data2 = list2[i]
            
            if (data1.price != data2.price || 
                data1.amount != data2.amount || 
                data1.timestamp != data2.timestamp) {
                Log.d(TAG, "데이터 내용 다름 (인덱스 $i): ${data1.price} vs ${data2.price}")
                return false
            }
        }
        
        return true
    }

    // 스마트 차트 업데이트 (새 거래 시에도 깜빡임 없음)
    private fun updateChartSmartly(newTickerData: List<TickerData>) {
        Log.d(TAG, "스마트 업데이트 시작 - 새 데이터: ${newTickerData.size}개, 이전: ${lastTickerData.size}개")
        
        // 데이터 비교 및 로깅
        val hasDataChanged = !isTickerDataEqual(newTickerData, lastTickerData)
        val hasNewTrades = newTickerData.size > lastTickerData.size
        val hasPriceUpdates = newTickerData.size == lastTickerData.size && hasDataChanged
        
        Log.d(TAG, "데이터 분석 - 변화: $hasDataChanged, 새거래: $hasNewTrades, 가격변화: $hasPriceUpdates")
        
        if (!hasDataChanged) {
            Log.d(TAG, "거래 데이터 변화 없음 - 차트 상태 확인")
            
            // 차트가 제대로 표시되지 않았다면 업데이트 필요
            if (ohlcvData.isEmpty()) {
                Log.d(TAG, "차트 데이터가 없음 - 전체 차트 생성")
                convertTickerDataToOHLCV(newTickerData, preserveState = true)
                lastTickerData = newTickerData
                lastDataCount = newTickerData.size
            } else {
                Log.d(TAG, "차트 데이터 존재 - 강제 차트 새로고침 실행")
                // 폴링 시에는 데이터가 같아도 차트를 강제로 새로고침
                // 가격 변동이나 실시간 업데이트를 확실하게 반영
                convertTickerDataToOHLCV(newTickerData, preserveState = true)
            updateRealtimeEmptyCandles()
            }
            return
        }
        
        // 새로운 거래 데이터가 있을 때
        if (newTickerData.size > lastTickerData.size) {
            Log.d(TAG, "새로운 거래 데이터 감지: ${newTickerData.size - lastTickerData.size}개 추가")
            
            // 새로 추가된 거래만 처리
            val newTrades = newTickerData.drop(lastTickerData.size)
            processNewTrades(newTrades)
            
            lastTickerData = newTickerData
            lastDataCount = newTickerData.size
        } else if (newTickerData.size == lastTickerData.size) {
            // 같은 개수지만 내용이 다를 경우 (가격 업데이트 등)
            Log.d(TAG, "기존 거래 데이터 업데이트 감지")
            
            // 마지막 캔들 업데이트 (현재 진행중인 캔들)
            updateCurrentCandle(newTickerData)
            
            lastTickerData = newTickerData
        } else {
            // 데이터가 줄어든 경우 (특이 상황) - 전체 재생성
            Log.w(TAG, "데이터 개수 감소 감지 - 전체 차트 재생성")
            convertTickerDataToOHLCV(newTickerData, preserveState = true)
            lastTickerData = newTickerData
            lastDataCount = newTickerData.size
        }
        
        // 시간 기반 빈 캔들도 확인
        updateRealtimeEmptyCandles()
    }

    // 새로운 거래만 처리해서 캔들 업데이트 (깜빡임 없음)
    private fun processNewTrades(newTrades: List<TickerData>) {
        if (newTrades.isEmpty()) return
        
        Log.d(TAG, "새 거래 처리 시작: ${newTrades.size}개")
        
        // 새로운 거래를 시간별로 그룹화
        val groupedData = when (currentTimeFilter) {
            TimeFilter.SECONDS -> groupBySeconds(newTrades)
            TimeFilter.MINUTES -> groupByMinutes(newTrades)
            TimeFilter.HOURS -> groupByHours(newTrades)
            TimeFilter.DAYS -> groupByDays(newTrades)
            TimeFilter.WEEKS -> groupByWeeks(newTrades)
            TimeFilter.MONTHS -> groupByMonths(newTrades)
            TimeFilter.YEARS -> groupByYears(newTrades)
        }
        
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
                
                // 기존 캔들이 있는지 확인
                val existingCandleIndex = ohlcvData.indexOfFirst { it.date == timeKey }
                
                if (existingCandleIndex >= 0) {
                    // 기존 캔들 업데이트
                    val existingCandle = ohlcvData[existingCandleIndex]
                    val updatedCandle = OHLCVData(
                        date = timeKey,
                        open = existingCandle.open,  // 시가는 유지
                        high = maxOf(existingCandle.high, high),
                        low = minOf(existingCandle.low, low),
                        close = close,  // 종가는 최신으로
                        volume = existingCandle.volume + volume
                    )
                    
                    // 데이터 업데이트
                    ohlcvData = ohlcvData.toMutableList().apply {
                        set(existingCandleIndex, updatedCandle)
                    }
                    
                    // 차트에 캔들 업데이트
                    activity?.runOnUiThread {
                        updateChartWithNewCandle(updatedCandle)
                    }
                    
                    Log.d(TAG, "기존 캔들 업데이트: $timeKey - O:${updatedCandle.open}, H:${updatedCandle.high}, L:${updatedCandle.low}, C:${updatedCandle.close}, V:${updatedCandle.volume}")
                } else {
                    // 새로운 캔들 추가
                    val newCandle = OHLCVData(
                        date = timeKey,
                        open = open,
                        high = high,
                        low = low,
                        close = close,
                        volume = volume
                    )
                    
                    // 데이터에 추가 (정렬 유지)
                    ohlcvData = (ohlcvData + newCandle).sortedBy { it.date }
                    
                    // 차트에 캔들 추가
                    activity?.runOnUiThread {
                        updateChartWithNewCandle(newCandle)
                    }
                    
                    Log.d(TAG, "새 캔들 추가: $timeKey - O:$open, H:$high, L:$low, C:$close, V:$volume")
                }
            }
        }
    }

    // 현재 캔들 업데이트 (같은 개수의 데이터지만 내용 변화)
    private fun updateCurrentCandle(newTickerData: List<TickerData>) {
        if (newTickerData.isEmpty() || ohlcvData.isEmpty()) return
        
        // 마지막 거래의 시간대 확인
        val lastTrade = newTickerData.last()
        val lastTradeTime = try {
            val utcDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            utcDateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val utcDate = utcDateFormat.parse(lastTrade.timestamp)
            
            val kstTimeZone = TimeZone.getTimeZone("Asia/Seoul")
            val calendar = Calendar.getInstance(kstTimeZone)
            calendar.time = utcDate
            
            when (currentTimeFilter) {
                TimeFilter.SECONDS -> {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(calendar.time)
                }
                TimeFilter.MINUTES -> {
                    when (currentMinuteFilter) {
                        MinuteFilter.MIN_1 -> {
                            calendar.set(Calendar.SECOND, 0)
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(calendar.time)
                        }
                        MinuteFilter.MIN_5 -> {
                            val minute = calendar.get(Calendar.MINUTE)
                            val adjustedMinute = (minute / 5) * 5
                            calendar.set(Calendar.MINUTE, adjustedMinute)
                            calendar.set(Calendar.SECOND, 0)
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(calendar.time)
                        }
                        MinuteFilter.MIN_15 -> {
                            val minute = calendar.get(Calendar.MINUTE)
                            val adjustedMinute = (minute / 15) * 15
                            calendar.set(Calendar.MINUTE, adjustedMinute)
                            calendar.set(Calendar.SECOND, 0)
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(calendar.time)
                        }
                    }
                }
                else -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(calendar.time)
            }
        } catch (e: Exception) {
            Log.w(TAG, "시간 파싱 실패: ${lastTrade.timestamp}")
            return
        }
        
        // 마지막 캔들이 해당 시간대인지 확인
        val lastCandle = ohlcvData.last()
        if (lastCandle.date == lastTradeTime) {
            // 현재 캔들의 종가만 업데이트
            val updatedCandle = lastCandle.copy(close = lastTrade.price.toFloat())
            
            // 데이터 업데이트
            ohlcvData = ohlcvData.toMutableList().apply {
                set(size - 1, updatedCandle)
            }
            
            // 차트에 업데이트
            activity?.runOnUiThread {
                updateChartWithNewCandle(updatedCandle)
            }
            
            Log.d(TAG, "현재 캔들 종가 업데이트: $lastTradeTime - ${lastCandle.close} -> ${updatedCandle.close}")
        }
    }

    // 새 캔들만 추가하는 함수 (차트 깜빡임 방지)
    private fun updateChartWithNewCandle(newCandle: OHLCVData) {
        if (_binding == null) return
        
        val candlestickData = mapOf(
            "time" to when (currentTimeFilter) {
                TimeFilter.SECONDS -> {
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(newCandle.date)?.time ?: 0
                    (timestamp / 1000).toString()
                }
                TimeFilter.MINUTES, TimeFilter.HOURS -> {
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(newCandle.date)?.time ?: 0
                    (timestamp / 1000).toString()
                }
                TimeFilter.DAYS -> {
                    val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(newCandle.date)?.time ?: 0
                    (timestamp / 1000).toString()
                }
                else -> {
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(newCandle.date)?.time ?: 0
                    (timestamp / 1000).toString()
                }
            },
            "open" to newCandle.open.toDouble(),
            "high" to newCandle.high.toDouble(),
            "low" to newCandle.low.toDouble(),
            "close" to newCandle.close.toDouble()
        )
        
        val candleJson = org.json.JSONObject(candlestickData).toString()
        
        // JavaScript로 새 캔들만 추가 (사용자 확대/이동 상태 보존)
        val script = """
            javascript:(function() {
                try {
                    if (typeof candlestickSeries !== 'undefined' && candlestickSeries && typeof chart !== 'undefined' && chart) {
                        var newCandle = $candleJson;
                        console.log('새 캔들 추가 (상태 보존):', newCandle);
                        
                        // 현재 차트 상태 저장 (확대/축소, 스크롤 위치)
                        var timeScale = chart.timeScale();
                        var currentVisibleRange = null;
                        try {
                            currentVisibleRange = timeScale.getVisibleRange();
                        } catch (e) {
                            console.log('현재 보이는 범위 가져오기 실패, 무시하고 계속');
                        }
                        
                        // 캔들 데이터 업데이트
                        candlestickSeries.update(newCandle);
                        
                        // 볼륨 데이터도 추가
                        if (typeof volumeSeries !== 'undefined' && volumeSeries) {
                            volumeSeries.update({
                                time: newCandle.time,
                                value: ${newCandle.volume}
                            });
                        }
                        
                        // 현재가 라인 색상 업데이트 (새 캔들 기준)
                        if (typeof updateCurrentPriceLineColor !== 'undefined') {
                            updateCurrentPriceLineColor([newCandle]);
                            
                            // 새 캔들 추가 후 전체 데이터 업데이트
                            if (window.chartData) {
                                // 기존 데이터에 새 캔들 추가
                                const existingData = window.chartData;
                                const updatedData = [...existingData, newCandle];
                                window.chartData = updatedData;
                            }
                        }
                        
                        // 사용자가 특정 위치를 보고 있었다면 그 상태 유지
                        if (currentVisibleRange && 
                            currentVisibleRange.from && 
                            currentVisibleRange.to &&
                            currentVisibleRange.from !== currentVisibleRange.to) {
                            
                            // 현재 시간 (최신 캔들 시간)
                            var currentTime = parseInt(newCandle.time);
                            var rangeEnd = parseInt(currentVisibleRange.to);
                            
                            // 사용자가 최신 시간 근처가 아닌 다른 곳을 보고 있다면 상태 보존
                            var timeDiff = Math.abs(currentTime - rangeEnd);
                            var fiveMinutes = 5 * 60; // 5분 = 300초
                            
                            if (timeDiff > fiveMinutes) {
                                console.log('사용자 확대/스크롤 상태 복원:', currentVisibleRange);
                                try {
                                    timeScale.setVisibleRange(currentVisibleRange);
                                } catch (e) {
                                    console.log('시간 범위 복원 실패, 무시하고 계속:', e);
                                }
                            } else {
                                console.log('최신 시간 근처 보고 있음 - 자연스럽게 업데이트');
                            }
                        } else {
                            console.log('시간 범위 정보 없음 - 기본 동작');
                        }
                    }
                } catch (e) {
                    console.error('새 캔들 추가 중 오류:', e);
                }
            })();
        """.trimIndent()
        
        // Fragment가 destroy된 경우 binding이 null일 수 있으므로 체크
        if (_binding != null) {
        binding.chartWebView.evaluateJavascript(script) { result ->
                // 콜백 실행 시점에도 binding이 null일 수 있으므로 체크
                if (_binding != null) {
            Log.d(TAG, "새 캔들 추가 완료: $result")
                } else {
                    Log.w(TAG, "새 캔들 추가 콜백: binding이 null입니다.")
                }
            }
        } else {
            Log.w(TAG, "새 캔들 추가 건너뜀: binding이 null입니다.")
        }
    }

    private fun updateChart() {
        binding.loadingIndicator.visibility = View.GONE
        binding.chartWebView.visibility = View.VISIBLE
        binding.emptyStateContainer.visibility = View.GONE

        // 기존 차트가 있는지 확인
        val checkExistingChartCode = """
            (function() {
                if (typeof chart !== 'undefined' && chart !== null && 
                    typeof candlestickSeries !== 'undefined' && 
                    typeof volumeSeries !== 'undefined') {
                    return 'exists';
                } else {
                    return 'not_exists';
                }
            })();
        """.trimIndent()

        // Fragment가 destroy된 경우 binding이 null일 수 있으므로 체크
        if (_binding != null) {
            binding.chartWebView.evaluateJavascript(checkExistingChartCode) { result: String? ->
                // 콜백 실행 시점에도 binding이 null일 수 있으므로 체크
                if (_binding != null) {
                    Log.d(TAG, "기존 차트 확인 결과: $result")
                    if (result == "\"exists\"") {
                        // 기존 차트가 있으면 데이터만 업데이트 (상태 유지)
                        Log.d(TAG, "기존 차트 발견 - 데이터만 업데이트하여 상태 유지")
                        updateChartData()
                    } else {
                        // 기존 차트가 없으면 HTML 새로 로드
                        Log.d(TAG, "기존 차트 없음 - HTML 새로 로드")
        loadChartHTML()
                    }
                } else {
                    Log.w(TAG, "기존 차트 확인 콜백: binding이 null입니다.")
                }
            }
        } else {
            Log.w(TAG, "기존 차트 확인 건너뜀: binding이 null입니다.")
            // binding이 null이면 HTML 새로 로드 시도
            loadChartHTML()
        }
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
                        
                        // 네이티브 터치 이벤트 처리 함수 정의
                        window.handleNativeTouch = function(type, x, y, ...args) {
                            try {
                                console.log('네이티브 터치 이벤트:', type, 'x:', x, 'y:', y, 'args:', args);
                                
                                switch(type) {
                                    case 'start':
                                        // 터치 시작 - 크로스헤어 표시
                                        chart.setCrosshairPosition(x, y, chart.timeScale().coordinateToTime(x));
                                        break;
                                        
                                    case 'scroll':
                                        // 스크롤 드래그 - 차트 이동 (방향 반전)
                                        var deltaX = args[0] || 0;
                                        var deltaY = args[1] || 0;
                                        var pressure = args[2] || 1.0;
                                        
                                        // 수평 스크롤만 처리 (시간축 이동)
                                        if (Math.abs(deltaX) > Math.abs(deltaY)) {
                                            var timeScale = chart.timeScale();
                                            var visibleRange = timeScale.getVisibleRange();
                                            if (visibleRange) {
                                                var timeDelta = -deltaX * 0.09
                                                timeScale.scrollToPosition(timeScale.scrollPosition() + timeDelta, false);
                                            }
                                        }
                                        break;
                                        
                                    case 'scale':
                                        // 핀치 줌 - 차트 확대/축소
                                        var scaleFactor = args[0] || 0.5;
                                        var currentSpan = args[1] || 0;
                                        var previousSpan = args[2] || 0;
                                        
                                        if (scaleFactor !== 1.0) {
                                            var timeScale = chart.timeScale();
                                            var visibleRange = timeScale.getVisibleRange();
                                            if (visibleRange) {
                                                var centerTime = (visibleRange.from + visibleRange.to) / 2;
                                                var range = visibleRange.to - visibleRange.from;
                                                var newRange = range / scaleFactor;
                                                
                                                // 최소/최대 줌 제한
                                                newRange = Math.max(10, Math.min(newRange, 1000));
                                                
                                                var newFrom = centerTime - newRange / 2;
                                                var newTo = centerTime + newRange / 2;
                                                
                                                timeScale.setVisibleRange({
                                                    from: newFrom,
                                                    to: newTo
                                                });
                                            }
                                        }
                                        break;
                                        
                                    case 'doubletap':
                                        // 더블탭 - 차트 리셋
                                        chart.timeScale().fitContent();
                                        break;
                                        
                                    case 'longpress':
                                        // 롱프레스 - 크로스헤어 고정
                                        chart.setCrosshairPosition(x, y, chart.timeScale().coordinateToTime(x));
                                        break;
                                        
                                    case 'end':
                                    case 'cancel':
                                        // 터치 종료 - 크로스헤어 숨김
                                        chart.clearCrosshairPosition();
                                        break;
                                }
                                
                                return 'handled';
                            } catch (error) {
                                console.error('터치 이벤트 처리 오류:', error);
                                return 'error';
                            }
                        };
                        
                        console.log('차트 생성 완료:', chart);



                        // 기본 캔들스틱 시리즈 생성 (현재가 라인 표시)
                        candlestickSeries = chart.addCandlestickSeries({
                            upColor: '#2196F3',
                            downColor: '#EF5350',
                            borderDownColor: '#EF5350',
                            borderUpColor: '#2196F3',
                            wickDownColor: '#EF5350',
                            wickUpColor: '#2196F3',
                            // 우측 현재가 표시
                            lastValueVisible: true,
                            priceLineVisible: false,
                            priceFormat: {
                                type: 'price',
                                precision: 2,
                                minMove: 0.01
                            }
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
                
                // 차트 상태 저장 변수
                let savedChartState = null;
                
                // 차트 상태 저장 함수
                function saveChartState() {
                    try {
                        if (!chart) {
                            console.log('차트가 초기화되지 않아 상태를 저장할 수 없습니다.');
                            return null;
                        }
                        
                        const timeScale = chart.timeScale();
                        const visibleRange = timeScale.getVisibleRange();
                        const logicalRange = timeScale.getVisibleLogicalRange();
                        
                        savedChartState = {
                            visibleRange: visibleRange,
                            logicalRange: logicalRange,
                            scrollPosition: timeScale.scrollPosition()
                        };
                        
                        console.log('차트 상태 저장됨:', savedChartState);
                        return savedChartState;
                    } catch (error) {
                        console.error('차트 상태 저장 오류:', error);
                        return null;
                    }
                }
                
                // 차트 상태 복원 함수
                function restoreChartState() {
                    try {
                        if (!chart || !savedChartState) {
                            console.log('복원할 차트 상태가 없습니다.');
                            return false;
                        }
                        
                        const timeScale = chart.timeScale();
                        
                        // 저장된 상태 복원
                        if (savedChartState.visibleRange) {
                            timeScale.setVisibleRange(savedChartState.visibleRange);
                            console.log('차트 visible range 복원됨:', savedChartState.visibleRange);
                        } else if (savedChartState.logicalRange) {
                            timeScale.setVisibleLogicalRange(savedChartState.logicalRange);
                            console.log('차트 logical range 복원됨:', savedChartState.logicalRange);
                        }
                        
                        return true;
                    } catch (error) {
                        console.error('차트 상태 복원 오류:', error);
                        return false;
                    }
                }
                
                // 차트 데이터 설정 함수 (상태 유지 옵션 추가)
                function setChartData(candlestickData, preserveState = false) {
                    try {
                        if (!candlestickSeries || !volumeSeries) {
                            console.error('차트 시리즈가 초기화되지 않았습니다.');
                            return;
                        }
                        
                        console.log('차트 데이터 설정 시작:', candlestickData.length + '개', '상태 유지:', preserveState);
                        
                        // 상태 유지가 요청된 경우 현재 상태 저장
                        if (preserveState) {
                            saveChartState();
                        }
                        
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
                                color: isUp ? 'rgba(33, 150, 243, 0.4)' : 'rgba(239, 83, 80, 0.4)'
                            };
                        });
                        volumeSeries.setData(volumeData);
                        
                        // 현재가 라인 색상 업데이트
                        updateCurrentPriceLineColor(candlestickData);
                        
                            // 차트 데이터를 전역에 저장 (스크롤 업데이트용)
                            window.chartData = candlestickData;
                            
                            // 스크롤 색상 업데이트 설정
                            setupScrollColorUpdate();
                        
                        // 차트 범위 설정
                        if (chart && container) {
                            if (preserveState && savedChartState) {
                                // 저장된 상태 복원
                                restoreChartState();
                            } else {
                                // 기본 범위 설정 (초기 로드 시)
                                const visibleCount = Math.min(10, candlestickData.length);
                            const startIndex = Math.max(0, candlestickData.length - visibleCount);
                            
                            chart.timeScale().setVisibleLogicalRange({
                                from: startIndex,
                                to: candlestickData.length - 1
                            });
                            }
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

                // 화면에 보이는 마지막 캔들 색상에 맞춰 레이블 업데이트
                function updateCurrentPriceLineColor(candlestickData) {
                    if (candlestickData && candlestickData.length > 0 && candlestickSeries) {
                        // 현재 화면에 보이는 시간 범위 가져오기
                        let visibleCandle = null;
                        try {
                            const timeScale = chart.timeScale();
                            const visibleRange = timeScale.getVisibleRange();
                            
                            if (visibleRange) {
                                // 보이는 범위의 마지막 캔들 찾기
                                const visibleTo = visibleRange.to;
                                
                                // 보이는 범위의 마지막 시간과 가장 가까운 캔들 찾기
                                visibleCandle = candlestickData.reduce((closest, current) => {
                                    const currentTime = parseFloat(current.time);
                                    const closestTime = parseFloat(closest.time);
                                    
                                    return Math.abs(currentTime - visibleTo) < Math.abs(closestTime - visibleTo) 
                                        ? current : closest;
                                });
                            }
                        } catch (e) {
                            console.log('보이는 범위 가져오기 실패, 마지막 캔들 사용:', e);
                        }
                        
                        // 보이는 캔들이 없으면 마지막 캔들 사용
                        const targetCandle = visibleCandle || candlestickData[candlestickData.length - 1];
                        
                        if (targetCandle) {
                            // 해당 캔들의 색상에 따라 레이블 색상 결정
                            const isUp = targetCandle.close >= targetCandle.open;
                            const lineColor = isUp ? '#2196F3' : '#EF5350'; // 상승: 파랑, 하락: 빨강
                            
                            // 현재가 레이블 DOM 요소 직접 찾아서 색상 변경
                            try {
                                // 짧은 지연 후 레이블 요소 찾기 (DOM 업데이트 대기)
                                setTimeout(() => {
                                    // 차트 컨테이너 내의 모든 테이블 요소 찾기
                                    const chartContainer = document.querySelector('.tv-lightweight-charts');
                                    if (chartContainer) {
                                        // 우측 price scale의 모든 레이블 찾기
                                        const priceLabels = chartContainer.querySelectorAll('table tr td');
                                        
                                        // 보이는 캔들의 close 가격과 일치하는 레이블 찾기
                                        const targetPrice = targetCandle.close.toFixed(2);
                                        priceLabels.forEach(label => {
                                            const labelText = label.textContent?.trim();
                                            
                                            // 기존 스타일 초기화
                                            label.style.backgroundColor = '';
                                            label.style.color = '';
                                            label.style.border = '';
                                            label.style.fontWeight = '';
                                            
                                            // 타겟 가격과 일치하는 레이블에만 색상 적용
                                            if (labelText === targetPrice) {
                                                label.style.backgroundColor = lineColor;
                                                label.style.color = 'white';
                                                label.style.border = '1px solid ' + lineColor;
                                                label.style.fontWeight = 'bold';
                                                console.log('보이는 캔들 레이블 색상 적용:', labelText, lineColor);
                                            }
                                        });
                                        
                                        // 더 포괄적인 검색 - 모든 div 요소
                                        const allDivs = chartContainer.querySelectorAll('div');
                                        allDivs.forEach(div => {
                                            const divText = div.textContent?.trim();
                                            
                                            // 기존 스타일 초기화
                                            if (div.dataset.priceLabel) {
                                                div.style.backgroundColor = '';
                                                div.style.color = '';
                                                div.style.border = '';
                                                div.style.borderRadius = '';
                                            }
                                            
                                            // 타겟 가격과 일치하는 div에만 색상 적용
                                            if (divText === targetPrice && div.style.position === 'absolute') {
                                                div.style.backgroundColor = lineColor;
                                                div.style.color = 'white';
                                                div.style.border = '1px solid ' + lineColor;
                                                div.style.borderRadius = '2px';
                                                div.dataset.priceLabel = 'true';
                                                console.log('보이는 캔들 div 레이블 색상 적용:', divText, lineColor);
                                            }
                                        });
                                    }
                                }, 100);
                            } catch (e) {
                                console.log('현재가 레이블 색상 적용 실패:', e);
                            }
                            
                            candlestickSeries.applyOptions({
                                lastValueVisible: true,
                                priceLineVisible: false  // 라인은 표시하지 않음
                            });
                            
                            console.log('보이는 캔들 기준 레이블 색상 업데이트:', {
                                price: targetCandle.close,
                                open: targetCandle.open,
                                isUp: isUp,
                                lineColor: lineColor,
                                isVisibleCandle: !!visibleCandle
                            });
                        }
                    }
                }
                
                // 스크롤할 때마다 레이블 색상 업데이트하는 함수
                function setupScrollColorUpdate() {
                    if (chart && candlestickSeries) {
                        const timeScale = chart.timeScale();
                        
                        // 화면 범위 변경 시 레이블 색상 업데이트
                        timeScale.subscribeVisibleTimeRangeChange(() => {
                            try {
                                // 현재 차트의 모든 데이터 가져오기
                                const allData = window.chartData || [];
                                if (allData.length > 0) {
                                    updateCurrentPriceLineColor(allData);
                                }
                            } catch (e) {
                                console.log('스크롤 색상 업데이트 실패:', e);
                            }
                        });
                        
                        console.log('스크롤 색상 업데이트 설정 완료');
                    }
                }
                
                // Android에서 호출할 수 있도록 전역 함수로 노출
                window.setChartData = setChartData;
                window.updateCurrentPriceLineColor = updateCurrentPriceLineColor;
                window.setupScrollColorUpdate = setupScrollColorUpdate;
                window.saveChartState = saveChartState;
                window.restoreChartState = restoreChartState;
                
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
                val volumeColor = if (isUp) "rgba(33, 150, 243, 0.3)" else "rgba(239, 83, 80, 0.3)" // 파랑/빨간색 반투명

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
                // Fragment가 destroy된 경우 binding이 null일 수 있으므로 체크
                if (_binding != null) {
                binding.chartWebView.evaluateJavascript(checkReadyCode) { result: String? ->
                        // 콜백 실행 시점에도 binding이 null일 수 있으므로 체크
                        if (_binding != null) {
                    Log.d(TAG, "차트 준비 상태 확인: $result")
                    if (result == "\"ready\"") {
                        Log.d(TAG, "차트가 준비됨 - 데이터 설정 시작")
                        // 차트가 준비되었으면 데이터 설정 (상태 유지 옵션과 함께)
                        val jsCode = """
                            try {
                                console.log('차트 데이터 설정 시작');
                                if (window.setChartData) {
                                    console.log('setChartData 함수 호출 (상태 유지: true)');
                                    window.setChartData($candlestickJson, true);
                                    console.log('차트 데이터 설정 완료');
                                    
                                    // 현재가 라인 색상 업데이트 (스크롤에 반응)
                                    if (window.updateCurrentPriceLineColor && $candlestickJson.length > 0) {
                                        window.updateCurrentPriceLineColor($candlestickJson);
                                    
                                    // 차트 데이터 업데이트
                                    window.chartData = $candlestickJson;
                                    }
                                } else {
                                    console.error('setChartData 함수가 정의되지 않음');
                                }
                            } catch (error) {
                                console.error('차트 데이터 설정 오류:', error);
                            }
                        """.trimIndent()

                        // Fragment가 destroy된 경우 binding이 null일 수 있으므로 체크
                        if (_binding != null) {
                        binding.chartWebView.evaluateJavascript(jsCode) { jsResult: String? ->
                                // 콜백 실행 시점에도 binding이 null일 수 있으므로 체크
                                if (_binding != null) {
                            Log.d(TAG, "JavaScript 실행 완료: $jsResult")
                                } else {
                                    Log.w(TAG, "JavaScript 콜백: binding이 null입니다.")
                                }
                            }
                        } else {
                            Log.w(TAG, "JavaScript 실행 건너뜀: binding이 null입니다.")
                        }
                    } else {
                        // 차트가 준비되지 않았으면 500ms 후 재시도
                        Log.d(TAG, "차트가 준비되지 않음, 500ms 후 재시도")
                        binding.chartWebView.postDelayed({
                            // Fragment가 destroy된 경우 binding이 null일 수 있으므로 체크
                            if (_binding != null) {
                                updateChartData()
                            } else {
                                Log.w(TAG, "postDelayed: binding이 null입니다. Fragment가 destroy되었을 수 있습니다.")
                            }
                        }, 500)
                    }
                        } else {
                            Log.w(TAG, "차트 준비 상태 확인 콜백: binding이 null입니다.")
                        }
                    }
                } else {
                    Log.w(TAG, "차트 준비 상태 확인 건너뜀: binding이 null입니다.")
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
                        Log.d(TAG, "===== 폴링 실행 (${ticker}) =====")
                        loadTradeData(showLoading = false) // 폴링 시에는 로딩 표시하지 않음
                    } else {
                        Log.d(TAG, "폴링 건너뜀 - isPollingActive: $isPollingActive, ticker: $ticker")
                    }
                } catch (e: CancellationException) {
                    Log.d(TAG, "폴링이 정상적으로 취소됨")
                    break
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
        scaleTouchDelegate = null
        
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