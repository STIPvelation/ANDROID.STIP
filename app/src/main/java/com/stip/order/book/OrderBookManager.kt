package com.stip.stip.order.book

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.databinding.FragmentOrderContentBinding
import com.stip.stip.iphome.model.OrderBookItem // OrderBookItem import 확인
import com.stip.stip.order.adapter.OrderBookAdapter
import com.stip.stip.R
import com.stip.stip.iptransaction.api.IpTransactionService
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.*
import kotlin.math.floor

class OrderBookManager(
    private val context: Context,
    private val recyclerView: RecyclerView,
    private val orderBookAdapter: OrderBookAdapter,
    private val numberParseFormat: DecimalFormat,
    private val fixedTwoDecimalFormatter: DecimalFormat,
    private val getCurrentPrice: () -> Float,
    private val binding: FragmentOrderContentBinding,
    private val getCurrentPairId: () -> String?
) {

    private var currentAggregationLevel: Double = 0.0
    private var isTotalAmountMode: Boolean = false
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val ORDER_BOOK_UPDATE_INTERVAL_MS = 5000L
        private const val TAG = "OrderBookManager"
        
        // 현재가 구간별 집계 레벨 정의
        private val PRICE_AGGREGATION_MAP = mapOf(
            // 현재가 < 1원
            Pair(0.0, 1.0) to listOf(0.0, 0.001, 0.01, 0.1),
            // 1원 <= 현재가 < 10원
            Pair(1.0, 10.0) to listOf(0.0, 0.01, 0.1, 1.0),
            // 10원 <= 현재가 < 100원
            Pair(10.0, 100.0) to listOf(0.0, 0.1, 1.0, 10.0),
            // 100원 <= 현재가 < 1,000원
            Pair(100.0, 1000.0) to listOf(0.0, 1.0, 10.0, 100.0),
            // 1,000원 <= 현재가 < 10,000원
            Pair(1000.0, 10000.0) to listOf(0.0, 10.0, 100.0, 1000.0),
            // 10,000원 <= 현재가 < 100,000원
            Pair(10000.0, 100000.0) to listOf(0.0, 100.0, 1000.0, 10000.0),
            // 100,000원 <= 현재가 < 1,000,000원
            Pair(100000.0, 1000000.0) to listOf(0.0, 1000.0, 10000.0, 100000.0)
        )
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateOrderBook()
            handler.postDelayed(this, ORDER_BOOK_UPDATE_INTERVAL_MS)
        }
    }

    private fun setupRecyclerView() {
        try {
            recyclerView.apply {
                if (layoutManager == null) {
                    layoutManager = LinearLayoutManager(context)
                }
                if (adapter == null) {
                    adapter = orderBookAdapter
                }
                setHasFixedSize(true)
                itemAnimator = null
            }
            orderBookAdapter.setDisplayMode(this.isTotalAmountMode)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RecyclerView", e)
        }
    }

    fun startAutoUpdate() {
        stopAutoUpdate()
        Log.d(TAG, "Starting auto update.")
        handler.postDelayed(updateRunnable, ORDER_BOOK_UPDATE_INTERVAL_MS)
    }


    fun updateCurrentPrice(newPrice: Float) {
        Log.d("OrderBookManager", "📌 updateCurrentPrice called with: $newPrice")
        orderBookAdapter.updateCurrentPrice(newPrice)
        
        // 현재가가 변경되면 집계 레벨이 유효한지 확인하고 필요시 조정
        val newAggregationLevels = getAggregationLevelsForPrice(newPrice)
        if (!newAggregationLevels.contains(this.currentAggregationLevel)) {
            // 현재 집계 레벨이 새로운 가격 구간에서 유효하지 않으면 기본값으로 설정
            this.currentAggregationLevel = 0.0
            Log.d(TAG, "Price changed to $newPrice, resetting aggregation level to default")
            updateAggregationButtonText()
        }
        
        triggerManualUpdate()  // 즉시 반영
    }



    fun stopAutoUpdate() {
        Log.d(TAG, "Stopping auto update.")
        handler.removeCallbacks(updateRunnable)
    }

    fun triggerManualUpdate() {
        Log.d(TAG, "Manual update triggered.")
        updateOrderBook()
    }

    fun release() {
        Log.d(TAG, "Releasing resources.")
        stopAutoUpdate()
        if (recyclerView.adapter != null) {
            recyclerView.adapter = null
        }
    }

    // --- ▼▼▼ 수정된 메서드 (호가창 API) ▼▼▼ ---
    private fun updateOrderBook() {
        try {
            val currentPrice = getCurrentPrice()
            val currentPairId = getCurrentPairId()
            
            Log.d(TAG, "Updating order book with currentPrice: $currentPrice, pairId: $currentPairId, aggregation: $currentAggregationLevel")
            
            if (currentPairId == null) {
                Log.w(TAG, "현재 pairId가 null 빈 호가창 표시")
                updateOrderBookUI(emptyList(), currentPrice)
                return
            }
            
            // 새로운 호가창 API 사용
            val orderBookRepository = com.stip.stip.api.repository.OrderBookRepository()
            
            // 코루틴 스코프에서 API 호출
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val orderBookItems = orderBookRepository.getOrderBook(currentPairId, currentPrice)
                    
                    if (orderBookItems.isNotEmpty()) {
                        Log.d(TAG, "호가창 API 호출 성공: ${orderBookItems.size}개 아이템")
                        
                        // UI 업데이트는 메인 스레드에서 실행
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            updateOrderBookUI(orderBookItems, currentPrice)
                        }
                    } else {
                        Log.w(TAG, "호가창 API 응답이 비어잇음 빈 호가창 표시")
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            updateOrderBookUI(emptyList(), currentPrice)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "호가창 API 호출 실패", e)
                    // 실패 시 빈 호가창 표시
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        updateOrderBookUI(emptyList(), currentPrice)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order book", e)
            // 예외 발생 시 빈 호가창 표시
            val currentPrice = getCurrentPrice()
            updateOrderBookUI(emptyList(), currentPrice)
        }
    }

    private fun updateOrderBookUI(rawList: List<OrderBookItem>, currentPrice: Float) {
        val aggregatedList = if (this.currentAggregationLevel > 0.0) {
            generateAggregatedOrderBook(rawList, this.currentAggregationLevel)
        } else {
            rawList
        }

        val sortedSells = aggregatedList.filter { !it.isBuy && !it.isGap }.sortedByDescending { parseDouble(it.price) }
        val sortedBuys = aggregatedList.filter { it.isBuy && !it.isGap }.sortedByDescending { parseDouble(it.price) }

        // 시장가 주문 Radio 비활성화 로직
        // 매수 탭: 매도(asks)가 0개면 비활성화, 매도 탭: 매수(bids)가 0개면 비활성화 (터치해도 시장 주문으로 변하지 않음)
        val tabPosition = binding.tabLayoutOrderMode.selectedTabPosition
        val radioMarket = binding.radioMarketOrder
        if (tabPosition == 0) { // 매수 탭
            radioMarket.isEnabled = sortedSells.isNotEmpty()
        } else if (tabPosition == 1) { // 매도 탭
            radioMarket.isEnabled = sortedBuys.isNotEmpty()
        }

        // 간격 아이템 제거 - 매도와 매수를 직접 연결
        val FIXED_SELL_SIZE = 30
        val FIXED_BUY_SIZE = 30

        fun topPad(list: List<OrderBookItem>, total: Int, isBuy: Boolean): List<OrderBookItem> {
            val padSize = total - list.size
            return List(padSize) { OrderBookItem(isBuy = isBuy) } + list
        }
        fun bottomPad(list: List<OrderBookItem>, total: Int, isBuy: Boolean): List<OrderBookItem> {
            val padSize = total - list.size
            return list + List(padSize) { OrderBookItem(isBuy = isBuy) }
        }

        val paddedSells = topPad(sortedSells, FIXED_SELL_SIZE, false)
        val paddedBuys = bottomPad(sortedBuys, FIXED_BUY_SIZE, true)

        // 간격 없이 매도와 매수를 직접 연결
        val listWithoutGap = mutableListOf<OrderBookItem>().apply {
            addAll(paddedSells)
            addAll(paddedBuys)
        }

        // 호가창 데이터 업데이트 전에 강조 표시 초기화
        orderBookAdapter.resetHighlight()
        
        // 새로운 데이터로 업데이트
        orderBookAdapter.updateData(listWithoutGap, currentPrice)
    }

    private fun scrollToCenter() {
        recyclerView.post {
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return@post
            val adapter = recyclerView.adapter as? OrderBookAdapter ?: return@post
            val height = recyclerView.height
            if (height <= 0 || adapter.itemCount <= 0) {
                Log.w(TAG,"Cannot scroll, RV height=$height, itemCount=${adapter.itemCount}")
                return@post
            }

            val firstBuyIndex = adapter.getFirstBuyOrderIndex()
            Log.d(TAG, "Scrolling to center. Height=$height, Count=${adapter.itemCount}, FirstBuyIndex(GapIndex+1)=$firstBuyIndex")

            try {
                if (firstBuyIndex >= 0 && firstBuyIndex < adapter.itemCount) {
                    val offset = height / 2 - (recyclerView.findViewHolderForAdapterPosition(firstBuyIndex)?.itemView?.height ?: 30) / 2
                    layoutManager.scrollToPositionWithOffset(firstBuyIndex, offset)
                } else if (adapter.itemCount > 0) {
                    layoutManager.scrollToPositionWithOffset(adapter.itemCount / 2, 0)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during scrolling", e)
            }
        }
    }

    fun setupBottomOptionListeners() {
        Log.d(TAG, "Setting up bottom option listeners.")
        updateQuantityTotalTextView()
        updateAggregationButtonText()

        binding.optionViewAllContainer.setOnClickListener {
            val currentPrice = getCurrentPrice()
            val aggregationLevels = getAggregationLevelsForPrice(currentPrice)
            val currentIndex = aggregationLevels.indexOf(this.currentAggregationLevel)
            val nextIndex = (currentIndex + 1) % aggregationLevels.size
            this.currentAggregationLevel = aggregationLevels[nextIndex]
            Log.d(TAG, "Aggregation level changed to: ${this.currentAggregationLevel} for price: $currentPrice")
            updateAggregationButtonText()
            triggerManualUpdate()
        }

        binding.optionQuantityTotalToggleContainer.setOnClickListener {
            this.isTotalAmountMode = !this.isTotalAmountMode
            Log.d(TAG, "Total amount mode toggled: ${this.isTotalAmountMode}")
            updateQuantityTotalTextView()
            orderBookAdapter.setDisplayMode(this.isTotalAmountMode)
            triggerManualUpdate()
        }
    }

    private fun updateQuantityTotalTextView() {
        binding.textOptionQuantityTotalToggle.text = context.getString(
            if (this.isTotalAmountMode)
                R.string.total_amount_label
            else
                R.string.quantity_label
        )
    }

    private fun updateAggregationButtonText() {
        val currentPrice = getCurrentPrice()
        val levelText = getAggregationLevelDisplayText(this.currentAggregationLevel)

        binding.textOptionViewAll.text = if (this.currentAggregationLevel == 0.0) {
            context.getString(R.string.gather_the_price)
        } else {
            context.getString(R.string.gather_the_price) + " ($levelText)"
        }
    }

    /**
     * 현재가에 따른 집계 레벨 목록을 반환
     */
    private fun getAggregationLevelsForPrice(currentPrice: Float): List<Double> {
        val price = currentPrice.toDouble()
        
        return PRICE_AGGREGATION_MAP.entries.find { (range, _) ->
            price >= range.first && price < range.second
        }?.value ?: listOf(0.0, 0.001, 0.01, 0.1) // 기본값
    }

    /**
     * 집계 레벨에 대한 표시 텍스트를 반환
     */
    private fun getAggregationLevelDisplayText(level: Double): String {
        return when (level) {
            0.0 -> "기본"
            0.001 -> "0.001"
            0.01 -> "0.01"
            0.1 -> "0.1"
            1.0 -> "1"
            10.0 -> "10"
            100.0 -> "100"
            1000.0 -> "1,000"
            10000.0 -> "10,000"
            100000.0 -> "100,000"
            else -> String.format(Locale.US, "%.3f", level)
        }
    }

    // 더미 데이터 생성 함수 - 사용하지 않음
    /*
    fun generateDummyOrderBook(
        currentPrice: Float,
        fixedTwoDecimalFormatter: DecimalFormat,
        numberParseFormat: DecimalFormat
    ): List<OrderBookItem> {
        if (currentPrice <= 0f) return emptyList()
        val sellOrders = mutableListOf<OrderBookItem>()
        val buyOrders = mutableListOf<OrderBookItem>()
        val step = when {
            currentPrice < 1f -> 0.001f; currentPrice < 10f -> 0.01f
            currentPrice < 100f -> 0.1f; currentPrice < 1000f -> 0.5f
            else -> 1.0f
        }.coerceAtLeast(0.001f)
        val numOrdersPerSide = 30
        val random = Random()

        for (i in numOrdersPerSide downTo 1) {
            val price = currentPrice + i * step
            val quantity = random.nextDouble() * 50 + 10
            val percent = if (currentPrice > 0) ((price - currentPrice) / currentPrice) * 100 else 0.0
            sellOrders.add(
                OrderBookItem(
                    price = fixedTwoDecimalFormatter.format(price.toDouble()),
                    percent = String.format(Locale.US, "+%.2f%%", percent),
                    quantity = String.format("%.3f", quantity),
                    isBuy = false
                )
            )
        }
        for (i in 1..numOrdersPerSide) {
            val price = (currentPrice - i * step).coerceAtLeast(step)
            if (price <= 0) continue
            val quantity = random.nextDouble() * 40 + 8
            val percent = if (currentPrice > 0) ((price - currentPrice) / currentPrice) * 100 else 0.0
            buyOrders.add(
                OrderBookItem(
                    price = fixedTwoDecimalFormatter.format(price.toDouble()),
                    percent = String.format(Locale.US, "%.2f%%", percent),
                    quantity = String.format("%.3f", quantity),
                    isBuy = true
                )
            )
        }
        return sellOrders + buyOrders
    }
    */

    fun generateAggregatedOrderBook(
        baseData: List<OrderBookItem>,
        aggregationStep: Double
    ): List<OrderBookItem> {
        if (aggregationStep <= 0.0) return baseData

        fun parseDoubleLocal(value: String?): Double {
            return try {
                if (value.isNullOrBlank()) 0.0 else numberParseFormat.parse(value)?.toDouble() ?: 0.0
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse double in aggregate: $value", e)
                0.0
            }
        }

        /**
         * 호가를 지정된 간격으로 그룹화하는 함수
         * 예: 100.15원과 100.1원을 0.1 단위로 묶으면 100.1원 그룹으로 합쳐져서 수량이 합산됨
         * 예: 105.5원과 105원을 1 단위로 묶으면 105원 그룹으로 합쳐져서 수량이 합산됨
         */
        fun aggregateOrdersByPriceStep(orderList: List<OrderBookItem>, isBuy: Boolean): List<OrderBookItem> {
            // 매수/매도 및 갭이 아닌 실제 주문만 필터링
            val filteredOrders = orderList.filter { it.isBuy == isBuy && !it.isGap }
            
            Log.d(TAG, "Before aggregation (${if (isBuy) "Buy" else "Sell"}): ${filteredOrders.size} orders")
            
            val groupedOrders = filteredOrders.groupBy { orderItem ->
                val originalPrice = parseDoubleLocal(orderItem.price)
                
                // 부동소수점 정밀도 문제를 해결하기 위해 반올림을 사용
                // 1. 먼저 aggregationStep으로 나누고
                // 2. 0.5를 더한 후 floor를 취해서 반올림 효과
                // 3. 다시 aggregationStep을 곱함
                val ratio = originalPrice / aggregationStep
                val groupKey = floor(ratio + 0.0000001) * aggregationStep  // 미세한 오차 보정
                
                groupKey
            }
            
            
            // 그룹별로 수량 합산하여 새로운 OrderBookItem 생성
            val result = groupedOrders.mapNotNull { (groupPrice, ordersInGroup) ->
                // 유효하지 않은 가격은 제외
                if (groupPrice <= 0) return@mapNotNull null
                
                // 그룹 내 모든 주문의 수량 합산
                val individualQuantities = ordersInGroup.map { parseDoubleLocal(it.quantity) }
                val totalQuantity = individualQuantities.sum()
                
                Log.d(TAG, "Group $groupPrice: ${ordersInGroup.size} orders")
                ordersInGroup.forEachIndexed { index, order ->
                    Log.d(TAG, "  Order ${index + 1}: Price=${order.price}, Quantity=${order.quantity}")
                }
                Log.d(TAG, "  -> Total Quantity: $totalQuantity")
                
                // 수량이 0 이하인 그룹은 제외
                if (totalQuantity <= 0) return@mapNotNull null

                // 그룹화된 주문 아이템 생성
                OrderBookItem(
                    price = fixedTwoDecimalFormatter.format(groupPrice),
                    quantity = String.format("%.3f", totalQuantity),
                    isBuy = isBuy,
                    percent = ""
                )
            }.sortedByDescending { parseDoubleLocal(it.price) } // 가격 내림차순 정렬
            
            result.forEach { item ->
                Log.d(TAG, "  Final: ${item.price} - ${item.quantity}")
            }
            return result
        }

        // 매도 주문과 매수 주문을 각각 그룹화
        val aggregatedSells = aggregateOrdersByPriceStep(baseData, false)
        val aggregatedBuys = aggregateOrdersByPriceStep(baseData, true)

        
        return aggregatedSells + aggregatedBuys
    }

    fun initializeAndStart() {
        Log.d(TAG, "Initializing Order Book...")
        setupRecyclerView()
        
        // 초기 집계 레벨을 현재가에 맞게 설정
        val currentPrice = getCurrentPrice()
        val initialAggregationLevels = getAggregationLevelsForPrice(currentPrice)
        if (!initialAggregationLevels.contains(this.currentAggregationLevel)) {
            this.currentAggregationLevel = 0.0
        }
        
        updateOrderBook()
        recyclerView.post { scrollToCenter() }
    }

    private fun parseDouble(value: String?): Double {
        return try {
            if (value.isNullOrBlank()) 0.0 else numberParseFormat.parse(value)?.toDouble() ?: 0.0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse double for sorting: '$value'")
            0.0
        }
    }
}