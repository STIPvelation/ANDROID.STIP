package com.stip.stip.iptransaction.fragment

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.stip.ipasset.repository.IpAssetRepository
import androidx.recyclerview.widget.LinearLayoutManager
import com.stip.stip.R
import com.stip.stip.ScrollableToTop
import com.stip.stip.custom.MyDividerItemDecoration
import com.stip.stip.databinding.FragmentIpHoldingBinding
import com.stip.stip.iptransaction.api.IpTransactionService
import com.stip.stip.iptransaction.model.DipHoldingitem
import com.stip.stip.iptransaction.model.MyIpHoldingsSummaryItem
import com.stip.stip.iptransaction.model.PortfolioIPResponseDto
import com.stip.stip.iptransaction.model.PortfolioIPSummaryDto
import com.stip.stip.iptransaction.model.PortfolioIPChartItemDto
import com.stip.stip.iptransaction.model.PortfolioIPHoldingDto
import com.stip.stip.signup.utils.PreferenceUtil
import com.github.mikephil.charting.components.Legend // 범례 설정 위해 추가
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
// import com.github.mikephil.charting.formatter.PercentFormatter // 사용 안 함
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.ArrayList
import java.math.BigDecimal
import com.stip.stip.iptransaction.fragment.PortfolioHoldingsAdapter

class IpHoldingFragment : Fragment(), ScrollableToTop {

    private lateinit var mainViewModel: com.stip.stip.MainViewModel
    private var _binding: FragmentIpHoldingBinding? = null
    private val binding get() = _binding!!

    private var isChartExpanded = true // 접기/펼치기 상태

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mainViewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[com.stip.stip.MainViewModel::class.java]
        _binding = FragmentIpHoldingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel.memberInfo.observe(viewLifecycleOwner) { memberInfo ->
            // TODO: 회원정보를 UI에 반영하는 코드 작성
        }

        // 자산 및 거래 내역 데이터 새로고침 이벤트 수신 설정
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                mainViewModel.refreshAppDataEvent.collect {
                    // 보유 잔고 데이터 새로고침
                    loadHoldingData()
                }
            }
        }
        
        // 포트폴리오 차트 접기/펼치기 기능 설정
        setupExpandCollapseAction()

        loadHoldingData()

        setupPieChart() // 수정된 범례 설정 반영
        updateChartVisibilityState() // 초기 차트 가시성 상태 설정
    }

    /**
     * 보유 내역 데이터 로드
     */
    private fun loadHoldingData() {
        lifecycleScope.launch {
            val memberId = PreferenceUtil.getUserId()
            if (memberId.isNullOrBlank()) {
                if (isAdded) {
                    binding.noipMessageText.visibility = View.VISIBLE
                    binding.noipMessageText.text = "로그인 정보가 없습니다."
                }
                return@launch
            }

            try {
                // 포트폴리오 API 호출
                val portfolioRepository = com.stip.api.repository.PortfolioRepository()
                val portfolioResponse = portfolioRepository.getPortfolioResponse(memberId)
                
                if (isAdded) {
                    if (portfolioResponse != null) {
                        // 요약 정보 표시
                        setupPortfolioSummaryInfo(portfolioResponse)
                        binding.summarySection.visibility = View.VISIBLE
                        
                        // 보유 내역 표시 (USD 제외하고 수량이 0개인 항목 제외)
                        val nonZeroWallets = portfolioResponse.wallets.filter { wallet ->
                            wallet.symbol != "USD" && wallet.balance > BigDecimal.ZERO
                        }
                        
                        if (nonZeroWallets.isNotEmpty()) {
                            // 마켓페어 정보를 미리 로드
                            val marketPairMap = loadMarketPairMapForHoldings(nonZeroWallets)
                            
                            val portfolioAdapter = PortfolioHoldingsAdapter(
                                holdings = nonZeroWallets.map { wallet ->
                                    com.stip.stip.iptransaction.model.PortfolioIPHoldingDto(
                                        marketPairId = wallet.marketPairId,
                                        symbol = wallet.symbol,
                                        walletId = wallet.walletId,
                                        balance = wallet.balance,
                                        address = wallet.address,
                                        price = wallet.price,
                                        evalAmount = wallet.evalAmount,
                                        buyAmount = wallet.buyAmount,
                                        buyAvgPrice = wallet.buyAvgPrice,
                                        profit = wallet.profit,
                                        profitRate = wallet.profitRate,
                                        name = wallet.symbol,
                                        amount = wallet.balance
                                    )
                                },
                                marketPairMap = marketPairMap
                            )
                            
                            binding.holdingsRecyclerView.apply {
                                layoutManager = LinearLayoutManager(requireContext())
                                adapter = portfolioAdapter
                            }
                            binding.holdingsRecyclerView.visibility = View.VISIBLE
                            binding.noipcurrently.visibility = View.GONE
                            
                            // 포트폴리오 차트 데이터 로드
                            loadPortfolioPieChartDataFromApi(portfolioResponse.portfolioChart, portfolioResponse.evalAmount ?: BigDecimal.ZERO)
                        } else {
                            binding.noipcurrently.visibility = View.VISIBLE
                            binding.noipMessageText.visibility = View.VISIBLE
                            binding.noipMessageText.text = "보유 중인 IP가 없습니다."
                            binding.holdingsRecyclerView.visibility = View.GONE
                        }
                    } else {
                        // API 응답이 null인 경우
                        binding.noipMessageText.visibility = View.VISIBLE
                        binding.noipMessageText.text = "데이터를 불러오는 중 오류가 발생했습니다."
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("IpHoldingFragment", "보유 내역 로드 실패: ${e.message}", e)
                if (isAdded) {
                    binding.noipMessageText.visibility = View.VISIBLE
                    binding.noipMessageText.text = "데이터를 불러오는 중 오류가 발생했습니다."
                }
            }
        }
    }

    /**
     * 포트폴리오 요약 정보 설정
     */
    private fun setupPortfolioSummaryInfo(portfolioResponse: com.stip.api.model.PortfolioResponse) {
        val formatter = java.text.DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }
        
        // USD 잔고
        binding.holdingUsdText.text = "$${formatter.format(portfolioResponse.usdBalance?.toDouble() ?: 0.0)}"
        
        // 총 보유자산
        binding.totalAssetText.text = "$${formatter.format(portfolioResponse.totalAmount?.toDouble() ?: portfolioResponse.evalAmount?.toDouble() ?: 0.0)}"
        
        // 총 매수금액
        binding.totalBuyText.text = "$${formatter.format(portfolioResponse.buyAmount?.toDouble() ?: 0.0)}"
        
        // 평가손익
        binding.valuationProfitText.text = "$${formatter.format(portfolioResponse.profit?.toDouble() ?: 0.0)}"
        
        // 총 평가금액 (IP 자산만)
        binding.totalValuationText.text = "$${formatter.format(portfolioResponse.evalAmount?.toDouble() ?: 0.0)}"
        
        // 수익률
        binding.profitRateText.text = "${formatter.format(portfolioResponse.profitRate?.toDouble() ?: 0.0)}%"
        
        // 주문 가능
        binding.availableOrderText.text = "$${formatter.format(portfolioResponse.usdAvailableBalance?.toDouble() ?: 0.0)}"
        
        // 평가손익과 수익률 색상 설정
        val profit = portfolioResponse.profit?.toDouble() ?: 0.0
        val isLoss = profit < 0
        val colorRes = if (isLoss) R.color.color_fall else R.color.color_rise
        val color = ContextCompat.getColor(requireContext(), colorRes)
        binding.valuationProfitText.setTextColor(color)
        binding.profitRateText.setTextColor(color)
    }

    /**
     * 포트폴리오 차트 데이터 로드
     */
    private fun loadPortfolioPieChartDataFromApi(
        portfolioChart: List<com.stip.api.model.PortfolioChartItemDto>,
        totalEval: java.math.BigDecimal?
    ) {
        if (portfolioChart.isNotEmpty()) {
            // 차트 데이터 설정
            val chartData = portfolioChart.map { chartItem ->
                com.stip.stip.iptransaction.model.PortfolioIPChartItemDto(
                    symbol = chartItem.symbol ?: "",
                    name = chartItem.name ?: "",
                    percent = chartItem.percent ?: BigDecimal.ZERO
                )
            }
            
            // 기존 차트 설정 로직 사용
            loadPortfolioPieChartData(chartData, totalEval)
        }
    }

    // 차트 가시성 및 아이콘 업데이트
    private fun updateChartVisibilityState() {
        if (isChartExpanded) {
            binding.portfolioPieChart.visibility = View.VISIBLE
            binding.ivExpandIcon.setImageResource(R.drawable.ic_arrow_up)
        } else {
            binding.portfolioPieChart.visibility = View.GONE
            binding.ivExpandIcon.setImageResource(R.drawable.ic_arrow_down)
        }
    }
    
    // 확장/축소 기능 구현
    private fun setupExpandCollapseAction() {
        binding.ivExpandIcon.setOnClickListener {
            isChartExpanded = !isChartExpanded
            updateChartVisibilityState()
        }
    }

    private fun setupSummaryInfo(summary: MyIpHoldingsSummaryItem) {
        // ... (기존 코드 유지) ...
        with(binding) {
            holdingUsdText.text = formatUsd(summary.usdBalance.toDouble())
            totalBuyText.text = formatUsd(summary.buyAmount.toDouble())
            totalValuationText.text = formatUsd(summary.evalAmount.toDouble())
            totalAssetText.text = formatUsd(summary.totalAmount?.toDouble() ?: summary.evalAmount.toDouble())
            valuationProfitText.text = formatUsd(summary.profit.toDouble())
            profitRateText.text = formatPercent(summary.profitRate.toDouble())
            availableOrderText.text = formatUsd(summary.usdAvailableBalance.toDouble())

            val isLoss = summary.profit.toDouble() < 0
            val colorRes = if (isLoss) R.color.color_fall else R.color.color_rise
            val color = ContextCompat.getColor(requireContext(), colorRes)
            valuationProfitText.setTextColor(color)
            profitRateText.setTextColor(color)
        }
    }

    private fun setupPieChart() {
        binding.portfolioPieChart.apply {
            isDrawHoleEnabled = true
            setUsePercentValues(false) // 퍼센트 값 자동 계산/표시 비활성화
            setEntryLabelTextSize(0f) // 차트 조각 라벨 숨김
            description.isEnabled = false
            setTouchEnabled(false)
            isRotationEnabled = false
            holeRadius = 65f
            transparentCircleRadius = 68f
            setHoleColor(Color.TRANSPARENT)
            setDrawCenterText(true)

            // 범례 설정 (오른쪽 기본값 또는 명시적 설정)
            legend.isEnabled = true
            // 기본값 사용 또는 필요시 명시적 설정
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT // 오른쪽
            legend.verticalAlignment = Legend.LegendVerticalAlignment.CENTER   // 수직 중앙
            legend.orientation = Legend.LegendOrientation.VERTICAL      // 세로 나열
            legend.setDrawInside(false)                                 // 차트 바깥쪽
            legend.isWordWrapEnabled = true                             // 긴 라벨 줄바꿈 허용
        }
    }

    // 데이터 로드 및 범례 라벨 수정
    private fun loadPieChartData(holdings: List<DipHoldingitem>) {
        val entries = ArrayList<PieEntry>()
        // 퍼센트 계산을 위한 전체 합계 계산
        val totalSum = holdings.sumOf { it.evalAmount?.toDouble() ?: 0.0 }

        for (item in holdings) {
            val evalAmount = item.evalAmount?.toDouble() ?: 0.0
            if (evalAmount > 0) {
                // 해당 항목의 퍼센티지 계산
                val percentage = if (totalSum > 0) (evalAmount / totalSum) * 100 else 0.0
                // 퍼센티지 문자열 포맷
                val percentageString = String.format("%.2f%%", percentage)
                // 범례에 표시될 최종 라벨 생성 ("이름  00.00%")
                val label = "${item.name ?: ""}  ${percentageString}"

                // PieEntry 생성 시 값과 *수정된 라벨* 사용
                entries.add(PieEntry(evalAmount.toFloat(), label))
            }
        }

        var shouldShowChart = false

        if (entries.isNotEmpty()) {
            shouldShowChart = true
            val dataSet = PieDataSet(entries, "") // 범례 그룹 제목은 비움
            dataSet.setColors(*ColorTemplate.MATERIAL_COLORS) // 색상 설정
            dataSet.setDrawValues(false) // *** 차트 조각 위에 값 표시 안 함 ***

            val data = PieData(dataSet)
            // data.setValueFormatter(...) // *** 값 포맷터 설정 필요 없음 ***

            binding.portfolioPieChart.data = data
            // 가운데 텍스트는 총 평가금액을 계속 표시 (totalSum 사용)
            binding.portfolioPieChart.centerText = generateCenterSpannableText(totalSum)
            binding.portfolioPieChart.invalidate()

        } else {
            shouldShowChart = false
            binding.portfolioPieChart.clear()
            binding.portfolioPieChart.invalidate()
        }

        // 최종 차트 가시성 결정
        if (!shouldShowChart) {
            isChartExpanded = false
        }
        updateChartVisibilityState() // 차트 데이터 로드 후 가시성 업데이트
    }

    // 포트폴리오 차트 데이터 로드
    private fun loadPortfolioPieChartData(chartItems: List<PortfolioIPChartItemDto>, totalValue: BigDecimal?) {
        val entries = ArrayList<PieEntry>()

        for (item in chartItems) {
            val percent = item.percent ?: BigDecimal.ZERO
            if (percent > BigDecimal.ZERO) {
                // 범례에 표시될 최종 라벨 생성 ("이름  00.00%") - 소수점 둘째 자리까지 표시
                val percentageString = String.format("%.2f%%", percent.toDouble())
                val label = "${item.name ?: ""}  $percentageString"

                // PieEntry 생성 시 값과 라벨 사용
                entries.add(PieEntry(percent.toFloat(), label))
            }
        }

        var shouldShowChart = false

        if (entries.isNotEmpty()) {
            shouldShowChart = true
            val dataSet = PieDataSet(entries, "")
            dataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
            dataSet.setDrawValues(false)

            val data = PieData(dataSet)

            binding.portfolioPieChart.data = data
            // 가운데 텍스트는 총 평가금액을 표시
            binding.portfolioPieChart.centerText = generateCenterSpannableText(totalValue?.toDouble() ?: 0.0)
            binding.portfolioPieChart.invalidate()

        } else {
            shouldShowChart = false
            binding.portfolioPieChart.clear()
            binding.portfolioPieChart.invalidate()
        }

        // 최종 차트 가시성 결정
        if (!shouldShowChart) {
            isChartExpanded = false
        }
        updateChartVisibilityState()
    }

    // 가운데 텍스트 생성 함수 (총 평가금액 표시)
    private fun generateCenterSpannableText(totalValue: Double): SpannableString {
        val label = getString(R.string.center_label_total_value)
        val formattedTotalValue = formatUsd(totalValue)
        val text = "$label\n$formattedTotalValue"
        val spannableString = SpannableString(text)

        val labelStartIndex = 0
        val labelEndIndex = text.indexOf('\n')
        if (labelEndIndex > 0) {
            spannableString.setSpan(RelativeSizeSpan(0.8f), labelStartIndex, labelEndIndex, 0)
            spannableString.setSpan(ForegroundColorSpan(Color.GRAY), labelStartIndex, labelEndIndex, 0)
        }

        val valueStartIndex = labelEndIndex + 1
        val valueEndIndex = text.length
        if (valueStartIndex < valueEndIndex) {
            spannableString.setSpan(RelativeSizeSpan(1.1f), valueStartIndex, valueEndIndex, 0)
            spannableString.setSpan(StyleSpan(Typeface.BOLD), valueStartIndex, valueEndIndex, 0)
            spannableString.setSpan(ForegroundColorSpan(Color.BLACK), valueStartIndex, valueEndIndex, 0)
        }

        return spannableString
    }


    override fun scrollToTop() {
        binding.holdingsRecyclerView.smoothScrollToPosition(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(): IpHoldingFragment = IpHoldingFragment()
    }

    private fun formatUsd(value: Double): String {
        return "$" + String.format("%,.2f", value)
    }

    private fun formatPercent(value: Double): String {
        return String.format("%.2f%%", value)
    }
    
    /**
     * 보유 종목들의 마켓페어 정보를 로드
     */
    private suspend fun loadMarketPairMapForHoldings(
        wallets: List<com.stip.api.model.PortfolioWalletItemDto>
    ): Map<String, Pair<String, String>> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            android.util.Log.d("IpHoldingFragment", "마켓페어 정보 로딩 시작: ${wallets.size}개 보유 종목")
            
            // 전체 마켓페어 목록을 한 번만 가져옴
            val marketPairsService = com.stip.stip.api.RetrofitClient.createTapiService(com.stip.stip.api.service.MarketPairsService::class.java)
            val allMarketPairs = marketPairsService.getMarketPairs()
            
            // 보유 종목의 marketPairId만 추출
            val holdingMarketPairIds = wallets.map { it.marketPairId }.toSet()
            
            // 필요한 마켓페어만 필터링해서 맵 생성
            val resultMap = allMarketPairs
                .filter { marketPair -> holdingMarketPairIds.contains(marketPair.id) }
                .associate { marketPair -> 
                    marketPair.id to Pair(marketPair.name ?: "", marketPair.symbol ?: "") 
                }
            resultMap
        } catch (e: Exception) {
            android.util.Log.e("IpHoldingFragment", "마켓페어 정보 로딩 실패", e)
            emptyMap()
        }
    }

    private fun simulateDipHoldings(): Pair<List<DipHoldingitem>, MyIpHoldingsSummaryItem> {
        // ... (기존 코드 유지) ...
        val holdings = listOf(
            DipHoldingitem(
                symbol = "WETALK",
                balance = BigDecimal("10"),
                price = BigDecimal("5.25"),
                evalAmount = BigDecimal("52.5"),
                buyAmount = BigDecimal("50.0"),
                buyAvgPrice = BigDecimal("5.0"),
                profit = BigDecimal("2.5"),
                profitRate = BigDecimal("5.0")
            ),
            DipHoldingitem(
                symbol = "MDM",
                balance = BigDecimal("20"),
                price = BigDecimal("2.45"),
                evalAmount = BigDecimal("49.0"),
                buyAmount = BigDecimal("50.0"),
                buyAvgPrice = BigDecimal("2.5"),
                profit = BigDecimal("-1.0"),
                profitRate = BigDecimal("-2.0")
            ),
            DipHoldingitem(
                symbol = "CDM",
                balance = BigDecimal("5"),
                price = BigDecimal("11.0"),
                evalAmount = BigDecimal("55.0"),
                buyAmount = BigDecimal("50.0"),
                buyAvgPrice = BigDecimal("10.0"),
                profit = BigDecimal("5.0"),
                profitRate = BigDecimal("10.0")
            ),
            DipHoldingitem(
                symbol = "TEST",
                balance = BigDecimal("5"),
                price = BigDecimal("11.0"),
                evalAmount = BigDecimal("55.0"),
                buyAmount = BigDecimal("50.0"),
                buyAvgPrice = BigDecimal("10.0"),
                profit = BigDecimal("5.0"),
                profitRate = BigDecimal("10.0")
            )

        )
        val totalBuy = holdings.sumOf { it.buyAmount.toDouble() }
        val totalValuation = holdings.sumOf { it.evalAmount.toDouble() }
        val valuationProfit = totalValuation - totalBuy
        val profitRate = if (totalBuy > 0) (valuationProfit / totalBuy) * 100 else 0.0

        val summary = MyIpHoldingsSummaryItem(
            usdBalance = BigDecimal("100000.0"),
            usdAvailableBalance = BigDecimal("98500.0"),
            evalAmount = BigDecimal(totalValuation.toString()),
            buyAmount = BigDecimal(totalBuy.toString()),
            profit = BigDecimal(valuationProfit.toString()),
            profitRate = BigDecimal(profitRate.toString())
        )
        return holdings to summary
    }
}