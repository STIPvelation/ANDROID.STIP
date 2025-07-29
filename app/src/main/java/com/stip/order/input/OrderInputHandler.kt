package com.stip.stip.order

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import com.stip.stip.R
import com.stip.stip.databinding.FragmentOrderContentBinding
import com.stip.stip.signup.utils.PreferenceUtil
import java.text.DecimalFormat
import kotlin.math.floor

class OrderInputHandler(
    private val context: Context,
    private val binding: FragmentOrderContentBinding,
    private val numberParseFormat: DecimalFormat,
    private val fixedTwoDecimalFormatter: DecimalFormat,
    private val getCurrentPrice: () -> Double,
    private val getFeeRate: () -> Double,
    private val availableUsdBalance: () -> Double,
    private val heldAssetQuantity: () -> Double,
    private val getCurrentTicker: () -> String?,
    private val getCurrentOrderType: () -> Int,
    private val getHeldAssetEvalAmount: () -> Double = { 0.0 }
) {

    private var calculatedMaxQty: Double = 0.0
    private enum class LastEdited { QUANTITY, TOTAL_AMOUNT, PRICE, NONE }
    private var lastEditedFocus: LastEdited = LastEdited.NONE

    val quantityTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            updateCalculatedTotal()
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    val priceTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            updateCalculatedTotal()
            calculateAndDisplayMaxQuantity()
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    val formatOnFocusLostListener = View.OnFocusChangeListener { view, hasFocus ->
        if (!hasFocus && view is EditText) {
            val currentText = view.text.toString()
            try {
                val number = if (currentText.isBlank()) 0.0 else (numberParseFormat.parse(currentText)?.toDouble() ?: 0.0)
                val watcher = getWatcherForEditText(view)
                watcher?.let { view.removeTextChangedListener(it) }
                view.setText(fixedTwoDecimalFormatter.format(number))
                watcher?.let { view.addTextChangedListener(it) }

                updateCalculatedTotal()
                if (view.id == R.id.editTextLimitPrice) {
                    calculateAndDisplayMaxQuantity()
                }
            } catch (e: Exception) {
                Log.e("OrderInputHandler", "Error formatting number on focus lost: $currentText", e)
                val watcher = getWatcherForEditText(view)
                watcher?.let { view.removeTextChangedListener(it) }
                view.setText(fixedTwoDecimalFormatter.format(0.0))
                watcher?.let { view.addTextChangedListener(it) }
                updateCalculatedTotal()
                if (view.id == R.id.editTextLimitPrice) {
                    calculateAndDisplayMaxQuantity()
                }
            }
        } else if (hasFocus && view is EditText) {
            when(view.id) {
                R.id.editTextQuantity -> lastEditedFocus = LastEdited.QUANTITY
                R.id.editTextLimitPrice -> lastEditedFocus = LastEdited.PRICE
            }
        }
    }

    private fun getWatcherForEditText(editText: EditText): TextWatcher? {
        return when(editText.id) {
            R.id.editTextQuantity -> quantityTextWatcher
            R.id.editTextLimitPrice -> priceTextWatcher
            else -> null
        }
    }

    fun setupInputListeners() {
        binding.editTextQuantity.addTextChangedListener(quantityTextWatcher)
        binding.editTextLimitPrice.addTextChangedListener(priceTextWatcher)
        binding.editTextQuantity.onFocusChangeListener = formatOnFocusLostListener
        binding.editTextLimitPrice.onFocusChangeListener = formatOnFocusLostListener
        binding.editTextTriggerPrice?.onFocusChangeListener = formatOnFocusLostListener
        setupTotalRowClickListener()
    }

    private fun setupTotalRowClickListener() {
        binding.rowCalculatedTotal.setOnClickListener {
            Log.d("OrderInputHandler", "Calculated Total Row clicked - Applying Max Quantity")
            val maxQty = calculateMaxQuantity()
            binding.editTextQuantity.setText(fixedTwoDecimalFormatter.format(maxQty.coerceAtLeast(0.0)))
            updateCalculatedTotal()
            showKeyboard(binding.editTextQuantity)
        }
        binding.editTextQuantity.setOnClickListener(null)
    }

    fun setupPriceAdjustmentButtons() {
        // 버튼 클릭 중복 방지를 위한 플래그
        var isAdjustingPrice = false
        
        binding.buttonPricePlus.setOnClickListener {
            if (!isAdjustingPrice) {
                isAdjustingPrice = true
                try {
                    adjustPrice(+1)
                    updateCalculatedTotal()
                    calculateAndDisplayMaxQuantity()
                } finally {
                    // 약간의 지연 후 플래그 해제 (중복 클릭 방지)
                    binding.buttonPricePlus.postDelayed({ isAdjustingPrice = false }, 100)
                }
            }
        }
        
        binding.buttonPriceMinus.setOnClickListener {
            if (!isAdjustingPrice) {
                isAdjustingPrice = true
                try {
                    adjustPrice(-1)
                    updateCalculatedTotal()
                    calculateAndDisplayMaxQuantity()
                } finally {
                    // 약간의 지연 후 플래그 해제 (중복 클릭 방지)
                    binding.buttonPriceMinus.postDelayed({ isAdjustingPrice = false }, 100)
                }
            }
        }
        
        binding.buttonTriggerPricePlus?.setOnClickListener { 
            if (!isAdjustingPrice) {
                isAdjustingPrice = true
                try {
                    adjustTriggerPrice(+1)
                } finally {
                    binding.buttonTriggerPricePlus?.postDelayed({ isAdjustingPrice = false }, 100)
                }
            }
        }
        
        binding.buttonTriggerPriceMinus?.setOnClickListener { 
            if (!isAdjustingPrice) {
                isAdjustingPrice = true
                try {
                    adjustTriggerPrice(-1)
                } finally {
                    binding.buttonTriggerPriceMinus?.postDelayed({ isAdjustingPrice = false }, 100)
                }
            }
        }
    }

    private fun adjustPrice(delta: Int) {
        try {
            val currentText = binding.editTextLimitPrice.text?.toString()
            val current = parseDouble(currentText)
            
            // 현재가를 기준으로 계산
            val currentPrice = getCurrentPrice()
            val base = if (current <= 0.0 || current.isNaN() || current.isInfinite()) {
                currentPrice.toDouble()
            } else {
                current
            }
            
            // 호가 단위 계산
            val tick = getTick(base)
            
            // 새 가격 계산 (음수가 되지 않도록 보호)
            var newPrice = (base + delta * tick).coerceAtLeast(tick)
            
            // 부동소수점 정밀도 문제 해결: 호가 단위에 맞춰 반올림
            newPrice = roundToTick(newPrice, tick)
            
            Log.d("OrderInputHandler", "adjustPrice: current=$current, base=$base, tick=$tick, delta=$delta, newPrice=$newPrice")
            
            // 유효한 가격인지 확인
            if (newPrice.isFinite() && newPrice > 0) {
                val formattedPrice = fixedTwoDecimalFormatter.format(newPrice)
                
                // TextWatcher 일시 제거하여 무한 루프 방지
                val watcher = priceTextWatcher
                binding.editTextLimitPrice.removeTextChangedListener(watcher)
                binding.editTextLimitPrice.setText(formattedPrice)
                binding.editTextLimitPrice.addTextChangedListener(watcher)
                
                // 예약 주문인 경우 트리거 가격도 동일하게 설정
                if (getCurrentOrderType() == R.id.radio_reserved_order) {
                    binding.editTextTriggerPrice?.let { triggerEditText ->
                        val triggerWatcher = getWatcherForEditText(triggerEditText)
                        triggerWatcher?.let { triggerEditText.removeTextChangedListener(it) }
                        triggerEditText.setText(formattedPrice)
                        triggerWatcher?.let { triggerEditText.addTextChangedListener(it) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("OrderInputHandler", "가격 조정 중 오류 발생", e)
        }
    }

    private fun adjustTriggerPrice(delta: Int) {
        try {
            val editText = binding.editTextTriggerPrice ?: return
            val currentText = editText.text?.toString()
            val current = parseDouble(currentText)
            
            // 현재가를 기준으로 계산
            val currentPrice = getCurrentPrice()
            val base = if (current <= 0.0 || current.isNaN() || current.isInfinite()) {
                currentPrice.toDouble()
            } else {
                current
            }
            
            // 호가 단위 계산
            val tick = getTick(base)
            
            // 새 가격 계산 (음수가 되지 않도록 보호)
            var newPrice = (base + delta * tick).coerceAtLeast(tick)
            
            // 부동소수점 정밀도 문제 해결: 호가 단위에 맞춰 반올림
            newPrice = roundToTick(newPrice, tick)
            
            // 유효한 가격인지 확인
            if (newPrice.isFinite() && newPrice > 0) {
                val formattedPrice = fixedTwoDecimalFormatter.format(newPrice)
                
                // TextWatcher 일시 제거하여 무한 루프 방지
                val watcher = getWatcherForEditText(editText)
                watcher?.let { editText.removeTextChangedListener(it) }
                editText.setText(formattedPrice)
                watcher?.let { editText.addTextChangedListener(it) }
            }
        } catch (e: Exception) {
            Log.e("OrderInputHandler", "트리거 가격 조정 중 오류 발생", e)
        }
    }

    fun resetInputs() {
        lastEditedFocus = LastEdited.NONE
        resetEditText(binding.editTextLimitPrice)
        resetEditText(binding.editTextQuantity)
        binding.editTextTriggerPrice?.let { resetEditText(it) }
        binding.textCalculatedTotal.text = fixedTwoDecimalFormatter.format(0.0)
        calculatedMaxQty = 0.0
        updateUiForOrderTypeChange() // 리셋 시 현재 주문 유형에 맞게 UI 업데이트
        try {
            if (binding.spinnerAvailableQuantity.adapter?.count ?: 0 > 0) {
                binding.spinnerAvailableQuantity.setSelection(0, false)
            }
        } catch (e: Exception) { Log.e("OrderInputHandler", "Error resetting spinner", e) }
    }

    fun setupResetButton() {
        binding.buttonReset.setOnClickListener { resetInputs() }
    }

    private fun resetEditText(editText: EditText) {
        val watcher = getWatcherForEditText(editText)
        watcher?.let { editText.removeTextChangedListener(it) }
        editText.setText(fixedTwoDecimalFormatter.format(0.0))
        watcher?.let { editText.addTextChangedListener(it) }
    }

    fun setupQuantitySpinner() {
        val ctx = binding.root.context
        val options = listOf(
            ctx.getString(R.string.quantity_option_available),
            ctx.getString(R.string.quantity_option_max),
            ctx.getString(R.string.quantity_option_75),
            ctx.getString(R.string.quantity_option_50),
            ctx.getString(R.string.quantity_option_25),
            ctx.getString(R.string.quantity_option_10)
        )

        val adapter = ArrayAdapter(ctx, R.layout.custom_spinner_item_quantity, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAvailableQuantity.adapter = adapter

        binding.spinnerAvailableQuantity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position == 0) return

                val factor = when (position) {
                    1 -> 1.0; 2 -> 0.75; 3 -> 0.5; 4 -> 0.25; 5 -> 0.10; else -> 0.0
                }

                val selectedTab = binding.tabLayoutOrderMode.selectedTabPosition
                
                if (selectedTab == 0) { // 매수 탭
                    val isMarketOrder = getCurrentOrderType() == R.id.radio_market_order
                    
                    if (isMarketOrder) {
                        // 시장가 매수: 주문가능 금액의 퍼센트만큼 총액 필드에 입력
                        val availableBalance = availableUsdBalance()
                        val targetAmount = availableBalance * factor
                        
                        val watcher = quantityTextWatcher
                        binding.editTextQuantity.removeTextChangedListener(watcher)
                        binding.editTextQuantity.setText(fixedTwoDecimalFormatter.format(targetAmount.coerceAtLeast(0.0)))
                        binding.editTextQuantity.addTextChangedListener(watcher)
                        
                        lastEditedFocus = LastEdited.TOTAL_AMOUNT
                    } else {
                        val priceStr = binding.editTextLimitPrice.text?.toString()
                        val currentPrice = parseDouble(priceStr)
                        
                        if (currentPrice <= 0) {
                            // 가격이 입력되지 않은 경우 토스트 메시지 표시
                            Toast.makeText(context, "가격을 먼저 입력해주세요", Toast.LENGTH_SHORT).show()
                            binding.spinnerAvailableQuantity.setSelection(0, false)
                            return
                        }
                        
                        // 가격이 입력된 경우 주문가능 금액의 퍼센트만큼 수량 필드에 입력 (수수료 없음)
                        val availableBalance = availableUsdBalance()
                        val targetAmount = availableBalance * factor
//                        val fee = getFeeRate()

                        // 수수료를 고려한 수량 계산: (주문가능금액 * 퍼센트) / (가격 * (1 + 수수료))
                        // 이렇게 하면 수량 * 가격 * (1 + 수수료) = 주문가능금액 * 퍼센트가 됨
//                        val targetQty = floor((targetAmount / (currentPrice * (1 + fee))) * 100_000_000) / 100_000_00 수수료 계산하면 이거 사용
                        // 수수료 없이 수량 계산: (주문가능금액 * 퍼센트) / 가격
                        val targetQty = floor((targetAmount / currentPrice) * 100_000_000) / 100_000_000
                        
                        val watcher = quantityTextWatcher
                        binding.editTextQuantity.removeTextChangedListener(watcher)
                        // 수량 표시 소수점 8자리까지 표시
                        val preciseFormatter = DecimalFormat("#,##0.########")
                        binding.editTextQuantity.setText(preciseFormatter.format(targetQty.coerceAtLeast(0.0)))
                        binding.editTextQuantity.addTextChangedListener(watcher)
                        
                        lastEditedFocus = LastEdited.QUANTITY
                    }
                } else { // 매도 탭
                    val isMarketOrder = getCurrentOrderType() == R.id.radio_market_order
                    
                    if (isMarketOrder) {
                        // 시장가 매도: 보유 자산 수량의 퍼센트만큼 수량 필드에 입력
                        val heldQty = heldAssetQuantity()
                        
                        if (heldQty <= 0) {
                            Toast.makeText(context, "보유 자산이 없습니다", Toast.LENGTH_SHORT).show()
                            binding.spinnerAvailableQuantity.setSelection(0, false)
                            return
                        }
                        
                        val targetQty = heldQty * factor
                        
                        val watcher = quantityTextWatcher
                        binding.editTextQuantity.removeTextChangedListener(watcher)
                        binding.editTextQuantity.setText(fixedTwoDecimalFormatter.format(targetQty.coerceAtLeast(0.0)))
                        binding.editTextQuantity.addTextChangedListener(watcher)
                        
                        lastEditedFocus = LastEdited.QUANTITY
                        Log.d("OrderInputHandler", "시장가 매도 - heldQty: $heldQty, targetQty: $targetQty, factor: $factor")
                    } else {
                        // 지정가/예약 매도: 가격이 입력되지 않은 경우 메시지 표시
                        val priceStr = binding.editTextLimitPrice.text?.toString()
                        val currentPrice = parseDouble(priceStr)
                        
                        if (currentPrice <= 0) {
                            // 가격이 입력되지 않은 경우 토스트 메시지 표시
                            Toast.makeText(context, "가격을 먼저 입력해주세요", Toast.LENGTH_SHORT).show()
                            binding.spinnerAvailableQuantity.setSelection(0, false)
                            return
                        }
                        
                        // 가격이 입력된 경우 보유 자산 수량의 퍼센트만큼 수량 필드에 입력
                        val heldQty = heldAssetQuantity()
                        
                        if (heldQty <= 0) {
                            Toast.makeText(context, "보유 자산이 없습니다", Toast.LENGTH_SHORT).show()
                            binding.spinnerAvailableQuantity.setSelection(0, false)
                            return
                        }
                        
                        val targetQty = heldQty * factor
                        
                        val watcher = quantityTextWatcher
                        binding.editTextQuantity.removeTextChangedListener(watcher)
                        binding.editTextQuantity.setText(fixedTwoDecimalFormatter.format(targetQty.coerceAtLeast(0.0)))
                        binding.editTextQuantity.addTextChangedListener(watcher)
                        
                        lastEditedFocus = LastEdited.QUANTITY
                        Log.d("OrderInputHandler", "지정가 매도 - heldQty: $heldQty, targetQty: $targetQty, factor: $factor, price: $currentPrice")
                    }
                }

                updateCalculatedTotal()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }


    fun updateCalculatedTotal() {
        val qtyStr = binding.editTextQuantity.text?.toString()
        val priceStr = binding.editTextLimitPrice.text?.toString()
        val qty = parseDouble(qtyStr)
        val price = parseDouble(priceStr)
        var displayTotal = 0.0

        try {
            if (price > 0 && qty > 0) {
                val isBuy = binding.tabLayoutOrderMode.selectedTabPosition == 0
                val isMarketOrder = getCurrentOrderType() == R.id.radio_market_order
//                val fee = getFeeRate()
//                displayTotal = if (isBuy && !isMarketOrder) grossAmount * (1 + fee) else if (!isBuy) grossAmount * (1 - fee) else grossAmount 수수료 계산하면 이거 사용
                val grossAmount = qty * price
                // 수수료 계산 제거 - 매수/매도 모두 수수료 없이 계산
                displayTotal = grossAmount
            }
        } catch (e: Exception) {
            Log.e("OrderInputHandler", "Error calculating total", e)
            displayTotal = 0.0
        }

        binding.textCalculatedTotal.text = fixedTwoDecimalFormatter.format(displayTotal.coerceAtLeast(0.0))
        binding.labelCalculatedTotal.text = context.getString(R.string.label_total_amount)
        binding.textUnitCalculatedTotal.text = context.getString(R.string.unit_usd)
    }

    // OrderInputHandler.kt 내부

    fun updateUiForOrderTypeChange() {
        val context = binding.root.context
        val orderType = getCurrentOrderType()
        val isMarketOrder = orderType == R.id.radio_market_order
        val isBuyTab = binding.tabLayoutOrderMode.selectedTabPosition == 0
        val isReservedOrder = orderType == R.id.radio_reserved_order

        // 가격 라벨 설정 (예약 주문 시 변경)
        binding.labelLimitPrice.text = if(isReservedOrder) context.getString(R.string.label_order_price1) else context.getString(R.string.label_price)
        // 총액(계산 결과) 라벨 및 단위는 항상 고정
        binding.labelCalculatedTotal.text = context.getString(R.string.label_total_amount)
        binding.textUnitCalculatedTotal.text = context.getString(R.string.unit_usd)

        // --- ▼▼▼ 시장가 주문에 따른 UI 변경 ▼▼▼ ---
        if (isMarketOrder) {
            if (isBuyTab) {
                // --- 시장가 매수: 총액 입력 모드 ---
                binding.labelQuantity.text = context.getString(R.string.label_total_amount) // 라벨: "총액"
                binding.editTextQuantity.hint = context.getString(R.string.hint_enter_total_amount) // 힌트: "총액 입력"
                binding.textUnitQuantity.text = context.getString(R.string.unit_usd) // 단위: "USD"
                binding.textOrderAvailableUnit.visibility = View.VISIBLE
                calculateAndDisplayMaxQuantity()
            } else {
                // --- 시장가 매도: 수량 입력 모드 ---
                binding.labelQuantity.text = context.getString(R.string.label_quantity) // 라벨: "수량"
                binding.editTextQuantity.hint = context.getString(R.string.hint_enter_quantity) // 힌트: "수량 입력"
                binding.textUnitQuantity.text = getCurrentTicker() ?: "" // 단위: 티커
                binding.textOrderAvailableUnit.visibility = View.VISIBLE
                calculateAndDisplayMaxQuantity()
            }
        } else {
            // --- 지정가/예약 주문: 수량 입력 모드 ---
            binding.labelQuantity.text = context.getString(R.string.label_quantity) // 라벨: "수량"
            binding.editTextQuantity.hint = context.getString(R.string.hint_enter_quantity) // 힌트: "수량 입력"
            binding.textUnitQuantity.text = getCurrentTicker() ?: "" // 단위: 티커
            binding.textOrderAvailableUnit.visibility = View.VISIBLE
            calculateAndDisplayMaxQuantity()
        }
        // --- ▲▲▲ 시장가 주문에 따른 UI 변경 ▲▲▲ ---

        binding.editTextQuantity.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        updateCalculatedTotal() // 주문 유형 변경 시 총액 재계산 및 표시
        updateOrderAvailableDisplay() // 주문가능 금액 표시 업데이트
    }


    fun handleOrderBookPriceClick(price: String) {
        try {
            val priceNum = numberParseFormat.parse(price)?.toDouble() ?: 0.0
            val formattedPrice = fixedTwoDecimalFormatter.format(priceNum)
            binding.editTextLimitPrice.setText(formattedPrice)

            if (getCurrentOrderType() == R.id.radio_reserved_order) {
                binding.editTextTriggerPrice?.setText(formattedPrice)
            }
            updateCalculatedTotal()
            calculateAndDisplayMaxQuantity()
        } catch (e: Exception) {
            Log.e("OrderInputHandler", "Error handling order book price click", e)
            binding.editTextLimitPrice.setText(fixedTwoDecimalFormatter.format(0.0))
            updateCalculatedTotal()
            calculateAndDisplayMaxQuantity()
        }
    }

    private fun calculateAndDisplayMaxQuantity() {
        val calculatedQty = calculateMaxQuantity()
        this.calculatedMaxQty = calculatedQty
        // binding.textOrderAvailableUnit.visibility = View.GONE
    }


    private fun calculateMaxQuantity(): Double {
        // 로그인 상태 확인
        val isLoggedIn = PreferenceUtil.getMemberInfo() != null
        
        // 로그인하지 않은 경우 무조건 0 반환
        if (!isLoggedIn) {
            return 0.0
        }
        
        val selectedTab = binding.tabLayoutOrderMode.selectedTabPosition
        var maxQty = 0.0
        val isMarketOrder = getCurrentOrderType() == R.id.radio_market_order

        if (selectedTab == 0) { // 매수 탭
            if (isMarketOrder) {
                // 시장가 매수: 총액 기준으로 계산
                maxQty = availableUsdBalance()
            } else {
                // 지정가/예약 매수: 수량 기준으로 계산
                val availableBalance = availableUsdBalance()
                val currentPrice = getCurrentPrice()
                if (currentPrice > 0) {
                    maxQty = availableBalance / currentPrice
                }
            }
        } else { // 매도 탭
            maxQty = heldAssetQuantity()
        }

        return maxQty.coerceAtLeast(0.0)
    }

    /**
     * 주문 가능 금액 표시 업데이트
     */
    fun updateOrderAvailableDisplay() {
        try {
            val selectedTab = binding.tabLayoutOrderMode.selectedTabPosition
            
            when (selectedTab) {
                0 -> { // 매수 탭
                    // USD 잔액 표시 - 주문가능 (수수료 제외하지 않음)
                    val availableBalance = availableUsdBalance()
                    binding.textOrderAvailableAmount.text = fixedTwoDecimalFormatter.format(availableBalance)
                    binding.textOrderAvailableUnit.text = context.getString(R.string.unit_usd)
                    binding.rowOrderAvailable.visibility = if (availableBalance > 0.0) View.VISIBLE else View.GONE
                    Log.d("OrderInputHandler", "매수 탭 - 주문가능 금액: $availableBalance USD")
                }
                1 -> { // 매도 탭
                    // 매도 탭에서는 해당 티커의 보유 자산 수량 표시
                    val heldQuantity = heldAssetQuantity()
                    val tickerName = getCurrentTicker() ?: "--"
                    binding.textOrderAvailableAmount.text = fixedTwoDecimalFormatter.format(heldQuantity)
                    binding.textOrderAvailableUnit.text = tickerName
                    binding.rowOrderAvailable.visibility = if (heldQuantity > 0.0) View.VISIBLE else View.GONE
                    Log.d("OrderInputHandler", "매도 탭 - 주문가능 수량: $heldQuantity $tickerName")
                }
                else -> {
                    binding.rowOrderAvailable.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            Log.e("OrderInputHandler", " 주문가능 디스플레이 에러", e)
        }
    }

    /**
     * 거래 정보 내용 업데이트
     */
    fun updateTradingInfoContent() {
        try {
            // OrderInfoManager를 사용하여 거래 정보 업데이트
            val orderInfoManager = OrderInfoManager(
                context = context,
                binding = binding,
                getCurrentPrice = { getCurrentPrice().toFloat() },
                getHeldAssetQuantity = { heldAssetQuantity() },
                getAverageBuyPrice = { getCurrentPrice() }, // 현재는 현재 가격을 평균 매수가로 사용
                getCurrentTicker = { getCurrentTicker() },
                userHoldsAsset = { heldAssetQuantity() > 0.0 }
            )
            orderInfoManager.updateTradingInfoViewContent()
        } catch (e: Exception) {
            Log.e("OrderInputHandler", "거래 정보 업데이트 에러", e)
        }
    }

    private fun parseDouble(value: String?): Double {
        return try {
            if (value.isNullOrBlank()) 0.0 else (numberParseFormat.parse(value)?.toDouble() ?: 0.0)
        } catch (e: Exception) {
            Log.w("OrderInputHandler", "Failed to parse double: '$value'")
            0.0
        }
    }

    private fun getTick(base: Double): Double {
        val tick = when {
            base >= 0.1 && base < 1.0 -> 0.001
            base >= 1.0 && base < 10.0 -> 0.01
            base >= 10.0 && base < 100.0 -> 0.1
            base >= 100.0 && base < 1000.0 -> 0.5
            base >= 1000.0 && base < 5000.0 -> 1.0
            base >= 5000.0 && base < 10000.0 -> 10.0
            base >= 10000.0 -> 100.0
            else -> 0.001 // 기본값 (0.1 미만)
        }
        
        Log.d("OrderInputHandler", "getTick: base=$base, tick=$tick")
        return tick
    }
    
    /**
     * 가격을 호가 단위에 맞춰 반올림
     */
    private fun roundToTick(price: Double, tick: Double): Double {
        if (tick <= 0) return price
        
        // 호가 단위의 소수점 자릿수 계산
        val tickString = tick.toString()
        val decimalPlaces = if (tickString.contains(".")) {
            tickString.substringAfter(".").length
        } else {
            0
        }
        
        // 호가 단위에 맞춰 반올림
        val multiplier = Math.pow(10.0, decimalPlaces.toDouble())
        val rounded = Math.round(price * multiplier) / multiplier
        
        return rounded
    }

    private fun showKeyboard(view: View) {
        view.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}