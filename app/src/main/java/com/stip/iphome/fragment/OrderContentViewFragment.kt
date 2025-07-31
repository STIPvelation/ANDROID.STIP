package com.stip.stip.iphome.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.stip.stip.R
import com.stip.stip.databinding.FragmentOrderContentBinding
import com.stip.stip.order.coordinator.OrderDataCoordinator
import com.stip.stip.order.OrderUIInitializer
import com.stip.stip.order.OrderUIStateManager
import com.stip.stip.order.OrderHistoryManager
import com.stip.stip.order.adapter.OrderBookAdapter
import com.stip.stip.order.book.OrderBookManager
import com.stip.stip.iphome.TradingDataHolder
import com.stip.stip.iphome.adapter.UnfilledOrderAdapter
import com.stip.stip.iphome.util.OrderUtils
import com.stip.stip.order.adapter.FilledOrderAdapter
import com.stip.stip.order.button.OrderButtonHandler
import com.stip.stip.order.OrderValidator
import com.stip.stip.signup.utils.PreferenceUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import android.widget.Toast
import com.stip.order.api.OrderService
import com.stip.stip.api.RetrofitClient
import com.stip.stip.iphome.fragment.CancelConfirmDialogFragment
import com.stip.stip.order.OrderInputHandler
import com.stip.api.repository.PortfolioRepository
import androidx.lifecycle.lifecycleScope
import java.math.BigDecimal

interface OnOrderBookItemClickListener {
    fun onPriceClicked(price: String)
}

class OrderContentViewFragment : Fragment(), OnOrderBookItemClickListener {
    private var _binding: FragmentOrderContentBinding? = null
    private val binding get() = _binding!!

    private lateinit var orderDataCoordinator: OrderDataCoordinator
    private lateinit var uiInitializer: OrderUIInitializer
    private lateinit var uiStateManager: OrderUIStateManager
    private lateinit var historyManager: OrderHistoryManager
    private lateinit var orderBookAdapter: OrderBookAdapter
    private lateinit var orderBookManager: OrderBookManager
    private lateinit var filledOrderAdapter: FilledOrderAdapter
    private lateinit var unfilledOrderAdapter: UnfilledOrderAdapter
    private lateinit var validator: OrderValidator
    private lateinit var orderButtonHandler: OrderButtonHandler
    private lateinit var orderInputHandler: OrderInputHandler
    private var initialTicker: String? = null
    private val orderService: OrderService = RetrofitClient.createOrderService()
    private val portfolioRepository = PortfolioRepository()
    private val priceFormatter = DecimalFormat("#,##0.00")
    
    // 주문 취소 로딩 상태 관리
    private var isCancellingOrders = false

    companion object {
        private const val TAG = "OrderContentViewFragment"
        private const val ARG_TICKER = "ticker"
        
        // 전역 OrderDataCoordinator 인스턴스 (잔액 새로고침용)
        private var globalOrderDataCoordinator: OrderDataCoordinator? = null
        
        fun setGlobalOrderDataCoordinator(coordinator: OrderDataCoordinator) {
            globalOrderDataCoordinator = coordinator
        }
        
        fun getGlobalOrderDataCoordinator(): OrderDataCoordinator? {
            return globalOrderDataCoordinator
        }
        
        fun newInstance(ticker: String?): OrderContentViewFragment {
            return OrderContentViewFragment().apply {
                arguments = Bundle().apply { putString(ARG_TICKER, ticker) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { initialTicker = it.getString(ARG_TICKER) }
        // PreferenceUtil 초기화
        PreferenceUtil.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            // Initialize Core Logic
            orderDataCoordinator = OrderDataCoordinator(initialTicker)
            
            // 전역 인스턴스로 설정 (다른 Fragment에서 잔액 새로고침용)
            setGlobalOrderDataCoordinator(orderDataCoordinator)
            
            // 잔액 업데이트 콜백 설정
            orderDataCoordinator.setOnBalanceUpdated {
                // UI 업데이트를 메인 스레드에서 실행
                requireActivity().runOnUiThread {
                    try {
                        // 주문 가능 금액 업데이트
                        orderInputHandler.updateOrderAvailableDisplay()
                        // 거래 정보 업데이트
                        orderInputHandler.updateTradingInfoContent()
                        // 주문 버튼 상태 업데이트
                        orderButtonHandler.updateOrderButtonStates()
                        // 포트폴리오 데이터 새로고침
                        loadPortfolioData()
                        
                        // 실시간 시세 동기화를 위한 호가창 업데이트
                        orderBookManager.triggerManualUpdate()
                    } catch (e: Exception) {
                        Log.e(TAG, "잔액 업데이트 중 UI 오류", e)
                    }
                }
            }

            // Initialize Adapters
            filledOrderAdapter = FilledOrderAdapter()
            unfilledOrderAdapter = UnfilledOrderAdapter()

            // Initialize UI Components
            uiInitializer = OrderUIInitializer(requireContext(), binding)
            historyManager = OrderHistoryManager(
                context = requireContext(),
                binding = binding,
                unfilledAdapter = unfilledOrderAdapter,
                filledAdapter = filledOrderAdapter,
                fragmentManager = parentFragmentManager,
                coroutineScope = CoroutineScope(Dispatchers.Main),
                orderDataCoordinator = orderDataCoordinator
            )

            // Setup adapter callbacks
            unfilledOrderAdapter.onSelectionChanged = { hasSelection ->
                historyManager.updateCancelButtonState(hasSelection)
            }

            validator = OrderValidator(
                context = requireContext(),
                binding = binding,
                getCurrentPrice = { orderDataCoordinator.currentPrice },
                getCurrentTicker = { orderDataCoordinator.currentTicker },
                availableUsdBalance = { orderDataCoordinator.availableUsdBalance },
                heldAssetQuantity = { orderDataCoordinator.heldAssetQuantity },
                feeRate = 0.0, // 수수료 추가 가능
                minimumOrderValue = 10.0,
                numberParseFormat = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN },
                fixedTwoDecimalFormatter = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN },
                showToast = { msg -> OrderUtils.showToast(requireContext(), msg) },
                showErrorDialog = { titleRes, message, colorRes -> OrderUtils.showErrorDialog(parentFragmentManager, titleRes, message, colorRes) },
                getCurrentPairId = { 
                    TradingDataHolder.ipListingItems
                        .find { it.ticker == orderDataCoordinator.currentTicker }?.registrationNumber 
                } // 최신 시장가 조회를 위한 pairId 추가
            )

            // OrderInputHandler 초기화
            orderInputHandler = OrderInputHandler(
                context = requireContext(),
                binding = binding,
                numberParseFormat = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN },
                fixedTwoDecimalFormatter = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN },
                getCurrentPrice = { orderDataCoordinator.currentPrice.toDouble() },
                getFeeRate = { 0.001 }, // 수수료 0.1%
                availableUsdBalance = { orderDataCoordinator.availableUsdBalance },
                heldAssetQuantity = { orderDataCoordinator.heldAssetQuantity },
                getCurrentTicker = { orderDataCoordinator.currentTicker },
                getCurrentOrderType = { binding.radioGroupOrderType.checkedRadioButtonId },
                getHeldAssetEvalAmount = { orderDataCoordinator.getActualSellableEvalAmount() }
            )

            orderButtonHandler = OrderButtonHandler(
                context = requireContext(),
                binding = binding,
                validator = validator,
                numberParseFormat = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN },
                fixedTwoDecimalFormatter = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN },
                getCurrentPrice = { orderDataCoordinator.currentPrice },
                getFeeRate = { 0.001 }, // 수수료 0.1%
                currentTicker = { orderDataCoordinator.currentTicker },
                minimumOrderValue = 10.0,
                availableUsdBalance = { orderDataCoordinator.availableUsdBalance },
                heldAssetQuantity = { orderDataCoordinator.heldAssetQuantity },
                showToast = { msg -> OrderUtils.showToast(requireContext(), msg) },
                showErrorDialog = { titleRes, message, colorRes -> OrderUtils.showErrorDialog(parentFragmentManager, titleRes, message, colorRes) },
                parentFragmentManager = parentFragmentManager,
                getCurrentPairId = { 
                    TradingDataHolder.ipListingItems
                        .find { it.ticker == orderDataCoordinator.currentTicker }?.registrationNumber 
                },
                orderDataCoordinator = orderDataCoordinator,
                orderInputHandler = orderInputHandler,
                onOrderSuccess = {
                    // 주문 성공 시 호가창 즉시 업데이트
                    orderBookManager.triggerManualUpdate()
                    Log.d(TAG, "주문 성공 후 호가창 업데이트 트리거")
                }
            )

            uiStateManager = OrderUIStateManager(
                requireContext(),
                binding,
                orderDataCoordinator,
                historyManager,
                uiInitializer
            )

            // Setup UI Components
            uiInitializer.setupTabLayoutColors { position -> uiStateManager.handleTabSelection(position) }
            
            // OrderInputHandler 설정
            orderInputHandler.setupInputListeners()
            orderInputHandler.setupPriceAdjustmentButtons()
            orderInputHandler.setupQuantitySpinner()
            orderInputHandler.setupResetButton()
            
            // 초기 UI 상태 설정
            orderInputHandler.updateUiForOrderTypeChange()
            
            // 주문 유형 라디오 버튼 리스너 설정 (시장 주문 시 가격 필드 비활성화 등)
            uiInitializer.setupRadioGroupListener(
                currentTicker = orderDataCoordinator.currentTicker,
                resetOrderInputsToZero = {
                    // 입력값 초기화 로직
                    binding.editTextLimitPrice.setText("")
                    binding.editTextQuantity.setText("")
                    binding.editTextTriggerPrice?.setText("")
                },
                updateInputHandlerUI = {
                    // OrderInputHandler의 UI 업데이트 메서드 호출
                    orderInputHandler.updateUiForOrderTypeChange()
                }
            )
            binding.tabLayoutOrderMode.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                    tab?.let { 
                        // 탭 변경 시 주문 유형을 지정가로 초기화
                        binding.radioGroupOrderType.check(R.id.radio_limit_order)
                        
                        // 입력값 초기화
                        binding.editTextLimitPrice.setText("")
                        binding.editTextQuantity.setText("")
                        binding.editTextTriggerPrice?.setText("")
                        
                        // 퍼센트 스피너 초기화 ("가능"으로 리셋)
                        try {
                            if (binding.spinnerAvailableQuantity.adapter?.count ?: 0 > 0) {
                                binding.spinnerAvailableQuantity.setSelection(0, false)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "퍼센트 스피너 초기화 에러", e)
                        }
                        
                        uiStateManager.handleTabSelection(it.position)
                        
                        // 탭 변경 시 API 데이터 새로고침 후 UI 업데이트
                        orderDataCoordinator.refreshBalance()
                        
                        // 탭 변경 시 OrderInputHandler UI 업데이트
                        orderInputHandler.updateUiForOrderTypeChange()
                        orderInputHandler.updateOrderAvailableDisplay()
                        
                        // 탭에 따른 정보 표시 업데이트
                        updateTradingInfoByTab(it.position)
                        
                        Log.d(TAG, "탭 전환 시 API 데이터 새로고침 완료: position=${it.position}")
                    }
                }
                override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
                override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            })

            // Setup OrderBook
            setupOrderBook()
            setupOrderBookManager()

            // Start OrderBook updates
            orderBookManager.initializeAndStart()
            orderBookManager.startAutoUpdate()
            
            // 주기적 주문가능 금액 업데이트 시작
            startPeriodicBalanceUpdate()

            // Initialize UI State
            val initialTabPosition = binding.tabLayoutOrderMode.selectedTabPosition
            
            // 매수 탭으로 초기화
            if (initialTabPosition != 0) {
                binding.tabLayoutOrderMode.selectTab(binding.tabLayoutOrderMode.getTabAt(0))
            }
            
            // UI 상태 초기화
            uiStateManager.handleTabSelection(0, true)
            
            // 내역 탭이 아닌 경우 숨김
            historyManager.hide()

            // 버튼 핸들러 초기화
            orderButtonHandler.setupOrderButtonClickListeners()
            
            // 주문 취소 확인 결과 리스너 설정
            setupCancelConfirmResultListener()
            
            // 포트폴리오 데이터 로드
            loadPortfolioData()
            
            // 초기 탭 상태에 맞는 정보 표시
            updateTradingInfoByTab(initialTabPosition)

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing OrderContentViewFragment", e)
        }
    }



    private fun setupOrderBook() {
        try {
            orderBookAdapter = OrderBookAdapter(orderDataCoordinator.currentPrice, this)
            binding.recyclerOrderBook.apply {
                layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
                adapter = orderBookAdapter
                setHasFixedSize(true)
                itemAnimator = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupOrderBook", e)
        }
    }

    private fun setupOrderBookManager() {
        try {
            val numberParseFormat = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }
            val fixedTwoDecimalFormatter = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }

            orderBookManager = OrderBookManager(
                context = requireContext(),
                recyclerView = binding.recyclerOrderBook,
                orderBookAdapter = orderBookAdapter,
                numberParseFormat = numberParseFormat,
                fixedTwoDecimalFormatter = fixedTwoDecimalFormatter,
                getCurrentPrice = { orderDataCoordinator.currentPrice },
                binding = binding,
                getCurrentPairId = { 
                    TradingDataHolder.ipListingItems
                        .find { it.ticker == orderDataCoordinator.currentTicker }?.registrationNumber 
                }
            ).also { manager ->
                manager.setupBottomOptionListeners()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupOrderBookManager", e)
        }
    }

    fun updateTicker(ticker: String?) {
        Log.d(TAG, "updateTicker called with: $ticker")
        initialTicker = ticker
        
        if (_binding == null || !isAdded) {
            Log.w(TAG, "updateTicker called but Fragment view is not available.")
            return
        }

        try {
            orderDataCoordinator.updateTicker(ticker)
            val currentPrice = orderDataCoordinator.currentPrice
            Log.d(TAG, "🔄 Updating OrderBook for ticker: $ticker, price: $currentPrice")
            orderBookManager.updateCurrentPrice(currentPrice)
            orderBookManager.initializeAndStart()
            
            // 포트폴리오 데이터 새로고침
            loadPortfolioData()
            
            // 티커 변경 시 즉시 주문가능 금액 업데이트
            orderDataCoordinator.refreshBalance()
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateTicker", e)
        }
    }
    
    override fun onResume() {
        super.onResume()
        try {
            // 화면 복귀 시 호가창 업데이트 재시작
            orderBookManager.startAutoUpdate()
            
            // 화면 복귀 시 즉시 주문가능 금액 업데이트
            orderDataCoordinator.refreshBalance()
            
            // 주기적 업데이트 재시작
            startPeriodicBalanceUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume", e)
        }
    }
    
    override fun onPause() {
        super.onPause()
        try {
            // 주기적 업데이트 중지
            stopPeriodicBalanceUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onPause", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            orderBookManager.release()
            binding.recyclerOrderBook.adapter = null
            binding.recyclerViewUnfilledOrders.adapter = null
            binding.recyclerViewFilledOrders.adapter = null
            
            // 주기적 업데이트 중지
            stopPeriodicBalanceUpdate()
            
            _binding = null
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroyView", e)
        }
    }

    override fun onPriceClicked(price: String) {
        try {
            Log.d(TAG, "Price clicked: $price")
            // 주문창에 가격 자동 반영
            orderInputHandler.handleOrderBookPriceClick(price)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onPriceClicked", e)
        }
    }
    
    /**
     * 주문 취소 확인 다이얼로그 결과 처리 리스너 설정
     */
    private fun setupCancelConfirmResultListener() {
        Log.d(TAG, "취소 확인 다이얼로그 결과 리스너 설정 완료")
        
        parentFragmentManager.setFragmentResultListener(
            CancelConfirmDialogFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { requestKey, bundle ->
            Log.d(TAG, "다이얼로그 결과 수신: requestKey=$requestKey")
            
            val confirmed = bundle.getBoolean(CancelConfirmDialogFragment.RESULT_KEY_CONFIRMED, false)
            Log.d(TAG, "사용자 선택: confirmed=$confirmed")
            
            if (confirmed) {
                Log.d(TAG, "사용자가 주문 취소를 확인했습니다.")
                
                val selectedOrderIds = unfilledOrderAdapter.getSelectedOrderIds()
                Log.d(TAG, "취소할 주문 ID들: $selectedOrderIds")
                
                if (selectedOrderIds.isEmpty()) {
                    Log.w(TAG, "취소할 주문이 없습니다.")
                    Toast.makeText(context, "취소할 주문이 없습니다.", Toast.LENGTH_SHORT).show()
                    return@setFragmentResultListener
                }

                // 주문 취소 시작 - 로딩 상태 활성화
                startOrderCancellation(selectedOrderIds)

            } else {
                Log.d(TAG, "사용자가 주문 취소를 거부했습니다.")
            }
        }
    }
    
    /**
     * 주문 취소 프로세스 시작
     */
    private fun startOrderCancellation(orderIds: List<String>) {
        if (isCancellingOrders) {
            Log.w(TAG, "이미 주문 취소가 진행 중입니다.")
            return
        }
        
        isCancellingOrders = true
        
        // UI 상태 초기화 및 로딩 상태 표시
        unfilledOrderAdapter.clearSelection()
        unfilledOrderAdapter.setCancellingOrdersState(true)
        historyManager.setCancellingOrdersState(true)
        showCancellationLoading(true)
        
        // 실제 주문 삭제 API 호출
        Log.d(TAG, "주문 삭제 API 호출 시작: $orderIds")
        deleteOrdersBatch(orderIds)
    }
    
    /**
     * 주문 취소 로딩 상태 표시/숨김
     */
    private fun showCancellationLoading(show: Boolean) {
        try {
            if (show) {
                // 로딩 중일 때 UI 비활성화
                binding.unfilledFilledBoxRoot.isEnabled = false
                
                // 로딩 인디케이터 표시
                binding.progressBarCancelOrders.visibility = View.VISIBLE
                
                Log.d(TAG, "주문 취소 로딩 상태 활성화")
            } else {
                // 로딩 완료 시 UI 활성화
                binding.unfilledFilledBoxRoot.isEnabled = true
                
                // 로딩 인디케이터 숨김
                binding.progressBarCancelOrders.visibility = View.GONE
                
                Log.d(TAG, "주문 취소 로딩 상태 비활성화")
            }
        } catch (e: Exception) {
            Log.e(TAG, "로딩 상태 표시 중 오류", e)
        }
    }
    
    /**
     * 주문들을 일괄 삭제합니다.
     * @param orderIds 삭제할 주문 ID 리스트
     */
    private fun deleteOrdersBatch(orderIds: List<String>) {
        if (orderIds.isEmpty()) {
            Log.w(TAG, "삭제할 주문 ID가 없습니다.")
            isCancellingOrders = false
            unfilledOrderAdapter.setCancellingOrdersState(false)
            historyManager.setCancellingOrdersState(false)
            showCancellationLoading(false)
            return
        }

        // 새로운 다중 주문 취소 API 사용
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "다중 주문 삭제 API 호출: $orderIds")
                val orderCancelRequest = com.stip.stip.order.data.OrderCancelRequest(orderIds = orderIds)
                val response = orderService.cancelOrders(orderCancelRequest)
                
                val totalCount = orderIds.size
                val resultMessage: String
                val isSuccess: Boolean
                
                if (response.isSuccessful) {
                    val deleteResponse = response.body()
                    if (deleteResponse?.success == true) {
                        isSuccess = true
                        resultMessage = "${totalCount}개 주문이 취소되었습니다."
                        Log.d(TAG, "다중 주문 삭제 성공 - ${deleteResponse.message}")
                    } else {
                        isSuccess = false
                        resultMessage = "주문 취소에 실패했습니다: ${deleteResponse?.message ?: "알 수 없는 오류"}"
                        Log.e(TAG, "다중 주문 삭제 실패 - ${deleteResponse?.message ?: "알 수 없는 오류"}")
                    }
                } else {
                    isSuccess = false
                    resultMessage = "주문 취소에 실패했습니다: HTTP ${response.code()}"
                    Log.e(TAG, "다중 주문 삭제 HTTP 오류 - ${response.code()}: ${response.message()}")
                }
                
                // UI 업데이트는 메인 스레드에서
                CoroutineScope(Dispatchers.Main).launch {
                    // 로딩 상태 비활성화
                    isCancellingOrders = false
                    unfilledOrderAdapter.setCancellingOrdersState(false)
                    historyManager.setCancellingOrdersState(false)
                    showCancellationLoading(false)
                    
                    Toast.makeText(context, resultMessage, Toast.LENGTH_LONG).show()
                    
                    // 주문 취소 완료 후 잔액 새로고침 (성공/실패 관계없이)
                    orderDataCoordinator.refreshBalance()
                    
                    // 성공한 경우에만 목록 새로고침
                    if (isSuccess) {
                        refreshUnfilledOrdersOnce()
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "다중 주문 삭제 예외 발생", e)
                
                // UI 업데이트는 메인 스레드에서
                CoroutineScope(Dispatchers.Main).launch {
                    // 로딩 상태 비활성화
                    isCancellingOrders = false
                    unfilledOrderAdapter.setCancellingOrdersState(false)
                    historyManager.setCancellingOrdersState(false)
                    showCancellationLoading(false)
                    
                    Toast.makeText(context, "주문 취소 중 오류가 발생했습니다", Toast.LENGTH_LONG).show()
                    
                    // 오류 발생 시에도 잔액 새로고침
                    orderDataCoordinator.refreshBalance()
                }
            }
        }
    }
    
    /**
     * 미체결 주문 목록을 강제로 새로고침합니다.
     */
    private fun refreshUnfilledOrders() {
        try {
            Log.d(TAG, "미체결 주문 목록 강제 새로고침 시작")
            
            // 1. 현재 미체결 탭이 선택되어 있는지 확인
            if (historyManager.isUnfilledTabSelected) {
                Log.d(TAG, "미체결 탭이 선택되어 있음 - 강제 새로고침")
                
                // 2. 히스토리 매니저의 강제 새로고침 메서드 호출
                historyManager.forceRefreshUnfilledOrders()
                
            } else {
                Log.d(TAG, "미체결 탭이 선택되지 않음 - 탭 전환 후 새로고침")
                // 미체결 탭으로 전환하고 강제 새로고침
                historyManager.handleTabClick(true)
                binding.root.postDelayed({
                    historyManager.forceRefreshUnfilledOrders()
                }, 300)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "미체결 주문 목록 새로고침 중 오류 발생", e)
        }
    }
    
    /**
     * 미체결 주문 취소 후 새로고침
     */
    private fun refreshUnfilledOrdersOnce() {
        try {
            // 현재 미체결 탭이 선택되어 있는지 확인
            if (historyManager.isUnfilledTabSelected) {
                
                // 히스토리 매니저의 새로고침 메서드 호출
                historyManager.refreshUnfilledOrders()
                
            } else {
                // 미체결 탭으로 전환하고 새로고침
                historyManager.handleTabClick(true)
                binding.root.postDelayed({
                    historyManager.refreshUnfilledOrders()
                }, 300)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "주문 취소 중 에러", e)
        }
    }
    
    /**
     * 주문 삭제 후 즉시 새로고침
     */
    private fun refreshUnfilledOrdersImmediately() {
        try {
            // 현재 미체결 탭이 선택되어 있는지 확인
            if (historyManager.isUnfilledTabSelected) {
                // 즉시 새로고침
                historyManager.refreshUnfilledOrders()
            } else {
                // 미체결 탭으로 전환하고 즉시 새로고침
                historyManager.handleTabClick(true)
                binding.root.postDelayed({
                    historyManager.refreshUnfilledOrders()
                }, 100)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "즉시 새로고침 중 에러", e)
        }
    }

    /**
     * 포트폴리오 데이터 로드
     */
    private fun loadPortfolioData() {
        val userId = PreferenceUtil.getUserId()
        if (userId.isNullOrEmpty()) {
            Log.w(TAG, "사용자 ID가 없어서 포트폴리오 데이터를 로드할 수 없습니다")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "포트폴리오 데이터 로드 시작: userId=$userId")
                
                val portfolioResponse = portfolioRepository.getPortfolioResponse(userId)
                
                if (portfolioResponse != null) {
                    // 현재 티커에 해당하는 포트폴리오 데이터 찾기
                    val currentTickerWallet = portfolioResponse.wallets.firstOrNull { wallet ->
                        wallet.symbol.equals(initialTicker, ignoreCase = true)
                    }
                    
                    // UI 업데이트 (메인 스레드에서 실행)
                    updatePortfolioUI(currentTickerWallet)
                    
                } else {
                    Log.w(TAG, "포트폴리오 데이터가 null입니다")
                    hidePortfolioInfo()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "포트폴리오 데이터 로드 중 오류 발생", e)
                hidePortfolioInfo()
            }
        }
    }

    // 주기적 주문가능 금액 업데이트를 위한 변수들
    private var balanceUpdateHandler: Handler? = null
    private var balanceUpdateRunnable: Runnable? = null
    private val balanceUpdateInterval = 5000L
    
    /**
     * 주기적 주문가능 금액 업데이트 시작
     */
    private fun startPeriodicBalanceUpdate() {
        stopPeriodicBalanceUpdate()
        
        balanceUpdateHandler = Handler(Looper.getMainLooper())
        balanceUpdateRunnable = object : Runnable {
            override fun run() {
                try {
                    Log.d(TAG, "주기적 주문가능 금액 업데이트 시작")
                    orderDataCoordinator.refreshBalance()
                    balanceUpdateHandler?.postDelayed(this, balanceUpdateInterval)
                } catch (e: Exception) {
                    Log.e(TAG, "주기적 주문가능 금액 업데이트 실패", e)
                    balanceUpdateHandler?.postDelayed(this, balanceUpdateInterval)
                }
            }
        }
        balanceUpdateHandler?.post(balanceUpdateRunnable!!)
        Log.d(TAG, "주기적 주문가능 금액 업데이트 시작됨 - 10초 간격")
    }
    
    /**
     * 주기적 주문가능 금액 업데이트 중지
     */
    private fun stopPeriodicBalanceUpdate() {
        balanceUpdateRunnable?.let { runnable ->
            balanceUpdateHandler?.removeCallbacks(runnable)
        }
        balanceUpdateHandler = null
        balanceUpdateRunnable = null
        Log.d(TAG, "주기적 주문가능 금액 업데이트 중지됨")
    }
    
    /**
     * 포트폴리오 UI 업데이트
     */
    private fun updatePortfolioUI(walletData: com.stip.api.model.PortfolioWalletItemDto?) {
        if (_binding == null || !isAdded) return
        
        try {
            val tradingInfoView = binding.tradingInfoView
            val currentTabPosition = binding.tabLayoutOrderMode.selectedTabPosition
            
            if (walletData != null && walletData.balance > BigDecimal.ZERO) {
                // 포트폴리오 데이터가 있는 경우
                
                // 보유자산 (USD) - 빈 값으로 처리
                tradingInfoView.textValueAssetsHeld.text = ""
                
                // 매수평균가 (buyAvgPrice)
                tradingInfoView.textValueAvgBuyPrice.text = formatPrice(walletData.buyAvgPrice)
                
                // 평가금액 (evalAmount)
                tradingInfoView.textValueValuationAmount.text = formatPrice(walletData.evalAmount)
                
                // 평가손익 (profit)
                val profit = walletData.profit
                tradingInfoView.textValueValuationPl.text = formatPrice(profit)
                
                // 수익률 (profitRate)
                val profitRate = walletData.profitRate
                tradingInfoView.textValueRateOfReturn.text = formatPercentage(profitRate)
                
                // 손익에 따른 색상 설정
                val context = requireContext()
                val profitColor = when {
                    profit > BigDecimal.ZERO -> androidx.core.content.ContextCompat.getColor(context, R.color.percentage_positive_red)
                    profit < BigDecimal.ZERO -> androidx.core.content.ContextCompat.getColor(context, R.color.percentage_negative_blue)
                    else -> androidx.core.content.ContextCompat.getColor(context, R.color.text_primary)
                }
                
                tradingInfoView.textValueValuationPl.setTextColor(profitColor)
                tradingInfoView.textValueRateOfReturn.setTextColor(profitColor)
                
                // 매도 탭일 때만 포트폴리오 정보 표시
                if (currentTabPosition == 1) {
                    tradingInfoView.groupValuationInfo.visibility = View.VISIBLE
                }
                
                Log.d(TAG, "포트폴리오 UI 업데이트 완료: ${walletData.symbol}, balance=${walletData.balance}, profit=${profit}")
                
            } else {
                // 보유량이 없는 경우 정보 숨김
                hidePortfolioInfo()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "포트폴리오 UI 업데이트 중 오류 발생", e)
            hidePortfolioInfo()
        }
    }

    /**
     * 포트폴리오 정보 숨김
     */
    private fun hidePortfolioInfo() {
        if (_binding == null || !isAdded) return
        
        try {
            val tradingInfoView = binding.tradingInfoView
            tradingInfoView.groupValuationInfo.visibility = View.GONE
            
            Log.d(TAG, "포트폴리오 정보 숨김 처리")
            
        } catch (e: Exception) {
            Log.e(TAG, "포트폴리오 정보 숨김 처리 중 오류 발생", e)
        }
    }

    /**
     * 가격 포맷팅
     */
    private fun formatPrice(amount: BigDecimal): String {
        return try {
            "$" + priceFormatter.format(amount)
        } catch (e: Exception) {
            "$0.00"
        }
    }

    /**
     * 퍼센트 포맷팅
     */
    private fun formatPercentage(rate: BigDecimal): String {
        return try {
            String.format("%.2f%%", rate)
        } catch (e: Exception) {
            "0.00%"
        }
    }

    /**
     * 탭에 따른 거래 정보 표시 업데이트
     */
    private fun updateTradingInfoByTab(tabPosition: Int) {
        if (_binding == null || !isAdded) return
        
        try {
            val tradingInfoView = binding.tradingInfoView
            
            when (tabPosition) {
                0 -> { // 매수 탭
                    // 주문 정보 표시 (최소주문금액, 수수료)
                    tradingInfoView.groupOrderInfo.visibility = View.VISIBLE
                    // 포트폴리오 정보 숨김
                    tradingInfoView.groupValuationInfo.visibility = View.GONE
                    // 보유자산은 빈 값으로 설정
                    tradingInfoView.textValueAssetsHeld.text = ""
                    
                    Log.d(TAG, "매수 탭: 주문 정보 표시")
                }
                1 -> { // 매도 탭
                    // 주문 정보와 포트폴리오 정보 모두 표시
                    tradingInfoView.groupOrderInfo.visibility = View.VISIBLE
                    // 포트폴리오 정보 표시 (loadPortfolioData에서 처리)
                    loadPortfolioData()
                    
                    Log.d(TAG, "매도 탭: 포트폴리오 정보와 주문 정보 모두 표시")
                }
                2 -> { // 내역 탭
                    // 둘 다 숨김
                    tradingInfoView.groupOrderInfo.visibility = View.GONE
                    tradingInfoView.groupValuationInfo.visibility = View.GONE
                    // 보유자산은 빈 값으로 설정
                    tradingInfoView.textValueAssetsHeld.text = ""
                    
                    Log.d(TAG, "내역 탭: 모든 정보 숨김")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "탭별 거래 정보 업데이트 중 오류 발생", e)
        }
    }

} 