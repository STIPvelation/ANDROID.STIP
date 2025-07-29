package com.stip.stip.order.coordinator

import com.stip.stip.iphome.TradingDataHolder
import com.stip.api.repository.PortfolioRepository
import com.stip.stip.signup.utils.PreferenceUtil
import android.util.Log // Log import for debugging output
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrderDataCoordinator(
    initialTicker: String?,
    private val feeRate: Double = 0.001
) {

    var currentTicker: String? = initialTicker
        private set

    var currentPrice: Float = 0.0f
        private set

    var availableUsdBalance: Double = 0.0
        private set

    var heldAssetQuantity: Double = 0.0
        private set

    var heldAssetEvalAmount: Double = 0.0
        private set

    val userHoldsAsset: Boolean
        get() = heldAssetQuantity > 0.0

    private val portfolioRepository = PortfolioRepository()
    
    // 잔액 업데이트 콜백
    private var onBalanceUpdated: (() -> Unit)? = null

    init {
        loadDataAndSetInitialState()
    }

    fun updateTicker(newTicker: String?) {
        this.currentTicker = newTicker
        loadDataAndSetInitialState()
    }

    private fun loadDataAndSetInitialState() {
        // 현재 가격 설정
        val marketItem = TradingDataHolder.ipListingItems.firstOrNull { it.ticker == currentTicker }
        currentPrice = marketItem?.currentPrice?.replace(",", "")?.toFloatOrNull() ?: 0f
        
        // API에서 실제 잔액 데이터 로드
        loadBalanceFromApi()
        
        Log.d("DataCoordinator", "Data loaded: Ticker=$currentTicker, Price=$currentPrice")
    }

    private fun loadBalanceFromApi() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = PreferenceUtil.getUserId()
                if (userId.isNullOrBlank()) {
                    Log.w("DataCoordinator", "사용자 ID가 없습니다.")
                    return@launch
                }

                val portfolioResponse = portfolioRepository.getPortfolioResponse(userId)
                if (portfolioResponse != null) {
                    // USD 사용 가능 잔액 설정
                    availableUsdBalance = portfolioResponse.usdAvailableBalance.toDouble()
                    
                    // 현재 티커에 해당하는 보유 자산 정보 설정
                    val currentAsset = portfolioResponse.wallets.find { it.symbol == currentTicker }
                    heldAssetQuantity = currentAsset?.availableBalance?.toDouble() ?: 0.0
                    heldAssetEvalAmount = currentAsset?.evalAmount?.toDouble() ?: 0.0
                    
                    Log.d("DataCoordinator", "API에서 잔액 로드 완료: USD=$availableUsdBalance, ${currentTicker}_available=$heldAssetQuantity, evalAmount=$heldAssetEvalAmount")
                    
                    // UI 업데이트 콜백 호출
                    onBalanceUpdated?.invoke()
                } else {
                    Log.w("DataCoordinator", "포트폴리오 API 응답이 null입니다.")
                }
            } catch (e: Exception) {
                Log.e("DataCoordinator", "API에서 잔액 로드 실패: ${e.message}", e)
            }
        }
    }

    fun updateCurrentPrice(newPrice: Float) {
        if (newPrice >= 0f) {
            this.currentPrice = newPrice
        }
    }

    fun adjustHoldingsAfterOrder(isBuy: Boolean, quantity: Double, price: Double?) {
        Log.d("DataCoordinator","adjustHoldingsAfterOrder: Before - USD_Balance=$availableUsdBalance, Available_Holdings=$heldAssetQuantity")
        val executionPrice = price ?: currentPrice.toDouble()

        if (executionPrice <= 0) {
            Log.e("DataCoordinator", "Error: Invalid execution price ($executionPrice).")
            return
        }

        val orderValue = quantity * executionPrice
//        val fee = orderValue * feeRate 수수료 계산

        if (isBuy) {
//            val totalCost = orderValue + fee  수수료 계산하면 이거 사용
            val totalCost = orderValue
            if (totalCost <= availableUsdBalance) {
                availableUsdBalance -= totalCost
                heldAssetQuantity += quantity
            }
        } else {
            if (quantity <= heldAssetQuantity) {
//                val totalReceived = orderValue - fee  수수료 계산하면 이거 사용
                val totalReceived = orderValue
                availableUsdBalance += totalReceived
                heldAssetQuantity -= quantity
            }
        }
        Log.d("DataCoordinator","adjustHoldingsAfterOrder: After - USD_Balance=$availableUsdBalance, Available_Holdings=$heldAssetQuantity")
        
        // 주문 후 잔액 업데이트 콜백 호출
        onBalanceUpdated?.invoke()
    }

    fun getActualBuyableAmount(): Double {
        return availableUsdBalance.coerceAtLeast(0.0)
    }

    fun getActualSellableQuantity(): Double {
        return heldAssetQuantity.coerceAtLeast(0.0)
    }

    fun getActualSellableEvalAmount(): Double {
        return heldAssetEvalAmount.coerceAtLeast(0.0)
    }

    fun getAverageBuyPrice(): Double {
        return currentPrice.toDouble()
    }

    /**
     * 잔액을 API에서 새로고침
     */
    fun refreshBalance() {
        loadBalanceFromApi()
    }
    
    /**
     * 강제로 잔액 업데이트 (즉시 콜백 호출)
     */
    fun forceUpdateBalance() {
        Log.d("DataCoordinator", "강제 잔액 업데이트 요청")
        onBalanceUpdated?.invoke()
    }
    
    /**
     * 잔액 업데이트 콜백 설정
     */
    fun setOnBalanceUpdated(callback: () -> Unit) {
        onBalanceUpdated = callback
    }
}