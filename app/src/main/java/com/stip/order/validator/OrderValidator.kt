package com.stip.stip.order

import android.content.Context
import com.stip.stip.R
import com.stip.stip.databinding.FragmentOrderContentBinding
import java.text.DecimalFormat
import android.util.Log

data class OrderParams(
    val limitPriceStr: String?,
    val quantityOrTotalStr: String?,
    val triggerPriceStr: String?,
    val isMarketOrder: Boolean,
    val isReservedOrder: Boolean,
    val isInputModeTotalAmount: Boolean
)

class OrderValidator(
    private val context: Context,
    private val binding: FragmentOrderContentBinding,
    private val getCurrentPrice: () -> Float,
    private val getCurrentTicker: () -> String?,
    private val availableUsdBalance: () -> Double,
    private val heldAssetQuantity: () -> Double,
    private val feeRate: Double,
    private val minimumOrderValue: Double,
    private val numberParseFormat: DecimalFormat,
    private val fixedTwoDecimalFormatter: DecimalFormat,
    private val showToast: (String) -> Unit,
    private val showErrorDialog: (titleResId: Int, message: String, colorResId: Int) -> Unit,
    private val getCurrentPairId: () -> String? = { null }
) {
    companion object {
        private const val TAG = "OrderValidator"
    }
    
    /**
     * 최신 시장가를 조회하는 메서드
     */
    private fun getLatestMarketPrice(): Double? {
        return try {
            val pairId = getCurrentPairId() ?: return null
            val marketService = com.stip.stip.api.RetrofitClient.createTapiService(com.stip.stip.api.service.MarketService::class.java)
            
            // 동기 호출을 위해 runBlocking 사용
            kotlinx.coroutines.runBlocking {
                try {
                    val marketResponse = marketService.getMarket(pairId)
                    val latestPrice = marketResponse.lastPrice?.toDouble()
                    Log.d(TAG, "검증용 최신 시장가 조회: $latestPrice")
                    latestPrice
                } catch (e: Exception) {
                    Log.w(TAG, "검증용 최신 시장가 조회 실패: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "검증용 최신 시장가 조회 중 예외: ${e.message}")
            null
        }
    }
    
    /**
     * 시장가 매도 시 실제 호가창 기준 예상 체결가 계산
     * @param sellQuantity 매도할 수량
     * @return 예상 가중평균 체결가 (null이면 현재가 사용)
     */
    private fun getExpectedMarketSellPrice(sellQuantity: Double?): Double? {
        if (sellQuantity == null || sellQuantity <= 0) return null
        
        return try {
            val pairId = getCurrentPairId() ?: return null
            val orderBookRepository = com.stip.stip.api.repository.OrderBookRepository()
            
            kotlinx.coroutines.runBlocking {
                try {
                    // 현재가 (변동률 계산용)
                    val currentPrice = getCurrentPrice()
                    
                    // 매수 호가 가져오기 (높은 가격 순으로 정렬됨)
                    val buyOrders = orderBookRepository.getBuyOrders(pairId, currentPrice)
                    
                    if (buyOrders.isEmpty()) {
                        Log.w(TAG, "검증: 호가창에 매수 호가가 없음")
                        return@runBlocking null
                    }
                    
                    // 수량만큼 체결했을 때의 가중평균 가격 계산
                    var remainingQuantity = sellQuantity
                    var totalValue = 0.0
                    var totalQuantity = 0.0
                    
                    for (order in buyOrders) {
                        if (remainingQuantity <= 0) break
                        
                        val orderPrice = order.price.toDouble()
                        val orderQuantity = order.quantity.toDouble()
                        val fillQuantity = minOf(remainingQuantity, orderQuantity)
                        
                        totalValue += fillQuantity * orderPrice
                        totalQuantity += fillQuantity
                        remainingQuantity -= fillQuantity
                        
                        Log.d(TAG, "검증 호가 체결: ${orderPrice} × ${fillQuantity} = ${fillQuantity * orderPrice}")
                    }
                    
                    val weightedAveragePrice = if (totalQuantity > 0) totalValue / totalQuantity else null
                    
                    if (remainingQuantity > 0) {
                        Log.w(TAG, "검증: 호가창 유동성 부족, ${remainingQuantity}개 미체결 예상")
                        // 유동성 부족 시 최소 주문금액 미달로 처리
                        return@runBlocking null
                    }
                    
                    Log.d(TAG, "검증용 시장가 매도 예상 체결가: $weightedAveragePrice (총 수량: $totalQuantity)")
                    weightedAveragePrice
                    
                } catch (e: Exception) {
                    Log.e(TAG, "검증용 호가창 기반 예상 체결가 계산 실패: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "검증용 예상 체결가 계산 중 예외: ${e.message}")
            null
        }
    }

    fun validateOrder(params: OrderParams, isBuyOrder: Boolean): Boolean {
        val parsedInputs = parseAndValidateBasicInputs(params) ?: return false
        val limitPrice: Double? = parsedInputs.first
        val quantityOrTotal: Double = parsedInputs.second
        val triggerPrice: Double? = parsedInputs.third

        var quantity: Double? = null
        var grossTotalValue: Double? = null

        if (params.isMarketOrder && isBuyOrder) {
            grossTotalValue = quantityOrTotal
            quantity = null
        } else if (params.isInputModeTotalAmount && !params.isMarketOrder) {
            grossTotalValue = quantityOrTotal
            if (limitPrice != null && limitPrice > 0) {
                quantity = grossTotalValue / limitPrice
            } else {
                Log.e(TAG, "Validation failed in quantity calc from total: Invalid price.")
                showErrorDialog(
                    R.string.dialog_title_error_order,
                    context.getString(R.string.toast_invalid_price_for_total),
                    R.color.dialog_title_error_generic
                )
                return false
            }
        } else {
            quantity = quantityOrTotal
            if (!params.isMarketOrder && limitPrice != null) {
                grossTotalValue = limitPrice * quantity
            } else if (params.isMarketOrder && !isBuyOrder) {
                grossTotalValue = quantity * getCurrentPrice().toDouble()
            }
        }

        if (!validateOrderRules(
                limitPrice,
                quantity,
                grossTotalValue,
                triggerPrice,
                isBuyOrder,
                params.isMarketOrder,
                params.isReservedOrder
            )
        ) {
            return false
        }

        if (!validateAvailability(quantity, grossTotalValue, isBuyOrder, params.isMarketOrder)) {
            return false
        }

        return true
    }

    private fun parseAndValidateBasicInputs(params: OrderParams): Triple<Double?, Double, Double?>? {
        var price: Double? = null
        var quantityOrTotal: Double?
        var triggerPrice: Double? = null

        if (!params.isMarketOrder) {
            price = try {
                numberParseFormat.parse(params.limitPriceStr ?: "")?.toDouble()
            } catch (e: Exception) {
                null
            }
            if (price == null || price <= 0.0) {
                Log.w(TAG, "Validation failed: Invalid limit price.")
                showToast(context.getString(R.string.toast_invalid_price))
                return null
            }
        }

        quantityOrTotal = try {
            numberParseFormat.parse(params.quantityOrTotalStr ?: "")?.toDouble()
        } catch (e: Exception) {
            null
        }
        if (quantityOrTotal == null || quantityOrTotal <= 0.0) {
            Log.w(TAG, "Validation failed: Invalid quantity/total input.")
            val messageResId =
                if (params.isInputModeTotalAmount) R.string.toast_enter_total_amount else R.string.toast_enter_quantity
            showToast(context.getString(messageResId))
            return null
        }

        if (params.isReservedOrder) {
            triggerPrice = try {
                numberParseFormat.parse(params.triggerPriceStr ?: "")?.toDouble()
            } catch (e: Exception) {
                null
            }
            if (triggerPrice == null || triggerPrice <= 0.0) {
                Log.w(TAG, "Validation failed: Invalid trigger price.")
                showToast(context.getString(R.string.toast_invalid_trigger_price))
                return null
            }
        }

        return Triple(price, quantityOrTotal, triggerPrice)
    }


    private fun validateOrderRules(
        price: Double?,
        quantity: Double?,
        grossValue: Double?,
        triggerPrice: Double?,
        isBuyOrder: Boolean,
        isMarketOrder: Boolean,
        isReservedOrder: Boolean
    ): Boolean {
        val minOrderDisplayValue = fixedTwoDecimalFormatter.format(minimumOrderValue)
        val minOrderMessage =
            context.getString(R.string.toast_minimum_order_violation, minOrderDisplayValue)

        val valueToCheck = when {
            isMarketOrder && isBuyOrder -> grossValue
            isMarketOrder && !isBuyOrder -> {
                quantity?.let { 
                    // 시장가 매도일 때 실제 호가창 기준 예상 체결가 사용
                    val expectedPrice = getExpectedMarketSellPrice(it) ?: getCurrentPrice().toDouble()
                    Log.d(TAG, "시장가 매도 검증: quantity=$it, expectedPrice=$expectedPrice, 총액=${it * expectedPrice}")
                    it * expectedPrice
                }
            }
            else -> grossValue
        }

        if (valueToCheck != null && valueToCheck < minimumOrderValue) {
            Log.w(TAG, "Validation failed: Minimum order value not met.")
            showErrorDialog(
                R.string.dialog_title_warning_order,
                minOrderMessage,
                R.color.dialog_title_error_generic
            )
            return false
        }

        if (isReservedOrder && price != null && triggerPrice != null) {
            if (isBuyOrder && triggerPrice < price) {
                Log.w(TAG, "Validation failed: Buy reserved trigger price condition.")
                showErrorDialog(
                    R.string.reserved_order_error_title,
                    context.getString(R.string.buy_trigger_price_error),
                    R.color.dialog_title_buy_error_red
                )
                return false
            } else if (!isBuyOrder && triggerPrice > price) {
                Log.w(TAG, "Validation failed: Sell reserved trigger price condition.")
                showErrorDialog(
                    R.string.reserved_order_error_title,
                    context.getString(R.string.sell_trigger_price_error),
                    R.color.dialog_title_sell_error_blue
                )
                return false
            }
        }
        return true
    }


    private fun validateAvailability(
        quantity: Double?,
        grossValue: Double?,
        isBuyOrder: Boolean,
        isMarketOrder: Boolean
    ): Boolean {
        val epsilon = 0.00000001

        if (isBuyOrder) {
            val requiredCost = when {
                isMarketOrder -> grossValue ?: 0.0
//                grossValue != null -> grossValue * (1.0 + feeRate)    수수료 계산하면 이거 사용
                grossValue != null -> grossValue // 수수료 없음
                else -> 0.0
            }
            val available = availableUsdBalance()

            Log.d(TAG, "Buy Check - Required: $requiredCost, Available: $available") // 값 확인 로그

            if (requiredCost > available + epsilon) {
                Log.w(
                    TAG,
                    "Validation failed: Insufficient funds. SHOWING DIALOG..."
                ) // <<<--- 로그 추가
                showErrorDialog(
                    R.string.dialog_title_error_order,
                    context.getString(R.string.buy_error_insufficient_funds),
                    R.color.dialog_title_buy_error_red
                )
                return false
            }
        } else {
            if (quantity == null || quantity <= 0.0) {
                Log.w(TAG, "Validation failed: Invalid quantity for sell.")
                showToast(context.getString(R.string.toast_enter_quantity))
                return false
            }
            val held = heldAssetQuantity()
            Log.d(TAG, "Sell Check - Required Qty: $quantity, Held Qty: $held") // 값 확인 로그

            if (quantity > held + epsilon) {
                Log.w(
                    TAG,
                    "Validation failed: Insufficient quantity. SHOWING DIALOG..."
                ) // <<<--- 로그 추가
                showErrorDialog(
                    R.string.dialog_title_error_order,
                    context.getString(R.string.sell_error_insufficient_quantity),
                    R.color.dialog_title_sell_error_blue
                )
                return false
            }
        }
        return true
    }
}