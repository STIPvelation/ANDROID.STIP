package com.stip.stip.iphome.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.stip.stip.R
import com.stip.stip.databinding.DialogConfirmOrderBinding
import com.stip.stip.iphome.util.OrderUtils // For parsing if needed

class ConfirmOrderDialogFragment : DialogFragment() {

    private var _binding: DialogConfirmOrderBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "ConfirmOrderDialog"
        const val REQUEST_KEY = "confirm_order_request_key" // Renamed for clarity

        const val RESULT_KEY_CONFIRMED = "confirm_order_result_key_confirmed"
        const val RESULT_KEY_IS_BUY = "confirm_order_result_key_is_buy"
        const val RESULT_KEY_QUANTITY = "confirm_order_result_key_quantity"
        const val RESULT_KEY_PRICE = "confirm_order_result_key_price"

        private const val ARG_IS_BUY_ORDER = "is_buy_order"
        private const val ARG_TICKER_FULL = "ticker_full"
        private const val ARG_ORDER_TYPE = "order_type"
        private const val ARG_PRICE_VALUE = "price_value"
        private const val ARG_QUANTITY_VALUE = "quantity_value"
        private const val ARG_TOTAL_VALUE = "total_value"
        private const val ARG_TRIGGER_PRICE = "trigger_price"
        private const val ARG_FEE_VALUE = "fee_value"

        fun newInstance(
            isBuyOrder: Boolean,
            tickerFull: String,
            orderType: String,
            priceValue: String,
            quantityValue: String,
            totalValue: String,
            feeValue: String,
            triggerPriceValue: String? = null
        ): ConfirmOrderDialogFragment {
            val args = Bundle().apply {
                putBoolean(ARG_IS_BUY_ORDER, isBuyOrder)
                putString(ARG_TICKER_FULL, tickerFull)
                putString(ARG_ORDER_TYPE, orderType)
                putString(ARG_PRICE_VALUE, priceValue)
                putString(ARG_QUANTITY_VALUE, quantityValue)
                putString(ARG_TOTAL_VALUE, totalValue)
                putString(ARG_FEE_VALUE, feeValue)
                triggerPriceValue?.let { putString(ARG_TRIGGER_PRICE, it) }
            }
            return ConfirmOrderDialogFragment().apply { arguments = args }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogConfirmOrderBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let { args ->
            val isBuyOrder = args.getBoolean(ARG_IS_BUY_ORDER)
            val tickerFull = args.getString(ARG_TICKER_FULL) ?: "N/A"
            val orderType = args.getString(ARG_ORDER_TYPE) ?: ""
            val priceValue = args.getString(ARG_PRICE_VALUE) ?: "0"
            val quantityValue = args.getString(ARG_QUANTITY_VALUE) ?: "0"
            val totalValue = args.getString(ARG_TOTAL_VALUE) ?: "0"
            val feeValue = args.getString(ARG_FEE_VALUE) ?: "0.00"
            val triggerPriceValue = args.getString(ARG_TRIGGER_PRICE)

            val tickerSymbol = tickerFull.substringBefore("/").ifEmpty { tickerFull }

            binding.textTickerConfirm.background?.setTintList(null)
            binding.textTickerConfirm.setBackgroundColor(Color.parseColor("#EDF5FD"))
            binding.textTickerConfirm.text = tickerFull

            if (isBuyOrder) {
                binding.textDialogTitle.text = getString(R.string.dialog_title_confirm_buy)
                binding.buttonConfirmSell.text = getString(R.string.dialog_confirm_buy)
                context?.let { binding.buttonConfirmSell.backgroundTintList = ContextCompat.getColorStateList(it, R.color.percentage_positive_red) }
            } else {
                binding.textDialogTitle.text = getString(R.string.dialog_title_confirm_sell)
                binding.buttonConfirmSell.text = getString(R.string.dialog_confirm_sell)
                context?.let { binding.buttonConfirmSell.backgroundTintList = ContextCompat.getColorStateList(it, R.color.confirm_button_blue) }
            }

            binding.textOrderTypeConfirm.text = orderType
            binding.textOrderPriceValueConfirm.text = priceValue
            binding.textOrderPriceUnitConfirm.text = getString(R.string.unit_usd)
            binding.textOrderQuantityValueConfirm.text = quantityValue
            binding.textOrderQuantityUnitConfirm.text = tickerSymbol

            binding.labelFeeConfirm.text = getString(R.string.label_fee)
            binding.textFeeValueConfirm.text = feeValue
            // 매수 시에는 baseAsset 단위, 매도 시에는 USD 단위
            binding.textFeeUnitConfirm.text = if (isBuyOrder) tickerSymbol else getString(R.string.unit_usd)

            binding.textOrderTotalValueConfirm.text = totalValue
            binding.textOrderTotalUnitConfirm.text = getString(R.string.unit_usd)

            if (triggerPriceValue != null) {
                binding.groupTriggerPriceConfirm.visibility = View.VISIBLE
                binding.textTriggerPriceValueConfirm.text = triggerPriceValue
                binding.textTriggerPriceUnitConfirm.text = getString(R.string.unit_usd)
            } else {
                binding.groupTriggerPriceConfirm.visibility = View.GONE
            }

        } ?: Log.e(TAG, "Arguments are null in onViewCreated")

        binding.buttonClose.setOnClickListener {
            parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle().apply {
                putBoolean(RESULT_KEY_CONFIRMED, false)
            })
            dismiss()
        }

        binding.buttonConfirmSell.setOnClickListener {
            val resultBundle = Bundle()
            resultBundle.putBoolean(RESULT_KEY_CONFIRMED, true)

            arguments?.let { args ->
                val isBuy = args.getBoolean(ARG_IS_BUY_ORDER)
                val quantityStr = args.getString(ARG_QUANTITY_VALUE)
                val priceStr = args.getString(ARG_PRICE_VALUE)
                val totalStr = args.getString(ARG_TOTAL_VALUE)
                val orderType = args.getString(ARG_ORDER_TYPE) ?: ""

                resultBundle.putBoolean(RESULT_KEY_IS_BUY, isBuy)

                // 시장가 매수의 경우 totalValue를 quantity로 사용, 시장가 매도는 일반 quantity 사용
                val quantity = if (isBuy && orderType.contains("시장가")) {
                    // 시장가 매수: totalValue 사용
                    try {
                        OrderUtils.numberParseFormat.parse(totalStr ?: "0")?.toDouble() ?: 0.0
                    } catch (e: Exception) { 
                        Log.e(TAG, "Error parsing total value for market buy: $totalStr", e)
                        0.0 
                    }
                } else {
                    // 지정가 주문 또는 시장가 매도: quantity 사용
                    try {
                        OrderUtils.numberParseFormat.parse(quantityStr ?: "0")?.toDouble() ?: 0.0
                    } catch (e: Exception) { 
                        Log.e(TAG, "Error parsing quantity: $quantityStr", e)
                        0.0 
                    }
                }
                
                Log.d(TAG, "Dialog result - isBuy: $isBuy, orderType: '$orderType', quantityStr: '$quantityStr', totalStr: '$totalStr', final quantity: $quantity")
                resultBundle.putDouble(RESULT_KEY_QUANTITY, quantity)

                try {
                    // Price might be "시장가" or a number, handle parsing
                    if (priceStr != null && priceStr != context?.getString(R.string.market_price)) {
                        val price = OrderUtils.numberParseFormat.parse(priceStr)?.toDouble()
                        if (price != null) {
                            resultBundle.putDouble(RESULT_KEY_PRICE, price)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not parse price value for result: $priceStr")
                }
            }
            parentFragmentManager.setFragmentResult(REQUEST_KEY, resultBundle)
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val dpWidth = 360
            val density = resources.displayMetrics.density
            val widthPx = (dpWidth * density).toInt()
            window.setLayout(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
            Log.d(TAG, "Dialog size set to: width=${widthPx}px, height=WRAP_CONTENT")
        } ?: Log.e(TAG, "Dialog window is null in onStart, cannot set size.")
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        // 모달이 닫힐 때 부모 Fragment에 알림
        parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle().apply {
            putBoolean(RESULT_KEY_CONFIRMED, false)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}