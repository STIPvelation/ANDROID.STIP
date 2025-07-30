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

@AndroidEntryPoint
class TradingChartFragment : Fragment() {

    private var _binding: FragmentTradingChartBinding? = null
    private val binding get() = _binding!!

    private var ticker: String? = null
    private var ohlcvData: List<OHLCVData> = emptyList()
    private var pollingJob: kotlinx.coroutines.Job? = null
    private var isPollingActive = false
    
    @Inject
    lateinit var tapiHourlyDataService: TapiHourlyDataService

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
                    }, 1000) // 1초 지연
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

    private fun loadTradeData(showLoading: Boolean = true) {
        // 로딩 상태 표시 (폴링 시에는 표시하지 않음)
        if (showLoading) {
            binding.loadingIndicator.visibility = View.VISIBLE
            binding.chartWebView.visibility = View.GONE
            binding.emptyStateContainer.visibility = View.GONE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val currentTicker = ticker
            if (currentTicker.isNullOrBlank()) {
                Log.e(TAG, "티커 코드가 없습니다. ticker: $currentTicker")
                showEmptyState()
                return@launch
            }

            try {
                Log.d(TAG, "API 데이터 로드 시작 - ticker: $currentTicker")

                // 1. 먼저 market/pairs에서 pairId 찾기
                val pairId = getPairIdForTicker(currentTicker)
                if (pairId == null) {
                    Log.e(TAG, "티커에 해당하는 pairId를 찾을 수 없습니다: $currentTicker")
                    showEmptyState()
                    return@launch
                }

                // 2. hourly ticker 데이터 가져오기
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
            val endDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(calendar.time)
            
            calendar.add(Calendar.MONTH, -3)
            val startDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(calendar.time)
            
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
            tickerDataList.sortedBy { it.timestamp }
            
        } catch (e: Exception) {
            Log.e(TAG, "hourly ticker 데이터 조회 실패: ${e.message}")
            emptyList()
        }
    }

    private fun convertTickerDataToOHLCV(tickerData: List<TickerData>) {
        if (tickerData.isEmpty()) {
            showEmptyState()
            return
        }

        // 시간별로 그룹화하여 OHLCV 데이터 생성
        val groupedData = tickerData.groupBy { data ->
            // UTC 시간을 한국 시간으로 변환 후 1시간 단위로 그룹화
            val timestamp = data.timestamp
            val utcDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            utcDateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val utcDate = utcDateFormat.parse(timestamp)
            
            // UTC를 한국 시간(KST)으로 변환
            val kstTimeZone = TimeZone.getTimeZone("Asia/Seoul")
            val calendar = Calendar.getInstance(kstTimeZone)
            calendar.time = utcDate
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            SimpleDateFormat("yyyy-MM-dd HH:00", Locale.getDefault()).format(calendar.time)
        }

        val ohlcvList = mutableListOf<OHLCVData>()

        groupedData.forEach { (timeKey, dataList) ->
            if (dataList.isNotEmpty()) {
                // 시간순으로 정렬된 데이터에서 첫 번째와 마지막 거래 가격을 기준으로 OHLC 설정
                val sortedData = dataList.sortedBy { it.timestamp }
                val prices = sortedData.map { it.price.toFloat() }
                val volumes = sortedData.map { it.amount.toFloat() }
                
                val open = prices.first()  // 해당 시간대 첫 거래 가격
                val close = prices.last()  // 해당 시간대 마지막 거래 가격
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

        Log.d(TAG, "OHLCV 데이터 변환 완료: ${ohlcvData.size}개")
        
        // 데이터 검증 로그
        ohlcvData.forEach { data ->
            Log.d(TAG, "검증된 OHLCV: ${data.date} - O:${data.open}, H:${data.high}, L:${data.low}, C:${data.close}, V:${data.volume}")
        }
        
        activity?.runOnUiThread {
            updateChart()
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
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Trading Chart</title>
            <style>
                body {
                    margin: 0;
                    padding: 0;
                    background-color: #131722;
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    overflow: hidden;
                }
                #chart-container {
                    width: 100vw;
                    height: 100vh;
                    background-color: #131722;
                    position: relative;
                }
                .chart-wrapper {
                    width: 100%;
                    height: 100%;
                    position: relative;
                }

            </style>
        </head>
        <body>
            <div id="chart-container">
                <div class="chart-wrapper" id="chart"></div>
            </div>
            
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
                        if (!libraryLoaded) {
                            return;
                        }
                        
                        // LightweightCharts 객체 확인
                        if (typeof LightweightCharts === 'undefined') {
                            return;
                        }
                        
                        const container = document.getElementById('chart');
                        if (!container) {
                            return;
                        }
                        
                        // 차트 생성
                        chart = LightweightCharts.createChart(container, {
                            width: window.innerWidth,
                            height: window.innerHeight,
                            layout: {
                                background: { color: '#131722' },
                                textColor: '#D1D4DC',
                            },
                            grid: {
                                vertLines: { color: '#2B2B43' },
                                horzLines: { color: '#2B2B43' },
                            },
                            crosshair: {
                                mode: LightweightCharts.CrosshairMode.Normal,
                            },
                            rightPriceScale: {
                                borderColor: '#2B2B43',
                                textColor: '#D1D4DC',
                                scaleMargins: {
                                    top: 0.1,
                                    bottom: 0.3,
                                },
                            },
                            timeScale: {
                                borderColor: '#2B2B43',
                                textColor: '#D1D4DC',
                                timeVisible: true,
                                secondsVisible: false,
                                tickMarkFormatter: function(time) {
                                    var date = new Date(time * 1000);
                                    // 한국 시간으로 변환 (UTC+9)
                                    var kstDate = new Date(date.getTime() + (9 * 60 * 60 * 1000));
                                    var month = (kstDate.getUTCMonth() + 1).toString().padStart(2, '0');
                                    var day = kstDate.getUTCDate().toString().padStart(2, '0');
                                    var hour = kstDate.getUTCHours().toString().padStart(2, '0');
                                    return month + '/' + day + ' ' + hour + ':00';
                                },
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

                        
                        // 시간 포맷터 설정
                        chart.timeScale().applyOptions({
                            tickMarkFormatter: function(time) {
                                var date = new Date(time * 1000);
                                var month = (date.getMonth() + 1).toString().padStart(2, '0');
                                var day = date.getDate().toString().padStart(2, '0');
                                var hour = date.getHours().toString().padStart(2, '0');
                                return month + '/' + day + ' ' + hour + ':00';
                            },
                        });
                        
                    } catch (error) {
                        // 오류 무시
                    }
                }

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
                function setChartData(candlestickData, volumeData) {
                    try {
                        if (!candlestickSeries || !volumeSeries) {
                            return;
                        }
                        
                        // 부드러운 업데이트를 위해 애니메이션 비활성화
                        chart.applyOptions({
                            rightPriceScale: {
                                autoScale: true,
                                scaleMargins: {
                                    top: 0.1,
                                    bottom: 0.3,
                                },
                            },
                        });
                        
                        // 데이터 업데이트 전에 차트 일시 중지
                        chart.timeScale().scrollPosition();
                        
                        // 캔들스틱 데이터 설정 (부드럽게)
                        candlestickSeries.setData(candlestickData);
                        
                        // 거래량 데이터 설정 (색상 포함)
                        volumeSeries.setData(volumeData);
                        
                        // 차트 자동 스케일링 (부드럽게)
                        chart.timeScale().fitContent();
                        
                        // 데이터 업데이트 후 차트 재개
                        chart.timeScale().scrollToPosition(0, false);
                        

                        
                    } catch (error) {
                        // 오류 무시
                    }
                }

                // Android에서 호출할 수 있도록 전역 함수로 노출
                window.setChartData = setChartData;
                
                // 라이브러리 로드 후 차트 초기화
                loadLibrary().then(() => {
                    console.log('라이브러리 로드 완료, 차트 초기화 시작');
                    initChart();
                }).catch((error) => {
                    // 오류 무시
                });
                
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
        if (ohlcvData.isEmpty()) {
            Log.w(TAG, "OHLCV 데이터가 비어있습니다")
            return
        }

        try {
            Log.d(TAG, "차트 데이터 업데이트 시작: ${ohlcvData.size}개 데이터")

            val candlestickData = ohlcvData.map { data ->
            // 시간을 Unix timestamp로 변환 (한국 시간 기준)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
            val date = dateFormat.parse(data.date)
            val timestamp = (date.time / 1000).toInt()

            val isUp = data.close > data.open
            Log.d(TAG, "캔들스틱 데이터 변환: time=$timestamp, open=${data.open}, high=${data.high}, low=${data.low}, close=${data.close}, isUp=$isUp")

            mapOf(
                "time" to timestamp,
                "open" to data.open,
                "high" to data.high,
                "low" to data.low,
                "close" to data.close
            )
        }

        val volumeData = ohlcvData.map { data ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
            val date = dateFormat.parse(data.date)
            val timestamp = (date.time / 1000).toInt()
            val isUp = data.close > data.open  // 같을 때는 하락으로 처리

            Log.d(TAG, "거래량 데이터 변환: time=$timestamp, volume=${data.volume}, open=${data.open}, close=${data.close}, isUp=$isUp")

            mapOf(
                "time" to timestamp,
                "value" to data.volume,
                "color" to if (isUp) "rgba(38, 166, 154, 0.3)" else "rgba(239, 83, 80, 0.3)"
            )
        }

            Log.d(TAG, "캔들스틱 데이터: ${candlestickData.size}개")
            Log.d(TAG, "거래량 데이터: ${volumeData.size}개")

            // JSON 문자열로 변환
            val candlestickJson = org.json.JSONArray(candlestickData.map { map ->
                org.json.JSONObject().apply {
                    put("time", map["time"])
                    put("open", map["open"])
                    put("high", map["high"])
                    put("low", map["low"])
                    put("close", map["close"])
                }
            }).toString()

            val volumeJson = org.json.JSONArray(volumeData.map { map ->
                org.json.JSONObject().apply {
                    put("time", map["time"])
                    put("value", map["value"])
                    put("color", map["color"])
                }
            }).toString()

            Log.d(TAG, "JSON 변환 완료")

            // 차트 준비 상태 확인 후 데이터 설정
            val checkReadyCode = """
                (function() {
                    if (window.candlestickSeries && window.volumeSeries) {
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
                                    window.setChartData($candlestickJson, $volumeJson);
                                }
                            } catch (error) {
                                // 오류 무시
                            }
                        """.trimIndent()

                        binding.chartWebView.evaluateJavascript(jsCode) { jsResult ->
                            Log.d(TAG, "JavaScript 실행 결과: $jsResult")
                        }
                    } else {
                        // 차트가 준비되지 않았으면 1초 후 재시도
                        Log.d(TAG, "차트가 준비되지 않음, 1초 후 재시도")
                        binding.chartWebView.postDelayed({
                            updateChartData()
                        }, 1000)
                    }
                }
            }

            Log.d(TAG, "차트 데이터 업데이트 완료")

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
        binding.loadingIndicator.visibility = View.GONE
        binding.chartWebView.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.VISIBLE
        

    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPolling()
        _binding = null
    }
}