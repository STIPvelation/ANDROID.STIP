package com.stip.stip.order.button

import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.FragmentManager
import com.stip.stip.R
import com.stip.stip.databinding.FragmentOrderContentBinding
import java.text.DecimalFormat
import com.stip.stip.iphome.fragment.ConfirmOrderDialogFragment
import android.util.Log
import com.stip.stip.api.RetrofitClient
import com.stip.stip.signup.login.LoginActivity
import com.stip.stip.signup.utils.PreferenceUtil
import com.stip.stip.signup.Constants
import com.stip.order.data.OrderRequest
import com.stip.stip.order.OrderParams
import com.stip.order.api.OrderService
import com.stip.stip.order.OrderValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.stip.stip.api.repository.IpListingRepository

class OrderButtonHandler(
    private val context: Context,
    private val binding: FragmentOrderContentBinding,
    private val validator: OrderValidator,
    private val numberParseFormat: DecimalFormat,
    private val fixedTwoDecimalFormatter: DecimalFormat,
    private val getCurrentPrice: () -> Float,
    private val getFeeRate: () -> Double,
    private val currentTicker: () -> String?,
    private val minimumOrderValue: Double,
    private val availableUsdBalance: () -> Double,
    private val heldAssetQuantity: () -> Double,
    private val showToast: (String) -> Unit,
    private val showErrorDialog: (titleResId: Int, message: String, colorResId: Int) -> Unit,
    private val parentFragmentManager: FragmentManager,
    private val getCurrentPairId: () -> String?,
    private val orderDataCoordinator: com.stip.stip.order.coordinator.OrderDataCoordinator? = null,
    private val orderInputHandler: com.stip.stip.order.OrderInputHandler? = null,
    private val onOrderSuccess: (() -> Unit)? = null
) {
    companion object {
        private const val TAG = "OrderButtonHandler"
    }
    
    /**
     * 최신 시장가를 동기적으로 가져오는 메서드
     * 주문 확인 다이얼로그에서 사용
     */
    private fun getLatestMarketPrice(): Double? {
        return try {
            val pairId = getCurrentPairId() ?: return null
            val marketService = RetrofitClient.createTapiService(com.stip.stip.api.service.MarketService::class.java)
            
            // 동기 호출을 위해 runBlocking 사용 (UI가 아닌 백그라운드에서만)
            kotlinx.coroutines.runBlocking {
                try {
                    val marketResponse = marketService.getMarket(pairId)
                    val latestPrice = marketResponse.lastPrice?.toDouble()
                    Log.d(TAG, "최신 시장가 조회: $latestPrice")
                    latestPrice
                } catch (e: Exception) {
                    Log.w(TAG, "최신 시장가 조회 실패: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "최신 시장가 조회 중 예외: ${e.message}")
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
                        Log.w(TAG, "호가창에 매수 호가가 없음")
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
                        
                        Log.d(TAG, "호가 체결: ${orderPrice} × ${fillQuantity} = ${fillQuantity * orderPrice}")
                    }
                    
                    val weightedAveragePrice = if (totalQuantity > 0) totalValue / totalQuantity else null
                    
                    if (remainingQuantity > 0) {
                        Log.w(TAG, "호가창 유동성 부족: ${remainingQuantity}개 미체결 예상")
                    }
                    
                    Log.d(TAG, "시장가 매도 예상 체결가: $weightedAveragePrice (총 수량: $totalQuantity)")
                    weightedAveragePrice
                    
                } catch (e: Exception) {
                    Log.e(TAG, "호가창 기반 예상 체결가 계산 실패: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "예상 체결가 계산 중 예외: ${e.message}")
            null
        }
    }

    private val orderService = RetrofitClient.createOrderService()

    init {
        setupInputListeners()
    }

    private fun setupInputListeners() {
        // 가격 입력 리스너
        binding.editTextLimitPrice.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateTotalAmount()
            }
        })

        // 수량 입력 리스너
        binding.editTextQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateTotalAmount()
            }
        })

        // 주문 유형 변경 리스너
        binding.radioGroupOrderType.setOnCheckedChangeListener { _, _ ->
            updateTotalAmount()
        }

        // 매수/매도 탭 변경 리스너
        binding.tabLayoutOrderMode.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                updateTotalAmount()
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun updateTotalAmount() {
        try {
            val selectedOrderTypeId = binding.radioGroupOrderType.checkedRadioButtonId
            val isMarketOrder = selectedOrderTypeId == R.id.radio_market_order
            
            val quantityStr = binding.editTextQuantity.text?.toString()
            val quantity = numberParseFormat.parse(quantityStr ?: "0")?.toDouble() ?: 0.0
            
            val price = if (isMarketOrder) {
                getCurrentPrice().toDouble()
            } else {
                val priceStr = binding.editTextLimitPrice.text?.toString()
                numberParseFormat.parse(priceStr ?: "0")?.toDouble() ?: 0.0
            }
            
            val feeRate = getFeeRate()
            val grossAmount = price * quantity
            val feeAmount = grossAmount * feeRate
            
            // 매수일 때는 수수료를 더하고, 매도일 때는 수수료를 뺌
            val isBuyOrder = binding.tabLayoutOrderMode.selectedTabPosition == 0
            val totalAmount = if (isBuyOrder) {
                grossAmount * (1.0 + feeRate)
            } else {
                grossAmount * (1.0 - feeRate)
            }
            
            // 총액 표시 업데이트
            binding.textCalculatedTotal.setText(if (totalAmount > 0) {
                "${fixedTwoDecimalFormatter.format(totalAmount)} USD"
            } else {
                "0 USD"
            })
            
        } catch (e: Exception) {
            Log.e(TAG, "총액 계산 중 오류 발생", e)
            binding.textCalculatedTotal.setText("0 USD")
        }
    }

    fun setupOrderButtonClickListeners() {
        // 버튼 클릭 리스너 - 현재 탭에 따라 매수/매도 구분
        binding.buttonBuy.setOnClickListener {
            val isBuyOrder = binding.tabLayoutOrderMode.selectedTabPosition == 0
            handleButtonClick(isBuyOrder)
        }
    }
    
    /**
     * 주문 버튼 상태 업데이트
     */
    fun updateOrderButtonStates() {
        try {
            val selectedTab = binding.tabLayoutOrderMode.selectedTabPosition
            val isLoggedIn = PreferenceUtil.isRealLoggedIn()
            
            if (!isLoggedIn) {
                // 로그인하지 않은 경우 버튼 비활성화
                binding.buttonBuy.isEnabled = false
                return
            }
            
            // 로그인한 경우 버튼 활성화
            binding.buttonBuy.isEnabled = true
            
            // 탭에 따라 버튼 텍스트와 색상 설정
            when (selectedTab) {
                0 -> { // 매수 탭
                    binding.buttonBuy.text = context.getString(R.string.button_buy)
                    binding.buttonBuy.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.percentage_positive_red))
                }
                1 -> { // 매도 탭
                    binding.buttonBuy.text = context.getString(R.string.button_sell)
                    binding.buttonBuy.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.percentage_negative_blue))
                }
                2 -> { // 내역 탭
                    binding.buttonBuy.isEnabled = false
                    binding.buttonBuy.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.darker_gray))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "탭 업데이트 에러", e)
        }
    }

    private fun handleButtonClick(isBuyOrder: Boolean) {
        // 로그인 상태 확인
        val isLoggedIn = PreferenceUtil.isRealLoggedIn()
        
        val token = PreferenceUtil.getToken()
        val isGuest = PreferenceUtil.isGuestMode()
        Log.d(TAG, "로그인 상태: token=${token != null}, isGuest=$isGuest, isRealLoggedIn=$isLoggedIn")
        
        if (!isLoggedIn) {
            // 비로그인 상태일 때 로그인 화면으로 이동
            val intent = Intent(context, LoginActivity::class.java)
            context.startActivity(intent)
        } else {
            // 로그인 상태일 때 기존과 동일하게 주문 처리
            gatherInputsAndValidate(isBuyOrder)
        }
    }

    private fun gatherInputsAndValidate(isBuyOrder: Boolean) {
        val selectedOrderTypeId = binding.radioGroupOrderType.checkedRadioButtonId
        val isMarketOrder = selectedOrderTypeId == R.id.radio_market_order
        val isReservedOrder = selectedOrderTypeId == R.id.radio_reserved_order
        
        val quantityOrTotalStr = binding.editTextQuantity.text?.toString()

        val orderParams = OrderParams(
            limitPriceStr = binding.editTextLimitPrice.text?.toString(),
            quantityOrTotalStr = quantityOrTotalStr,
            triggerPriceStr = binding.editTextTriggerPrice?.text?.toString(),
            isMarketOrder = isMarketOrder,
            isReservedOrder = isReservedOrder,
            isInputModeTotalAmount = isMarketOrder && isBuyOrder
        )

        if (validator.validateOrder(orderParams, isBuyOrder)) {
            prepareAndShowConfirmationDialog(orderParams, isBuyOrder)
        }
    }

    private fun prepareAndShowConfirmationDialog(params: OrderParams, isBuyOrder: Boolean) {
        var price: Double? = null
        var quantity: Double? = null
        var triggerPrice: Double? = null
        var quantityOrTotalInput: Double = 0.0

        var orderTypeText: String
        var priceConfirmStr: String
        var quantityConfirmStr: String
        var displayTotalValueStr: String
        var calculatedFee: Double
        var feeConfirmStr: String
        var triggerPriceConfirmStr: String? = null

        try {
            quantityOrTotalInput = numberParseFormat.parse(params.quantityOrTotalStr ?: "0")?.toDouble() ?: 0.0
            
            if (!params.isMarketOrder) {
                price = numberParseFormat.parse(params.limitPriceStr ?: "0")?.toDouble() ?: 0.0
            }
            if (params.isReservedOrder) {
                triggerPrice = numberParseFormat.parse(params.triggerPriceStr ?: "0")?.toDouble() ?: 0.0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing values for confirmation dialog", e)
            showErrorDialog(R.string.dialog_title_error_order, "주문 정보를 처리하는 중 오류가 발생했습니다.", R.color.dialog_title_error_generic)
            return
        }

        var grossTotalValue: Double?
        if (params.isMarketOrder && isBuyOrder) {
            grossTotalValue = quantityOrTotalInput
            quantity = quantityOrTotalInput
            quantityConfirmStr = "--"
        } else {
            quantity = quantityOrTotalInput
            quantityConfirmStr = fixedTwoDecimalFormatter.format(quantity)
            grossTotalValue = if (!params.isMarketOrder && price != null) {
                price * quantity
            } else if (params.isMarketOrder && !isBuyOrder) {
                // 시장가 매도일 때 실제 호가창 매수 호가 사용
                val expectedPrice = getExpectedMarketSellPrice(quantity) ?: getCurrentPrice().toDouble()
                Log.d(TAG, "시장가 매도 예상 체결가: $expectedPrice (수량: $quantity)")
                quantity * expectedPrice
            } else {
                null
            }
        }

        // 시장가 주문일 때 실제 예상 체결가 표시
        priceConfirmStr = if (price != null) {
            fixedTwoDecimalFormatter.format(price)
        } else if (params.isMarketOrder) {
            if (!isBuyOrder) {
                // 시장가 매도: 실제 호가창 기준 예상 체결가
                val expectedPrice = getExpectedMarketSellPrice(quantity)
                if (expectedPrice != null) {
                    "${fixedTwoDecimalFormatter.format(expectedPrice)} (예상 체결가)"
                } else {
                    "${fixedTwoDecimalFormatter.format(getCurrentPrice())} (시장가)"
                }
            } else {
                // 시장가 매수: 최신 현재가 사용
                val latestPrice = getLatestMarketPrice()
                if (latestPrice != null) {
                    "${fixedTwoDecimalFormatter.format(latestPrice)} (최신 시장가)"
                } else {
                    "${fixedTwoDecimalFormatter.format(getCurrentPrice())} (시장가)"
                }
            }
        } else {
            context.getString(R.string.market_price)
        }
        triggerPriceConfirmStr = triggerPrice?.let { fixedTwoDecimalFormatter.format(it) }

        orderTypeText = when {
            params.isReservedOrder && isBuyOrder -> context.getString(R.string.order_type_reserved_limit_buy)
            params.isReservedOrder && !isBuyOrder -> context.getString(R.string.order_type_reserved_limit_sell)
            params.isMarketOrder && isBuyOrder -> context.getString(R.string.order_type_market_buy)
            params.isMarketOrder && !isBuyOrder -> context.getString(R.string.order_type_market_sell)
            isBuyOrder -> context.getString(R.string.order_type_limit_buy)
            else -> context.getString(R.string.order_type_limit_sell)
        }

        val feeRate = getFeeRate()
        if (params.isMarketOrder && isBuyOrder) {
            calculatedFee = (grossTotalValue ?: 0.0) * feeRate / (1.0 + feeRate)
            displayTotalValueStr = fixedTwoDecimalFormatter.format(grossTotalValue ?: 0.0)
        } else if (params.isMarketOrder && !isBuyOrder) {
            calculatedFee = (grossTotalValue ?: 0.0) * feeRate
            displayTotalValueStr = context.getString(R.string.market_total)
        } else if (price != null && quantity != null){
            val calculatedGross = price * quantity
            calculatedFee = calculatedGross * feeRate
            val finalDisplayAmount = if (isBuyOrder) calculatedGross * (1.0 + feeRate) else calculatedGross * (1.0 - feeRate)
            displayTotalValueStr = fixedTwoDecimalFormatter.format(finalDisplayAmount)
        } else {
            calculatedFee = 0.0
            displayTotalValueStr = "--"
        }
        feeConfirmStr = fixedTwoDecimalFormatter.format(calculatedFee)

        val dialog = ConfirmOrderDialogFragment.newInstance(
            isBuyOrder = isBuyOrder,
            tickerFull = "${currentTicker() ?: "N/A"}/USD",
            orderType = orderTypeText,
            priceValue = priceConfirmStr,
            quantityValue = quantityConfirmStr,
            totalValue = displayTotalValueStr,
            feeValue = feeConfirmStr,
            triggerPriceValue = triggerPriceConfirmStr
        )

        parentFragmentManager.setFragmentResultListener(
            ConfirmOrderDialogFragment.REQUEST_KEY,
            context as androidx.lifecycle.LifecycleOwner
        ) { _, result ->
            val confirmed = result.getBoolean(ConfirmOrderDialogFragment.RESULT_KEY_CONFIRMED, false)
            if (confirmed) {
                val resultIsBuy = result.getBoolean(ConfirmOrderDialogFragment.RESULT_KEY_IS_BUY, false)
                val resultQuantity = result.getDouble(ConfirmOrderDialogFragment.RESULT_KEY_QUANTITY, 0.0)
                val resultPrice = result.getDouble(ConfirmOrderDialogFragment.RESULT_KEY_PRICE, 0.0)
                
                Log.d(TAG, "Order confirmed - isBuy: $resultIsBuy, quantity: $resultQuantity, price: $resultPrice")
                executeOrder(resultIsBuy, resultPrice, resultQuantity)
            }
        }

        dialog.show(parentFragmentManager, ConfirmOrderDialogFragment.TAG)
    }

    private fun executeOrder(isBuyOrder: Boolean, price: Double, quantity: Double) {
        val userId = PreferenceUtil.getUserId() ?: run {
            val orderType = if (isBuyOrder) "매수" else "매도"
            Log.e(TAG, "$orderType 실패: userId is null")
            showErrorDialog(
                R.string.dialog_title_error_order,
                "사용자 정보를 찾을 수 없습니다.",
                R.color.red
            )
            return
        }

        val pairId = getCurrentPairId() ?: run {
            val orderType = if (isBuyOrder) "매수" else "매도"
            Log.e(TAG, "$orderType 실패: pairId is null")
            showErrorDialog(
                R.string.dialog_title_error_order,
                "선택된 IP 정보를 찾을 수 없습니다.",
                R.color.red
            )
            return
        }

        // 시장가 주문인지 확인
        val selectedOrderTypeId = binding.radioGroupOrderType.checkedRadioButtonId
        val isMarketOrder = selectedOrderTypeId == R.id.radio_market_order
        
        val orderType = if (isBuyOrder) "매수" else "매도"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 시장가 주문일 때 최신 현재가 조회
                val finalPrice = if (isMarketOrder) {
                    val marketService = RetrofitClient.createTapiService(com.stip.stip.api.service.MarketService::class.java)
                    try {
                        Log.d(TAG, "시장가 주문: 최신 현재가 조회 중... pairId=$pairId")
                        val marketResponse = marketService.getMarket(pairId)
                        val latestPrice = marketResponse.lastPrice?.toDouble() ?: getCurrentPrice().toDouble()
                        
                        Log.d(TAG, "최신 현재가 조회 완료: $latestPrice (기존: ${getCurrentPrice()})")
                        
                        latestPrice
                    } catch (e: Exception) {
                        Log.w(TAG, "최신 현재가 조회 실패, 기존 가격 사용: ${e.message}")
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            showToast("현재가 조회 실패, 기존 가격으로 주문 진행")
                        }
                        getCurrentPrice().toDouble()
                    }
                } else {
                    price // 지정가 주문은 입력된 가격 사용
                }
                
                Log.d(TAG, "주문 실행: ${orderType}, isMarket=$isMarketOrder, price=${if (isMarketOrder) finalPrice else price}")
                
                val response = if (isMarketOrder) {
                    if (isBuyOrder) {
                        // 시장가 매수 API 사용
                        val marketBuyRequest = com.stip.order.data.MarketBuyRequest(
                            pairId = pairId,
                            amount = quantity
                        )
                        Log.d(TAG, "시장가 $orderType 주문 요청: $marketBuyRequest")
                        orderService.marketBuyOrder(marketBuyRequest)
                    } else {
                        // 시장가 매도 API 사용
                        val marketSellRequest = com.stip.order.data.MarketSellRequest(
                            pairId = pairId,
                            quantity = quantity
                        )
                        Log.d(TAG, "시장가 $orderType 주문 요청: $marketSellRequest")
                        orderService.marketSellOrder(marketSellRequest)
                    }
                } else {
                    // 지정가 주문
                    val orderRequest = OrderRequest(
                        pairId = pairId,
                        quantity = quantity,
                        price = price
                    )
                    Log.d(TAG, "지정가 $orderType 주문 요청: $orderRequest")
                    if (isBuyOrder) {
                        orderService.buyOrder(orderRequest)
                    } else {
                        orderService.sellOrder(orderRequest)
                    }
                }
                
                Log.d(TAG, "$orderType API 응답 - isSuccessful: ${response.isSuccessful}, code: ${response.code()}")
                
                CoroutineScope(Dispatchers.Main).launch {
                    if (response.isSuccessful) {
                        val orderResponse = response.body()
                        if (orderResponse?.success == true) {
                            Log.d(TAG, "$orderType 주문 성공: ${orderResponse.message}")
                            showToast("${orderType} 주문이 성공적으로 실행되었습니다.")
                            
                            // 주문 성공 후 잔액 새로고침
                            orderDataCoordinator?.refreshBalance()
                            Log.d(TAG, "$orderType 주문 완료 후 잔액 새로고침 요청")
                            
                            // 시장가 주문 성공 시 최신 가격으로 OrderDataCoordinator 업데이트
                            if (isMarketOrder && orderDataCoordinator != null) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val latestPrice = getLatestMarketPrice()
                                        if (latestPrice != null) {
                                            orderDataCoordinator.updateCurrentPrice(latestPrice.toFloat())
                                            Log.d(TAG, "주문 체결 후 현재가 업데이트: $latestPrice")
                                        }
                                    } catch (e: Exception) {
                                        Log.w(TAG, "주문 체결 후 현재가 업데이트 실패: ${e.message}")
                                    }
                                }
                            }
                            
                            // 주문 성공 후 TradingDataHolder 데이터 새로고침 및 TradingFragment 업데이트
                            refreshTradingDataAfterOrder()
                            
                            // 실시간 시세 동기화를 위한 즉시 업데이트
                            orderDataCoordinator?.refreshBalance()
                            
                            // 주문 성공 콜백 호출
                            onOrderSuccess?.invoke()
                            
                            // 주문 성공 후 강제로 주문가능 금액 업데이트 (지연 실행)
                            CoroutineScope(Dispatchers.Main).launch {
                                kotlinx.coroutines.delay(500) // API 응답 대기
                                try {
                                    // OrderInputHandler가 있는 경우 강제 업데이트
                                    orderInputHandler?.updateOrderAvailableDisplay()
                                    orderInputHandler?.updateTradingInfoContent()
                                    Log.d(TAG, "주문 체결 후 강제 주문가능 금액 업데이트 완료")
                                } catch (e: Exception) {
                                    Log.e(TAG, "주문 체결 후 강제 업데이트 실패", e)
                                }
                            }
                        } else {
                            Log.e(TAG, "$orderType 주문 실패: ${orderResponse?.message}")
                            showErrorDialog(
                                R.string.dialog_title_error_order,
                                "${orderType} 주문 실패: ${orderResponse?.message}",
                                R.color.red
                            )
                        }
                    } else {
                        Log.e(TAG, "$orderType 주문 실패: ${response.code()} - ${response.errorBody()?.string()}")
                        showErrorDialog(
                            R.string.dialog_title_error_order,
                            "${orderType} 주문 실패: ${response.errorBody()?.string()}",
                            R.color.red
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "$orderType 주문 실행 중 오류 발생: ${e.message}", e)
                CoroutineScope(Dispatchers.Main).launch {
                    showErrorDialog(
                        R.string.dialog_title_error_order,
                        "${orderType} 주문 실행 중 오류가 발생했습니다: ${e.message}",
                        R.color.red
                    )
                }   
            }
        }
    }
    
    /**
     * 주문 체결 후 TradingDataHolder 데이터 새로고침 및 TradingFragment 업데이트
     */
    private fun refreshTradingDataAfterOrder() {
        try {
            Log.d(TAG, "주문 체결 후 데이터 새로고침 시작")
            
            // 1. API에서 최신 IP 리스팅 데이터 가져오기
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = IpListingRepository()
                    val updatedData = repository.getIpListing()
                    
                    if (updatedData.isNotEmpty()) {
                        // 2. TradingDataHolder 업데이트
                        com.stip.stip.iphome.TradingDataHolder.ipListingItems = updatedData
                        
                        Log.d(TAG, "TradingDataHolder 업데이트 완료: ${updatedData.size}개 아이템")
                        
                        // 3. UI 스레드에서 TradingFragment 업데이트
                        CoroutineScope(Dispatchers.Main).launch {
                            refreshTradingFragmentPriceData()
                        }
                    } else {
                        Log.w(TAG, "API에서 업데이트된 데이터가 없음")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "API 데이터 새로고침 실패", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "주문 체결 후 데이터 새로고침 중 오류", e)
        }
    }
    
    /**
     * TradingFragment의 가격 데이터를 강제로 새로고침
     */
    private fun refreshTradingFragmentPriceData() {
        try {
            // 현재 Activity에서 TradingFragment 찾기
            val activity = context as? androidx.fragment.app.FragmentActivity
            activity?.let { act ->
                // 모든 Fragment를 순회하면서 TradingFragment 찾기
                for (fragment in act.supportFragmentManager.fragments) {
                    if (fragment is com.stip.stip.iphome.fragment.TradingFragment) {
                        Log.d(TAG, "TradingFragment 발견 - 가격 데이터 강제 새로고침")
                        fragment.forceRefreshPriceData()
                        break
                    }
                    
                    // 중첩된 Fragment들도 확인
                    fragment.childFragmentManager.fragments.forEach { childFragment ->
                        if (childFragment is com.stip.stip.iphome.fragment.TradingFragment) {
                            Log.d(TAG, "중첩된 TradingFragment 발견 - 가격 데이터 강제 새로고침")
                            childFragment.forceRefreshPriceData()
                            return@forEach
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TradingFragment 가격 데이터 새로고침 중 오류", e)
        }
    }
}