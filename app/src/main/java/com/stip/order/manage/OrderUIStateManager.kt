package com.stip.stip.order

import android.content.Context
import android.util.Log
import android.view.View
import com.stip.stip.R
import com.stip.stip.databinding.FragmentOrderContentBinding
import com.stip.stip.iphome.util.OrderUtils
import com.stip.stip.order.coordinator.OrderDataCoordinator
import com.stip.stip.signup.utils.PreferenceUtil
import androidx.core.content.ContextCompat

class OrderUIStateManager(
    private val context: Context,
    private val binding: FragmentOrderContentBinding,
    private val orderDataCoordinator: OrderDataCoordinator,
    private val historyManager: OrderHistoryManager, // <<<--- 수정된 타입
    private val uiInitializer: OrderUIInitializer
) {
    private lateinit var orderEntryViews: List<View>
    private var currentTabPosition: Int = 0

    companion object {
        private const val TAG = "OrderUIStateManager"
    }

    init {
        initializeOrderEntryViews()
        handleTabSelection(binding.tabLayoutOrderMode.selectedTabPosition, true)
    }

    private fun initializeOrderEntryViews() {
        orderEntryViews = listOfNotNull(
            binding.radioGroupOrderType,
            binding.rowOrderAvailable,
            binding.orderInputContainer,
            binding.buttonContainer,
            binding.tradingInfoView?.root
        )
    }

    fun handleTabSelection(position: Int, isInitialSetup: Boolean = false) {
        // 초기 설정이 아니고 같은 탭이면 스킵 (초기 설정 시에는 항상 실행)
        if (!isInitialSetup && position == currentTabPosition) {
            return
        }
        
        currentTabPosition = position

        val isHistoryTab = position == 2
        binding.unfilledFilledBoxRoot.visibility = if (isHistoryTab) View.VISIBLE else View.GONE
        setOrderEntryViewsVisibility(if (isHistoryTab) View.GONE else View.VISIBLE)

        try { // historyManager 메서드 호출 시 발생 가능한 오류 처리
            if (isHistoryTab) {
                historyManager.activate() // activate 호출 확인
            } else {
                historyManager.hide() // hide 호출 확인
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling historyManager activate/hide", e)
        }

        // 매수/매도 탭에 따라 버튼 상태 업데이트
        when (position) {
            0 -> { // 매수 탭
                binding.buttonBuy.text = context.getString(R.string.button_buy)
                binding.buttonBuy.setBackgroundColor(ContextCompat.getColor(context, R.color.percentage_positive_red))
            }
            1 -> { // 매도 탭
                binding.buttonBuy.text = context.getString(R.string.button_sell)
                binding.buttonBuy.setBackgroundColor(ContextCompat.getColor(context, R.color.percentage_negative_blue))
            }
            2 -> { // 내역 탭
                binding.buttonBuy.isEnabled = false
                binding.buttonBuy.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            }
        }

        updateTradingInfoVisibility()
        uiInitializer.updateBuySellButtonAppearance(position)
    }

    private fun setOrderEntryViewsVisibility(visibility: Int) {
        try {
            if (::orderEntryViews.isInitialized) {
                orderEntryViews.forEach { it.visibility = visibility }
            } else {
                Log.w(TAG, "orderEntryViews not initialized before setOrderEntryViewsVisibility call")
            }
        } catch (e: Exception) { // Catch potential exceptions like IllegalStateException
            Log.e(TAG, "Error setting visibility", e)
        }
    }

    fun updateTradingInfoVisibility() {
        try {
            val infoLayout = binding.tradingInfoView ?: return
            val holds = orderDataCoordinator.userHoldsAsset
            
            // Check if user is logged in
            val isLoggedIn = PreferenceUtil.isRealLoggedIn()
            
            if (!isLoggedIn) {
                // If not logged in, hide all trading info
                infoLayout.groupValuationInfo.visibility = View.GONE
                infoLayout.dividerLine.visibility = View.GONE
                infoLayout.groupOrderInfo.visibility = View.GONE
                return
            }
            
            if (currentTabPosition == 0 || currentTabPosition == 1) {
                val visibility = if (holds) View.VISIBLE else View.GONE
                infoLayout.groupValuationInfo.visibility = visibility
                infoLayout.dividerLine.visibility = visibility
                infoLayout.groupOrderInfo.visibility = View.VISIBLE
            } else {
                infoLayout.groupValuationInfo.visibility = View.GONE
                infoLayout.dividerLine.visibility = View.GONE
                infoLayout.groupOrderInfo.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateTradingInfoVisibility", e)
        }
    }

    fun updateOrderAvailableDisplay() {
        try {
            // 내역 탭(position == 2)인 경우 항상 주문가능 텍스트 숨김
            if (currentTabPosition == 2) {
                binding.rowOrderAvailable.visibility = View.GONE
                return
            }
            
            // 로그인 상태 확인
            val isLoggedIn = PreferenceUtil.isRealLoggedIn()
            
            if (!isLoggedIn) {
                binding.textOrderAvailableAmount.text = OrderUtils.fixedTwoDecimalFormatter.format(0.0)
                binding.textOrderAvailableUnit.text = if (currentTabPosition == 0) {
                    context.getString(R.string.unit_usd)
                } else {
                    orderDataCoordinator.currentTicker ?: "--"
                }
                binding.rowOrderAvailable.visibility = View.VISIBLE
                return
            }
            
            when (currentTabPosition) {
                0 -> {
                    // USD 잔액 표시 - 주문가능 (수수료 제외하지 않음)
                    val availableBalance = orderDataCoordinator.availableUsdBalance
                    binding.textOrderAvailableAmount.text = OrderUtils.fixedTwoDecimalFormatter.format(availableBalance)
                    binding.textOrderAvailableUnit.text = context.getString(R.string.unit_usd)
                    binding.rowOrderAvailable.visibility = if (availableBalance > 0.0) View.VISIBLE else View.GONE
                    Log.d("OrderUIStateManager", "매수 탭 - 주문가능 금액: $availableBalance")
                }
                1 -> {
                    // 매도 탭에서는 해당 티커의 보유 자산 수량 표시
                    val heldQuantity = orderDataCoordinator.heldAssetQuantity
                    val tickerName = orderDataCoordinator.currentTicker ?: "--"
                    binding.textOrderAvailableAmount.text = OrderUtils.fixedTwoDecimalFormatter.format(heldQuantity)
                    binding.textOrderAvailableUnit.text = tickerName
                    binding.rowOrderAvailable.visibility = if (heldQuantity > 0.0) View.VISIBLE else View.GONE
                    Log.d("OrderUIStateManager", "매도 탭 - 주문가능 수량: $heldQuantity $tickerName")
                }
                else -> {
                    binding.rowOrderAvailable.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateOrderAvailableDisplay error", e)
            try { binding.rowOrderAvailable.visibility = View.GONE } catch (_: Exception) {}
        }
    }
}