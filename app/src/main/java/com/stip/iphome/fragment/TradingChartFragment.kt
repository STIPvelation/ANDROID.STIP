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
                // 디버깅을 위한 설정
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                // 추가 설정
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = false
            }
            
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
                override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
                    super.onConsoleMessage(message, lineNumber, sourceID)
                    Log.d(TAG, "WebView Console: $message (line: $lineNumber, source: $sourceID)")
                }
            }
            

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
            // 다른 필터 선택 시에만 데이터 로드
            updateTimeFilterUI()
            loadTradeData(showLoading = true)
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
        
        loadTradeData(showLoading = true)
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
                binding.chartWebView.visibility = View.GONE
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
                }
                #chart {
                    width: 100%;
                    height: 100%;
                    min-height: 300px;
                    background-color: #FFFFFF;
                    position: relative;
                    display: block;
                    margin: 0;
                    padding: 0;
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
                            chart.remove();
                            chart = null;
                        }
                        
                        // 차트 생성
                        chart = LightweightCharts.createChart(container, {
                            width: container.clientWidth || window.innerWidth,
                            height: container.clientHeight || window.innerHeight,
                            layout: {
                                background: { color: '#FFFFFF' },
                                textColor: '#333333',
                            },
                            grid: {
                                vertLines: { color: '#E1E5E9' },
                                horzLines: { color: '#E1E5E9' },
                            },
                            crosshair: {
                                mode: LightweightCharts.CrosshairMode.Normal,
                                vertLine: {
                                    visible: true,
                                    labelVisible: false,
                                },
                                horzLine: {
                                    visible: true,
                                    labelVisible: true,
                                },
                            },
                            rightPriceScale: {
                                borderColor: '#E1E5E9',
                                textColor: '#333333',
                                scaleMargins: {
                                    top: 0.05,
                                    bottom: 0.25,
                                },
                            },
                            timeScale: {
                                borderColor: '#E1E5E9',
                                textColor: '#333333',
                                timeVisible: $timeVisible,
                                secondsVisible: $secondsVisible,
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
                            leftPriceScale: {
                                visible: false,
                            },
                        });



                        // 캔들스틱 시리즈 생성
                        candlestickSeries = chart.addCandlestickSeries({
                            upColor: '#26A69A',
                            downColor: '#EF5350',
                            borderDownColor: '#EF5350',
                            borderUpColor: '#26A69A',
                            wickDownColor: '#EF5350',
                            wickUpColor: '#26A69A',
                            // 같은 가격일 때는 하락으로 처리
                            priceFormat: {
                                type: 'price',
                                precision: 2,
                                minMove: 0.01,
                            },
                            // 애니메이션 비활성화로 깜빡임 방지
                            lastValueVisible: false,
                            priceLineVisible: false,
                        });

                        // 거래량 시리즈 생성 (하단에 별도 영역)
                        volumeSeries = chart.addHistogramSeries({
                            priceFormat: {
                                type: 'volume',
                            },
                            priceScaleId: 'volume',
                            scaleMargins: {
                                top: 0.8,
                                bottom: 0,
                            },
                            // 애니메이션 비활성화로 깜빡임 방지
                            lastValueVisible: false,
                            // 반투명 설정
                            color: 'rgba(76, 175, 80, 0.3)',
                        });

                        // 거래량 스케일 설정
                        chart.priceScale('volume').applyOptions({
                            scaleMargins: {
                                top: 0.8,
                                bottom: 0,
                            },
                        });

                        // 전역 변수로 노출
                        window.candlestickSeries = candlestickSeries;
                        window.volumeSeries = volumeSeries;
                        window.chart = chart;
                        
                        console.log('차트 초기화 완료');
                        
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
                
                // 차트 데이터 설정 함수 (제거 - 중복 정의)
                
                // 라이브러리 로드 후 차트 초기화 (한 번만 실행)
                if (!libraryLoaded) {
                    loadLibrary().then(function() {
                        console.log('라이브러리 로드 완료, 차트 초기화 시작');
                        initChart();
                    }).catch(function(error) {
                        console.error('라이브러리 로드 실패:', error);
                    });
                }
                
                // 윈도우 리사이즈 이벤트
                window.addEventListener('resize', function() {
                    if (chart) {
                        chart.applyOptions({
                            width: window.innerWidth,
                            height: window.innerHeight,
                        });
                    }
                });

                // 윈도우 리사이즈 처리
                window.addEventListener('resize', () => {
                    if (chart) {
                        chart.applyOptions({
                            width: window.innerWidth,
                            height: window.innerHeight,
                        });
                    }
                });

                // 차트 데이터 설정 함수
                function setChartData(candlestickData) {
                    try {
                        if (!candlestickSeries || !volumeSeries) {
                            console.error('차트 시리즈가 초기화되지 않았습니다.');
                            return;
                        }
                        
                        console.log('차트 데이터 설정 시작:', candlestickData.length + '개');
                        console.log('데이터 샘플:', JSON.stringify(candlestickData[0]));
                        
                        // 캔들스틱 데이터 설정
                        candlestickSeries.setData(candlestickData);
                        
                        // 거래량 데이터 설정 (색상 포함)
                        var volumeData = candlestickData.map(function(item) {
                            return {
                                time: item.time,
                                value: item.volume,
                                color: item.close >= item.open ? 'rgba(38, 166, 154, 0.5)' : 'rgba(239, 83, 80, 0.5)'
                            };
                        });
                        volumeSeries.setData(volumeData);
                        
                        // 차트 자동 스케일링
                        chart.timeScale().fitContent();
                        
                        // 차트가 보이도록 강제 리사이즈
                        setTimeout(() => {
                            if (chart) {
                                chart.resize(container.clientWidth, container.clientHeight);
                            }
                        }, 50);
                        
                        console.log('차트 데이터 설정 완료');
                        
                    } catch (error) {
                        console.error('차트 데이터 설정 오류:', error);
                    }
                }

                // Android에서 호출할 수 있도록 전역 함수로 노출
                window.setChartData = setChartData;
                
                // 라이브러리 로드 후 차트 초기화 (한 번만 실행)
                if (!libraryLoaded) {
                    loadLibrary().then(() => {
                        console.log('라이브러리 로드 완료, 차트 초기화 시작');
                        initChart();
                    }).catch((error) => {
                        console.error('라이브러리 로드 실패:', error);
                    });
                }
                
                // 페이지 로드 시에도 초기화 시도
                window.addEventListener('load', function() {
                    if (libraryLoaded && !chart) {
                        initChart();
                    }
                });
                
                // DOMContentLoaded 이벤트에서도 초기화 시도
                document.addEventListener('DOMContentLoaded', function() {
                    if (libraryLoaded && !chart) {
                        initChart();
                    }
                });
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
                binding.chartWebView.evaluateJavascript(checkReadyCode) { result ->
                    Log.d(TAG, "차트 준비 상태: $result")
                    if (result == "\"ready\"") {
                        // 차트가 준비되었으면 데이터 설정
                        val jsCode = """
                            try {
                                if (window.setChartData) {
                                    window.setChartData($candlestickJson);
                                }
                            } catch (error) {
                                console.error('차트 데이터 설정 오류:', error);
                            }
                        """.trimIndent()

                        binding.chartWebView.evaluateJavascript(jsCode) { jsResult ->
                            Log.d(TAG, "JavaScript 실행 결과: $jsResult")
                        }
                    } else {
                        // 차트가 준비되지 않았으면 1초 후 재시도
                        Log.d(TAG, "차트가 준비되지 않음, 1초 후 재시도")
                        binding.chartWebView.postDelayed({
                            // Fragment가 destroy된 경우 binding이 null일 수 있으므로 체크
                            if (_binding != null) {
                                updateChartData()
                            } else {
                                Log.w(TAG, "postDelayed: binding이 null입니다. Fragment가 destroy되었을 수 있습니다.")
                            }
                        }, 1000)
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
        // WebView 정리
        try {
            _binding?.chartWebView?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "WebView 정리 중 오류: ${e.message}", e)
        }
        _binding = null
    }
}