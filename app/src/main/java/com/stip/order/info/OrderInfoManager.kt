package com.stip.stip.order

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.stip.stip.R
import com.stip.stip.databinding.FragmentOrderContentBinding
import com.stip.stip.iphome.util.OrderUtils
import com.stip.stip.signup.utils.PreferenceUtil
import java.util.Locale

class OrderInfoManager(
    private val context: Context,
    private val binding: FragmentOrderContentBinding,
    private val getCurrentPrice: () -> Float,
    private val getHeldAssetQuantity: () -> Double,
    private val getAverageBuyPrice: () -> Double,
    private val getCurrentTicker: () -> String?,
    private val userHoldsAsset: () -> Boolean
) {

    companion object {
        private const val TAG = "OrderInfoManager"
    }


    fun updateTradingInfoViewContent() {
        val infoBinding = binding.tradingInfoView ?: return
        
        // 로그인 상태 확인
        val isLoggedIn = PreferenceUtil.getMemberInfo() != null
        
        if (!isLoggedIn) {
            // 로그인하지 않은 경우 모든 값을 0 또는 "--"로 표시
            resetInfoViewTextsWithZeros(getCurrentTicker() ?: "")
            return
        }

        if (userHoldsAsset()) {
            try {
                val currentPrice = getCurrentPrice().toDouble()
                val heldQuantity = getHeldAssetQuantity()
                val avgBuyPrice = getAverageBuyPrice()
                val currentTicker = getCurrentTicker() ?: ""

                if (currentPrice <= 0 || heldQuantity <= 0) {
                    Log.w(TAG, "Cannot calculate trading info: Price=$currentPrice, Quantity=$heldQuantity")
                    resetInfoViewTexts(currentTicker)
                    return
                }

                val currentValuationAmount = heldQuantity * currentPrice
                val totalBuyAmount = heldQuantity * avgBuyPrice
                val valuationPl = currentValuationAmount - totalBuyAmount
                val rateOfReturn = if (totalBuyAmount > 0) (valuationPl / totalBuyAmount) * 100 else 0.0

                infoBinding.textLabelAssetsHeld.text = context.getString(R.string.label_assets_held_quantity)
                infoBinding.textValueAssetsHeld.text = "${OrderUtils.fixedTwoDecimalFormatter.format(heldQuantity)} $currentTicker"
                infoBinding.textValueAvgBuyPrice.text = "$${OrderUtils.fixedTwoDecimalFormatter.format(avgBuyPrice)}"
                infoBinding.textValueValuationAmount.text = "$${OrderUtils.fixedTwoDecimalFormatter.format(currentValuationAmount)}"
                infoBinding.textValueValuationPl.text = "$${OrderUtils.fixedTwoDecimalFormatter.format(valuationPl)}"
                infoBinding.textValueRateOfReturn.text = String.format(Locale.US, "%.2f%%", rateOfReturn)

                val isLoss = valuationPl < 0.0
                val colorRes = if (isLoss) R.color.percentage_negative_blue else R.color.percentage_positive_red
                val color = ContextCompat.getColor(context, colorRes)
                infoBinding.textValueValuationPl.setTextColor(color)
                infoBinding.textValueRateOfReturn.setTextColor(color)

            } catch (e: Exception) {
                Log.e(TAG, "Error updating trading info view content", e)
                resetInfoViewTexts(getCurrentTicker() ?: "")
            }
        } else {
            resetInfoViewTexts(getCurrentTicker() ?: "")
        }
    }

    private fun resetInfoViewTexts(ticker: String) {
        val infoBinding = binding.tradingInfoView ?: return
        val defaultColor = ContextCompat.getColor(context, R.color.text_primary)

        infoBinding.textValueAssetsHeld.text = "-- $ticker"
        infoBinding.textValueAvgBuyPrice.text = "$--.--"
        infoBinding.textValueValuationAmount.text = "$--.--"
        infoBinding.textValueValuationPl.text = "$--.--"
        infoBinding.textValueRateOfReturn.text = "--.--%%"
        infoBinding.textValueValuationPl.setTextColor(defaultColor)
        infoBinding.textValueRateOfReturn.setTextColor(defaultColor)
    }
    
    // 로그인하지 않은 경우 모든 값을 0으로 표시하는 새로운 메소드
    private fun resetInfoViewTextsWithZeros(ticker: String) {
        val infoBinding = binding.tradingInfoView ?: return
        val defaultColor = ContextCompat.getColor(context, R.color.text_primary)

        infoBinding.textValueAssetsHeld.text = "0.00 $ticker"
        infoBinding.textValueAvgBuyPrice.text = "$0.00"
        infoBinding.textValueValuationAmount.text = "$0.00"
        infoBinding.textValueValuationPl.text = "$0.00"
        infoBinding.textValueRateOfReturn.text = "0.00%"
        infoBinding.textValueValuationPl.setTextColor(defaultColor)
        infoBinding.textValueRateOfReturn.setTextColor(defaultColor)
    }
}