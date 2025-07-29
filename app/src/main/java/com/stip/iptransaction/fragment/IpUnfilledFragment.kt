package com.stip.stip.iptransaction.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView

import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.stip.stip.R
import com.stip.stip.ScrollableToTop
import com.stip.stip.databinding.FragmentIpUnfilledBinding
import com.stip.stip.iphome.adapter.UnfilledOrderAdapter
import com.stip.stip.iptransaction.api.IpTransactionService
import com.stip.stip.iptransaction.model.UnfilledOrder
import com.stip.stip.signup.utils.PreferenceUtil
import com.stip.stip.iptransaction.viewmodel.IpTransactionViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import java.text.DecimalFormat

@AndroidEntryPoint
class IpUnfilledFragment : Fragment(), ScrollableToTop {

    private lateinit var mainViewModel: com.stip.stip.MainViewModel
    private val viewModel: com.stip.stip.iptransaction.viewmodel.IpTransactionViewModel by viewModels()

    private var _binding: FragmentIpUnfilledBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: UnfilledOrderAdapter
    private var selectedOrderType: OrderType = OrderType.ALL

    enum class OrderType {
        ALL, NORMAL, RESERVED
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIpUnfilledBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[com.stip.stip.MainViewModel::class.java]
        mainViewModel.memberInfo.observe(viewLifecycleOwner) { memberInfo ->
            // TODO: 회원정보를 UI에 반영하는 코드 작성
        }

        // 모든주문 필터 UI 숨김
        binding.filterContainerOrder.visibility = View.GONE

        binding.cancelOrderButton.setOnClickListener {
            if (::adapter.isInitialized && adapter.hasCheckedItems()) {
                val selectedIds = adapter.getSelectedOrderIds()
                if (selectedIds.isEmpty()) return@setOnClickListener
                // 주문취소 확인 팝업 띄우기
                val message = if (selectedIds.size == 1) {
                    getString(R.string.dialog_message_cancel_order_confirm)
                } else {
                    getString(R.string.dialog_message_cancel_order_confirm_count, selectedIds.size)
                }
                val dialog = com.stip.stip.iphome.fragment.CancelConfirmDialogFragment.newInstance(
                    R.string.dialog_title_cancel_order_confirm,
                    message
                )
                dialog.show(parentFragmentManager, com.stip.stip.iphome.fragment.CancelConfirmDialogFragment.TAG)
                // 결과 리스너 등록
                parentFragmentManager.setFragmentResultListener(
                    com.stip.stip.iphome.fragment.CancelConfirmDialogFragment.REQUEST_KEY,
                    viewLifecycleOwner
                ) { _, bundle ->
                    val confirmed = bundle.getBoolean(com.stip.stip.iphome.fragment.CancelConfirmDialogFragment.RESULT_KEY_CONFIRMED, false)
                    if (confirmed) {
                        binding.cancelOrderButton.isEnabled = false
                        cancelOrdersSequentially(selectedIds)
                    }
                }
            } else {
                android.widget.Toast.makeText(requireContext(), "주문을 선택하세요.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        loadUnfilledOrders()

        updateOrderTypeText()
    }

    private fun loadUnfilledOrders() {
        // 로그인 여부 확인
        val memberId = PreferenceUtil.getUserId()
        if (memberId == null) {
            binding.nodatatext.text = "로그인이 필요합니다."
            binding.nodatatext.visibility = View.VISIBLE
            binding.recyclerViewUnfilled.visibility = View.GONE
            return
        }

        // 로딩 상태 표시
        // 프로그레스바가 없어 주석 처리
        // binding.progressBar.visibility = View.VISIBLE
        binding.nodatatext.visibility = View.GONE
        binding.recyclerViewUnfilled.visibility = View.GONE

        // API 호출
        IpTransactionService.getApiUnfilledOrders(
            memberId = memberId,
            page = 1,
            limit = 50
        ) { data, error ->
            requireActivity().runOnUiThread {
                // binding.progressBar.visibility = View.GONE // 프로그레스바 없음

                if (error != null) {
                    // 오류가 있어도 미체결 주문 없음 메시지 표시
                    showEmptyState()
                    return@runOnUiThread
                }

                // 데이터 표시
                if (data != null && data.isNotEmpty()) {
                    // ApiOrderResponse를 UnfilledOrder로 변환
                    val unfilledOrders = data.map { apiOrder ->
                        val tickerName = apiOrder.marketPair.symbol
                        UnfilledOrder(
                            orderId = apiOrder.id,
                            memberNumber = apiOrder.member.id,
                            ticker = tickerName,
                            tradeType = if (apiOrder.type == "buy") "매수" else "매도",
                            watchPrice = "--",
                            orderPrice = apiOrder.price.toString(),
                            orderQuantity = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }.format(apiOrder.quantity),
                            unfilledQuantity = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }.format(apiOrder.quantity - apiOrder.filledQuantity),
                            orderTime = formatOrderTime(apiOrder.createdAt),
                            status = "미체결"
                        )
                    }
                    setupUnfilledOrderList(unfilledOrders)
                } else {
                    showEmptyState()
                }
            }
        }
    }

    private fun showEmptyState() {
        binding.nodatatext.visibility = View.VISIBLE
        binding.nodatatext.text = "미체결 주문이 없습니다."
        binding.recyclerViewUnfilled.visibility = View.GONE
    }

    private fun setupUnfilledOrderList(orderList: List<UnfilledOrder>) {
        if (orderList.isEmpty()) {
            binding.recyclerViewUnfilled.visibility = View.GONE
            binding.nodatatext.visibility = View.VISIBLE
        } else {
            binding.nodatatext.visibility = View.GONE
            binding.recyclerViewUnfilled.visibility = View.VISIBLE

            adapter = UnfilledOrderAdapter(orderList)
            adapter.onSelectionChanged = { isAnySelected ->
                activity?.runOnUiThread {
                    updateCancelButtonState(isAnySelected)
                }
            }

            binding.recyclerViewUnfilled.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerViewUnfilled.adapter = adapter
        }
        updateCancelButtonState(if (::adapter.isInitialized) adapter.hasCheckedItems() else false)
    }

    private fun updateCancelButtonState(isAnySelected: Boolean) {
        if (_binding == null || !isAdded) return
        val context = requireContext()
        binding.cancelOrderButton.isEnabled = isAnySelected

        if (isAnySelected) {
            binding.cancelOrderButton.setBackgroundColor(
                ContextCompat.getColor(context, R.color.main_point_blue)
            )
            binding.cancelOrderButton.setTextColor(
                ContextCompat.getColor(context, R.color.white)
            )
        } else {
            binding.cancelOrderButton.setBackgroundColor(
                ContextCompat.getColor(context, R.color.button_disabled_grey)
            )
            binding.cancelOrderButton.setTextColor(
                ContextCompat.getColor(context, R.color.text_disabled_grey)
            )
        }
    }

    private fun showOrderTypePopup(anchorView: View) {
        val inflater = LayoutInflater.from(requireContext())
        val popupView = inflater.inflate(R.layout.order_type_popup, null)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 8f
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val allOrders = popupView.findViewById<TextView>(R.id.order_type_all)
        val normalOrders = popupView.findViewById<TextView>(R.id.order_type_normal)
        val reservedOrders = popupView.findViewById<TextView>(R.id.order_type_reserved)

        val selectedColor = ContextCompat.getColor(requireContext(), R.color.main_point_blue)
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.text_primary)

        fun applySelectedStyle() {
            allOrders.setTextColor(if (selectedOrderType == OrderType.ALL) selectedColor else defaultColor)
            normalOrders.setTextColor(if (selectedOrderType == OrderType.NORMAL) selectedColor else defaultColor)
            reservedOrders.setTextColor(if (selectedOrderType == OrderType.RESERVED) selectedColor else defaultColor)
        }

        applySelectedStyle()

        allOrders.setOnClickListener {
            selectedOrderType = OrderType.ALL
            updateOrderTypeText()
            popupWindow.dismiss()
        }
        normalOrders.setOnClickListener {
            selectedOrderType = OrderType.NORMAL
            updateOrderTypeText()
            popupWindow.dismiss()
        }
        reservedOrders.setOnClickListener {
            selectedOrderType = OrderType.RESERVED
            updateOrderTypeText()
            popupWindow.dismiss()
        }

        popupWindow.showAsDropDown(anchorView, 0, 10)
    }

    private fun updateOrderTypeText() {
        if (_binding == null || !isAdded) return
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.main_point_blue)
        binding.allOrdersText.setTextColor(selectedColor)
        binding.allOrdersText.text = when (selectedOrderType) {
            OrderType.ALL -> getString(R.string.all_orders)
            OrderType.NORMAL -> getString(R.string.order_type_normal)
            OrderType.RESERVED -> getString(R.string.order_type_reserved)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun scrollToTop() {
        if (_binding != null) {
            binding.recyclerViewUnfilled.scrollToPosition(0)
        }
    }

    private fun formatOrderTime(dateString: String): String {
        return try {
            val inputFormats = listOf(
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", java.util.Locale.getDefault()).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                },
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
            )
            val outputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
            }
            val date = inputFormats.firstNotNullOfOrNull { format ->
                try { format.parse(dateString) } catch (e: Exception) { null }
            }
            if (date != null) outputFormat.format(date) else dateString
        } catch (e: Exception) {
            dateString
        }
    }

    // 주문취소 순차 실행 함수
    private fun cancelOrdersSequentially(orderIds: List<String>) {
        if (orderIds.isEmpty()) {
            binding.cancelOrderButton.isEnabled = true
            return
        }
        
        var successCount = 0
        var failCount = 0
        var currentIndex = 0
        
        // 기존 Observer 제거
        viewModel.cancelOrderResult.removeObservers(viewLifecycleOwner)
        
        fun finishAll() {
            // UI 업데이트
            binding.cancelOrderButton.isEnabled = true
            
            // 성공한 주문이 있으면 목록 새로고침
            if (successCount > 0) {
                // 선택 상태 먼저 초기화
                if (::adapter.isInitialized) {
                    adapter.clearSelection()
                }
                // 목록 새로고침
                loadUnfilledOrders()
            } else {
                // 실패한 경우에도 선택 상태 초기화
                if (::adapter.isInitialized) {
                    adapter.clearSelection()
                }
            }
            
            // 결과 메시지 표시
            val msg = when {
                failCount == 0 -> "${orderIds.size}개 주문이 취소되었습니다."
                successCount == 0 -> "모든 주문 취소에 실패했습니다."
                else -> "${successCount}개 성공, ${failCount}개 실패"
            }
            android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_LONG).show()
        }
        
        // 단일 Observer로 모든 주문 취소 처리
        viewModel.cancelOrderResult.observe(viewLifecycleOwner) { result ->
            // 현재 주문 처리 결과 기록
            if (result.isSuccess) {
                successCount++
            } else {
                failCount++
            }
            
            // 다음 주문으로 진행
            currentIndex++
            
            if (currentIndex >= orderIds.size) {
                // 모든 주문 처리 완료
                viewModel.cancelOrderResult.removeObservers(viewLifecycleOwner)
                finishAll()
            } else {
                // 다음 주문 취소
                val nextOrderId = orderIds[currentIndex]
                viewModel.cancelOrder(nextOrderId)
            }
        }
        
        // 첫 번째 주문 취소 시작
        val firstOrderId = orderIds[currentIndex]
        viewModel.cancelOrder(firstOrderId)
    }

    companion object {
        @JvmStatic
        fun newInstance() = IpUnfilledFragment()
    }
}