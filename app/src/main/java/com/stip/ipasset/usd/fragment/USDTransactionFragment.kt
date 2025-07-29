package com.stip.ipasset.usd.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.stip.stip.NavGraphIpAssetDirections
import androidx.recyclerview.widget.LinearLayoutManager
import com.stip.ipasset.usd.model.USDReturnTransaction
import com.stip.ipasset.usd.model.USDProcessTransaction
import com.stip.ipasset.usd.adapter.USDTransactionAdapter
// Removed dummy data import
import com.stip.ipasset.usd.manager.USDAssetManager
import com.stip.ipasset.usd.model.TransactionStatus
import com.stip.ipasset.usd.model.TransactionType
import com.stip.ipasset.usd.model.USDDepositTransaction
import com.stip.ipasset.usd.model.USDTransaction
import com.stip.ipasset.usd.model.USDWithdrawalTransaction
import com.stip.api.repository.PortfolioRepository
import com.stip.stip.signup.utils.PreferenceUtil
import com.stip.stip.R
import com.stip.stip.databinding.FragmentIpAssetUsdTransactionBinding
import java.text.NumberFormat
import java.text.DecimalFormat
import java.util.Locale
import kotlinx.coroutines.launch

class USDTransactionFragment : Fragment() {
    
    companion object {
        private const val TAG = "USDTransactionFragment"
    }
    
    private var _binding: FragmentIpAssetUsdTransactionBinding? = null
    private val binding get() = _binding!!
    
    // USD 자산 매니저 (트랜잭션용)
    private val assetManager = USDAssetManager.getInstance()
    
    // 포트폴리오 API
    private val portfolioRepository = PortfolioRepository()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIpAssetUsdTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 진입 시 액티비티의 메인 헤더는 숨기기 (이 프래그먼트가 자체 헤더를 표시하므로)
        val headerLayout = requireActivity().findViewById<View>(R.id.headerLayout)
        headerLayout?.visibility = View.GONE
        
        // 툴바 제목 설정 - 총 보유 표기
        binding.materialToolbar.title = "총 보유"
        
        // Set up back button using the MaterialToolbar
        binding.materialToolbar.setNavigationOnClickListener {
            // 뒤로 가기할 때 메인 화면의 헤더 표시
            headerLayout?.visibility = View.VISIBLE
            
            requireActivity().supportFragmentManager.popBackStack()
        }
        
        // 번들로 전달받은 USD 데이터 처리
        binding.currencyIconText.text = "$" // 항상 $ 기호 표시
        binding.currencyIconText.visibility = View.VISIBLE
        
        // 포트폴리오 API에서 USD 잔고 로드
        loadUsdBalanceFromPortfolio()
        
        // USD 로고 표시 설정 - 파란색 배경
        binding.currencyIconBackground.backgroundTintList = resources.getColorStateList(R.color.token_usd, null)
        
        // '입금' 버튼 클릭 시 USDDepositFragment로 이동
        binding.depositButtonContainer.setOnClickListener {
            try {
                val fragmentManager = requireActivity().supportFragmentManager
                val fragmentTransaction = fragmentManager.beginTransaction()
                
                // 애니메이션 추가
                fragmentTransaction.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                
                // 헤더는 계속 숨긴 상태 유지 (이미 USDTransactionFragment에서 숨겼음)
                // USDDepositFragment로 이동
                val depositFragment = USDDepositFragment()
                
                // USD 데이터 전달
                arguments?.let { args ->
                    depositFragment.arguments = args
                }
                
                fragmentTransaction.replace(R.id.fragment_container, depositFragment)
                fragmentTransaction.addToBackStack(null)
                fragmentTransaction.commit()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "화면 이동 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        // '출금' 버튼 클릭 시 USDWithdrawalInputFragment로 이동
        binding.withdrawButtonContainer.setOnClickListener {
            try {
                val fragmentManager = requireActivity().supportFragmentManager
                val fragmentTransaction = fragmentManager.beginTransaction()
                
                // 애니메이션 추가
                fragmentTransaction.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                
                // 헤더는 계속 숨긴 상태 유지
                // USDWithdrawalInputFragment로 이동
                val withdrawalFragment = USDWithdrawalInputFragment()
                
                // USD 데이터 전달
                arguments?.let { args ->
                    withdrawalFragment.arguments = args
                }
                
                fragmentTransaction.replace(R.id.fragment_container, withdrawalFragment)
                fragmentTransaction.addToBackStack(null)
                fragmentTransaction.commit()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "화면 이동 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 탭 필터 설정
        setupFilterTabs()
        
        // 초기 데이터 로드
        loadUSDTransactionsFromAPI()
    }
    
    override fun onResume() {
        super.onResume()
        // 화면으로 돌아올 때마다 USD 내역 새로고침
        loadUSDTransactionsFromAPI()
    }
    
    private lateinit var transactionAdapter: USDTransactionAdapter
    private var currentTransactions: List<Any> = emptyList()
    private var isLoading = false
    
    private fun setupFilterTabs() {
        // 트랜잭션 어댑터 초기화
        setupTransactionAdapter()
        
        // 모든 탭 버튼 리스트
        val tabButtons = listOf(
            binding.tabAll,
            binding.tabDeposit,
            binding.tabWithdrawal,
            binding.tabReturn,
            binding.tabInProgress
        )
        
        // 초기 활성화 탭 설정 (기본: 전체)
        setActiveTab(binding.tabAll, tabButtons)
        loadAllTransactions()
        
        // 각 탭에 클릭 리스너 설정
        binding.tabAll.setOnClickListener { 
            setActiveTab(binding.tabAll, tabButtons) 
            loadAllTransactions()
        }
        binding.tabDeposit.setOnClickListener { 
            setActiveTab(binding.tabDeposit, tabButtons) 
            // 현재 데이터가 있으면 즉시 필터링, 없으면 API 호출
            val allTransactions = assetManager.transactions.value ?: emptyList()
            if (allTransactions.isNotEmpty()) {
                loadDepositTransactions()
            } else {
                loadUSDTransactionsFromAPI()
            }
        }
        binding.tabWithdrawal.setOnClickListener { 
            setActiveTab(binding.tabWithdrawal, tabButtons) 
            // 현재 데이터가 있으면 즉시 필터링, 없으면 API 호출
            val allTransactions = assetManager.transactions.value ?: emptyList()
            if (allTransactions.isNotEmpty()) {
                loadWithdrawalTransactions()
            } else {
                loadUSDTransactionsFromAPI()
            }
        }
        binding.tabReturn.setOnClickListener { 
            setActiveTab(binding.tabReturn, tabButtons) 
            // 현재 데이터가 있으면 즉시 필터링, 없으면 API 호출
            val allTransactions = assetManager.transactions.value ?: emptyList()
            if (allTransactions.isNotEmpty()) {
                loadReturnTransactions()
            } else {
                loadUSDTransactionsFromAPI()
            }
        }
        binding.tabInProgress.setOnClickListener { 
            setActiveTab(binding.tabInProgress, tabButtons) 
            // 현재 데이터가 있으면 즉시 필터링, 없으면 API 호출
            val allTransactions = assetManager.transactions.value ?: emptyList()
            if (allTransactions.isNotEmpty()) {
                loadInProgressTransactions()
            } else {
                loadUSDTransactionsFromAPI()
            }
        }
    }
    
    private fun setupTransactionAdapter() {
        if (!isAdded || _binding == null) {
            return
        }
        
        transactionAdapter = USDTransactionAdapter { transaction ->
            // Fragment가 여전히 유효한지 확인
            if (!isAdded || _binding == null) {
                return@USDTransactionAdapter
            }
            
            // 트랜잭션 아이템 클릭 이벤트 처리
            when (transaction) {
                is USDDepositTransaction -> {
                    // 입금 완료 트랜잭션은 입금 상세 페이지로 이동
                    navigateToDepositDetail(transaction.id)
                }
                is USDWithdrawalTransaction -> {
                    // 출금 완료 트랜잭션은 출금 상세 페이지로 이동
                    navigateToWithdrawalDetail(transaction.id)
                }
                is USDReturnTransaction -> {
                    // 반환됨 트랜잭션은 토스트 메시지만 표시 (향후 구현 필요)
                    Toast.makeText(requireContext(), "반환됨 트랜잭션: ${transaction.id}", Toast.LENGTH_SHORT).show()
                }
                is USDProcessTransaction -> {
                    // 진행중 트랜잭션은 토스트 메시지만 표시 (향후 구현 필요)
                    Toast.makeText(requireContext(), "진행중 트랜잭션: ${transaction.id}", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(requireContext(), "알 수 없는 트랜잭션 유형", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        try {
            binding.recyclerViewTransactions.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = transactionAdapter
            }
        } catch (e: Exception) {
            Log.e(TAG, "RecyclerView 설정 중 오류", e)
        }
    }
    
    /**
     * USDAssetManager로부터 트랜잭션 데이터 관찰
     */
    private fun observeTransactionData() {
        assetManager.transactions.observe(viewLifecycleOwner, Observer { transactions ->
            // 현재 선택된 탭에 따라 트랜잭션 필터링하여 표시
            val currentTab = getCurrentActiveTab()
            when (currentTab) {
                binding.tabAll -> loadAllTransactions()
                binding.tabDeposit -> loadDepositTransactions()
                binding.tabWithdrawal -> loadWithdrawalTransactions()
                binding.tabReturn -> loadReturnTransactions()
                binding.tabInProgress -> loadInProgressTransactions()
                else -> loadAllTransactions()
            }
        })
    }
    
    /**
     * 포트폴리오 API에서 USD 잔고 로드
     */
    private fun loadUsdBalanceFromPortfolio() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val memberId = PreferenceUtil.getUserId()
                if (memberId.isNullOrBlank()) {
                    if (isAdded && _binding != null) {
                        Toast.makeText(requireContext(), "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                // 포트폴리오 API에서 USD 잔고 조회
                val portfolioResponse = portfolioRepository.getPortfolioResponse(memberId)
                
                if (portfolioResponse != null) {
                    // usd 잔고
                    val usdBalance = portfolioResponse.usdBalance.toDouble()
                    
                    // UI 업데이트는 메인 스레드에서 안전하게
                    if (isAdded && _binding != null && view != null) {
                        val formatter = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }
                        binding.totalHoldings.text = "${formatter.format(usdBalance)} USD"
                        
                        // KRW 환산액 표시 (API 기반 환율 변환)
                        val krwAmount = com.stip.utils.ExchangeRateManager.convertUsdToKrwWithApi(usdBalance)
                        binding.equivalentAmount.text = "≈ ${NumberFormat.getNumberInstance(Locale.US).format(krwAmount.toInt())} KRW"
                        
                        android.util.Log.d("USDTransactionFragment", "USD 잔고 로드 완료: $usdBalance USD")
                    }
                } else {
                    // API 응답이 null인 경우 0으로 표시
                    if (isAdded && _binding != null && view != null) {
                        binding.totalHoldings.text = "0.00 USD"
                        binding.equivalentAmount.text = "≈ 0 KRW"
                    }
                    android.util.Log.w("USDTransactionFragment", "포트폴리오 API 응답이 null")
                }
            } catch (e: Exception) {
                android.util.Log.e("USDTransactionFragment", "USD 잔고 로드 실패: ${e.message}", e)
                // 오류 시 0으로 표시
                if (isAdded && _binding != null && view != null) {
                    binding.totalHoldings.text = "0.00 USD"
                    binding.equivalentAmount.text = "≈ 0 KRW"
                }
            }
        }
    }
    
    /**
     * 현재 활성화된 탭 버튼 반환
     */
    private fun getCurrentActiveTab(): View? {
        val tabButtons = listOf(
            binding.tabAll,
            binding.tabDeposit,
            binding.tabWithdrawal,
            binding.tabReturn,
            binding.tabInProgress
        )
        
        return tabButtons.find { it.isSelected }
    }
    
    private fun loadAllTransactions() {
        if (!isAdded || _binding == null) {
            return
        }
        
        // 현재 로드된 데이터가 있으면 그것을 사용, 없으면 API 호출
        val allTransactions = assetManager.transactions.value ?: emptyList()
        if (allTransactions.isNotEmpty()) {
            // 이미 로드된 데이터가 있으면 필터링해서 표시
            val adapterItems = allTransactions.map { transaction -> convertToAdapterItem(transaction) }
            currentTransactions = adapterItems
            transactionAdapter.submitList(adapterItems)
            updateEmptyState(adapterItems.isEmpty())
        } else {
            // 데이터가 없으면 API에서 로드
            loadUSDTransactionsFromAPI()
        }
    }
    
    private fun loadDepositTransactions() {
        if (!isAdded || _binding == null) {
            return
        }
        
        val allTransactions = assetManager.transactions.value ?: emptyList()
        // 입금 타입이면 모든 상태 포함 (완료, 진행중, 반환)
        val depositTransactions = allTransactions
            .filter { transaction -> transaction.type == TransactionType.DEPOSIT }
        
        Log.d("USDTransactionFragment", "입금 탭 필터링: 전체 ${allTransactions.size}개 중 입금 ${depositTransactions.size}개")
        depositTransactions.forEach { transaction ->
            Log.d("USDTransactionFragment", "입금 트랜잭션: status=${transaction.status}, statusText=${getStatusText(transaction)}")
        }
        
        val adapterItems = depositTransactions.map { transaction -> convertToAdapterItem(transaction) }
        currentTransactions = adapterItems
        transactionAdapter.submitList(adapterItems)
        
        // 빈 상태 처리
        updateEmptyState(adapterItems.isEmpty())
    }
    
    private fun loadWithdrawalTransactions() {
        if (!isAdded || _binding == null) {
            return
        }
        
        val allTransactions = assetManager.transactions.value ?: emptyList()
        // 출금 타입이면 모든 상태 포함 (완료, 진행중, 반환)
        val withdrawalTransactions = allTransactions
            .filter { transaction -> transaction.type == TransactionType.WITHDRAWAL }
        
        Log.d("USDTransactionFragment", "출금 탭 필터링: 전체 ${allTransactions.size}개 중 출금 ${withdrawalTransactions.size}개")
        withdrawalTransactions.forEach { transaction ->
            Log.d("USDTransactionFragment", "출금 트랜잭션: status=${transaction.status}, statusText=${getStatusText(transaction)}")
        }
        
        val adapterItems = withdrawalTransactions.map { transaction -> convertToAdapterItem(transaction) }
        currentTransactions = adapterItems
        transactionAdapter.submitList(adapterItems)
        
        // 빈 상태 처리
        updateEmptyState(adapterItems.isEmpty())
    }
    
    private fun loadReturnTransactions() {
        if (!isAdded || _binding == null) {
            return
        }
        
        val allTransactions = assetManager.transactions.value ?: emptyList()
        val returnTransactions = allTransactions
            .filter { transaction -> transaction.status == TransactionStatus.RETURNED }
        
        Log.d("USDTransactionFragment", "반환 탭 필터링: 전체 ${allTransactions.size}개 중 반환 ${returnTransactions.size}개")
        returnTransactions.forEach { transaction ->
        Log.d("USDTransactionFragment", "반환 트랜잭션: type=${transaction.type}, status=${transaction.status}, statusText=${getStatusText(transaction)}")
        }
        
        val adapterItems = returnTransactions.map { transaction -> convertToAdapterItem(transaction) }
        currentTransactions = adapterItems
        transactionAdapter.submitList(adapterItems)
        
        // 빈 상태 처리
        updateEmptyState(adapterItems.isEmpty())
    }
    
    private fun loadInProgressTransactions() {
        if (!isAdded || _binding == null) {
            return
        }
        
        val allTransactions = assetManager.transactions.value ?: emptyList()
        val inProgressTransactions = allTransactions
            .filter { transaction -> transaction.status == TransactionStatus.IN_PROGRESS }
        
        Log.d("USDTransactionFragment", "진행중 탭 필터링: 전체 ${allTransactions.size}개 중 진행중 ${inProgressTransactions.size}개")
        inProgressTransactions.forEach { transaction ->
        Log.d("USDTransactionFragment", "진행중 트랜잭션: type=${transaction.type}, status=${transaction.status}, statusText=${getStatusText(transaction)}")
        }
        
        val adapterItems = inProgressTransactions.map { transaction -> convertToAdapterItem(transaction) }
        currentTransactions = adapterItems
        transactionAdapter.submitList(adapterItems)
        
        // 빈 상태 처리
        updateEmptyState(adapterItems.isEmpty())
    }
    
    /**
     * API에서 USD 입출금 내역 가져오기
     */
    private fun loadUSDTransactionsFromAPI() {
        // 이미 로딩 중이면 중복 호출 방지
        if (isLoading) {
            return
        }
        
        isLoading = true
        
        // 로딩 상태 표시
        if (isAdded && _binding != null) {
            binding.recyclerViewTransactions.visibility = View.GONE
            binding.emptyStateContainer.visibility = View.GONE
            // 로딩 인디케이터가 있다면 표시
            // binding.loadingIndicator.visibility = View.VISIBLE
        }
        
        lifecycleScope.launch {
            try {
                // 토큰 가져오기
                val token = PreferenceUtil.getToken()
                if (token.isNullOrBlank()) {
                    android.util.Log.w("USDTransactionFragment", "토큰이 없습니다.")
                    return@launch
                }
                
                // USD marketPairId 가져오기 - 포트폴리오 API에서 가져오기
                val memberId = PreferenceUtil.getUserId()
                if (memberId.isNullOrBlank()) {
                    android.util.Log.w("USDTransactionFragment", "사용자 ID가 없습니다.")
                    return@launch
                }
                
                val portfolioResponse = portfolioRepository.getPortfolioResponse(memberId)
                val usdWallet = portfolioResponse?.wallets?.find { it.symbol.equals("USD", ignoreCase = true) }
                val marketPairId = usdWallet?.marketPairId
                
                if (marketPairId == null) {
                    android.util.Log.w("USDTransactionFragment", "포트폴리오에서 USD marketPairId를 찾을 수 없습니다.")
                    return@launch
                }
                
                android.util.Log.d("USDTransactionFragment", "USD marketPairId 찾음: $marketPairId")
                
                // Wallet History API 호출
                val walletHistoryRepository = com.stip.api.repository.WalletHistoryRepository()
                val history = walletHistoryRepository.getWalletHistory(
                    authorization = "Bearer $token",
                    marketPairId = marketPairId
                )
                
                android.util.Log.d("USDTransactionFragment", "API에서 가져온 USD 내역: ${history.size}개")
                
                // API 응답 디버깅 로그
                history.forEachIndexed { index, record ->
                    android.util.Log.d("USDTransactionFragment", "Record $index: type=${record.type}, status=${record.status}, amount=${record.amount}")
                }
                
                // API 데이터를 USDTransaction 형식으로 변환
                val usdTransactions = history.mapNotNull { record ->
                    try {
                        val transactionType = when (record.type.uppercase()) {
                            "DEPOSIT" -> TransactionType.DEPOSIT
                            "WITHDRAW" -> TransactionType.WITHDRAWAL
                            else -> TransactionType.WITHDRAWAL // 기본값
                        }
                        
                        val transactionStatus = when (record.status.uppercase()) {
                            "APPROVED" -> TransactionStatus.COMPLETED
                            "REQUEST" -> TransactionStatus.IN_PROGRESS
                            "REJECTED" -> TransactionStatus.RETURNED
                            else -> TransactionStatus.IN_PROGRESS // 기본값
                        }
                        
                        // 날짜 파싱
                        val timestamp = record.timestamp
                        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", java.util.Locale.getDefault()).apply {
                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                        }
                        val date = dateFormat.parse(timestamp) ?: java.util.Date()
                        val displayDateFormat = java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.getDefault()).apply {
                            timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
                        }
                        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).apply {
                            timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
                        }
                        
                        // 실제 환율 적용
                        val krwAmount = com.stip.utils.ExchangeRateManager.convertUsdToKrwWithApi(record.amount)
                        
                        val transaction = USDTransaction(
                            id = record.id,
                            date = displayDateFormat.format(date),
                            time = timeFormat.format(date),
                            type = transactionType,
                            status = transactionStatus,
                            amountUsd = record.amount,
                            amountKrw = krwAmount.toInt()
                        )
                        
                        // 변환된 트랜잭션 디버깅 로그
                        android.util.Log.d("USDTransactionFragment", "변환된 트랜잭션: type=${transaction.type}, status=${transaction.status}, statusText=${getStatusText(transaction)}")
                        
                        transaction
                    } catch (e: Exception) {
                        android.util.Log.e("USDTransactionFragment", "트랜잭션 변환 실패: ${e.message}")
                        null
                    }
                }
                
                // Fragment가 여전히 유효한지 확인
                if (!isAdded || _binding == null) {
                    return@launch
                }
                
                // assetManager에 트랜잭션 데이터 저장 (모든 탭에서 공유)
                assetManager.updateTransactions(usdTransactions)
                
                // 현재 선택된 탭에 따라 데이터 표시
                val currentTab = getCurrentActiveTab()
                when (currentTab) {
                    binding.tabAll -> {
                        // 전체 탭: 모든 트랜잭션 표시
                        val adapterItems = usdTransactions.map { transaction -> convertToAdapterItem(transaction) }
                        currentTransactions = adapterItems
                        transactionAdapter.submitList(adapterItems)
                        updateEmptyState(adapterItems.isEmpty())
                    }
                    binding.tabDeposit -> loadDepositTransactions()
                    binding.tabWithdrawal -> loadWithdrawalTransactions()
                    binding.tabReturn -> loadReturnTransactions()
                    binding.tabInProgress -> loadInProgressTransactions()
                    else -> {
                        // 기본값: 전체 탭
                        val adapterItems = usdTransactions.map { transaction -> convertToAdapterItem(transaction) }
                        currentTransactions = adapterItems
                        transactionAdapter.submitList(adapterItems)
                        updateEmptyState(adapterItems.isEmpty())
                    }
                }
                
                android.util.Log.d("USDTransactionFragment", "USD 내역 로드 완료: ${usdTransactions.size}개")
                
            } catch (e: Exception) {
                android.util.Log.e("USDTransactionFragment", "USD 내역 API 호출 실패: ${e.message}", e)
                
                // Fragment가 여전히 유효한지 확인
                if (!isAdded || _binding == null) {
                    return@launch
                }
                
                // 오류 시 빈 리스트 표시
                currentTransactions = emptyList()
                transactionAdapter.submitList(emptyList())
                updateEmptyState(true)
            } finally {
                // 로딩 상태 해제
                isLoading = false
                
                // 로딩 인디케이터 숨기기
                if (isAdded && _binding != null) {
                    // binding.loadingIndicator.visibility = View.GONE
                }
            }
        }
    }
    
    /**
     * USDTransaction 모델을 어댑터에서 사용하는 모델로 변환
     */
    private fun convertToAdapterItem(transaction: USDTransaction): Any {
        // 상태에 따라 색상 설정
        val statusColor = when (transaction.status) {
            TransactionStatus.COMPLETED -> android.R.color.holo_blue_dark
            TransactionStatus.IN_PROGRESS -> R.color.light_orange_in_progress   // 커스텀 오렌지색 사용
            TransactionStatus.RETURNED -> R.color.light_green_return            // 커스텀 녹색 사용
            else -> android.R.color.darker_gray
        }
        
        // 상태에 따라 다른 트랜잭션 타입 반환
        return when {
            // 반환됨 상태면 반환 트랜잭션으로 처리
            transaction.status == TransactionStatus.RETURNED -> USDReturnTransaction(
                id = transaction.id,
                date = transaction.date,
                time = transaction.time,
                status = getStatusText(transaction),
                usdAmount = transaction.amountUsd,
                krwAmount = transaction.amountKrw.toLong(),
                statusColorRes = statusColor
            )
            // 진행중 상태면 진행중 트랜잭션으로 처리
            transaction.status == TransactionStatus.IN_PROGRESS -> USDProcessTransaction(
                id = transaction.id,
                date = transaction.date,
                time = transaction.time,
                status = getStatusText(transaction),
                usdAmount = transaction.amountUsd,
                krwAmount = transaction.amountKrw.toLong(),
                statusColorRes = statusColor
            )
            // 입금 타입이면 입금 트랜잭션으로 처리
            transaction.type == TransactionType.DEPOSIT -> USDDepositTransaction(
                id = transaction.id,
                date = transaction.date,
                time = transaction.time,
                status = getStatusText(transaction),
                usdAmount = transaction.amountUsd,
                krwAmount = transaction.amountKrw,
                statusColorRes = statusColor
            )
            // 그 외는 출금 트랜잭션으로 처리
            else -> USDWithdrawalTransaction(
                id = transaction.id,
                date = transaction.date,
                time = transaction.time,
                status = getStatusText(transaction),
                usdAmount = transaction.amountUsd,
                krwAmount = transaction.amountKrw,
                statusColorRes = statusColor
            )
        }
    }
    
    /**
     * 트랜잭션 상태 텍스트 반환
     */
    private fun getStatusText(transaction: USDTransaction): String {
        return when (transaction.status) {
            TransactionStatus.COMPLETED -> {
                when (transaction.type) {
                    TransactionType.DEPOSIT -> "입금 완료"
                    TransactionType.WITHDRAWAL -> "출금 완료"
                    TransactionType.RETURN -> "반환 완료"
                    else -> "완료"
                }
            }
            TransactionStatus.IN_PROGRESS -> {
                when (transaction.type) {
                    TransactionType.DEPOSIT -> "입금 진행중"
                    TransactionType.WITHDRAWAL -> "출금 진행중"
                    TransactionType.RETURN -> "반환 진행중"
                    else -> "진행중"
                }
            }
            TransactionStatus.RETURNED -> {
                when (transaction.type) {
                    TransactionType.DEPOSIT -> "입금 반려"
                    TransactionType.WITHDRAWAL -> "출금 반려"
                    TransactionType.RETURN -> "반환 반려"
                    else -> "반려"
                }
            }
            else -> "알 수 없음"
        }
    }
    
    // 활성화된 탭 설정 및 UI 업데이트
    private fun setActiveTab(activeTab: View, allTabs: List<View>) {
        // 모든 탭을 비활성화 상태로 설정 (배경 없음)
        allTabs.forEach { tab ->
            tab.background = resources.getDrawable(R.drawable.bg_rounded_transparent_button, null)
            if (tab is android.widget.Button) {
                tab.setTextColor(resources.getColor(android.R.color.darker_gray, null))
                tab.textSize = 14f
                tab.typeface = android.graphics.Typeface.DEFAULT
            }
        }
        
        // 선택된 탭만 활성화 상태로 설정
        activeTab.background = resources.getDrawable(R.drawable.bg_rounded_blue_button, null)
        if (activeTab is android.widget.Button) {
            activeTab.setTextColor(resources.getColor(android.R.color.white, null))
            activeTab.textSize = 14f
            activeTab.typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }
    
    /**
     * 빈 상태 처리
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        if (!isAdded || _binding == null) {
            return
        }
        
        try {
            if (isEmpty) {
                binding.recyclerViewTransactions.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.VISIBLE
            } else {
                binding.recyclerViewTransactions.visibility = View.VISIBLE
                binding.emptyStateContainer.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateEmptyState 실행 중 오류", e)
        }
    }
    
    /**
     * 샘플 트랜잭션 데이터 설정
     * 샘플 데이터 표시는 더 이상 사용하지 않음 (레이아웃에서 샘플 카드 삭제됨)
     */
    private fun setupSampleTransactions() {
        // 샘플 카드 UI가 레이아웃에서 삭제되었으므로 더 이상 필요하지 않음
        // 실제 트랜잭션 데이터가 USDAssetManager에서 관리됨
        Log.d(TAG, "샘플 트랜잭션 설정 기능은 더 이상 사용하지 않음")
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // 어떤 방식으로든 프래그먼트에서 나갈 때 헤더 표시를 복원하여 '입출금' 텍스트가 항상 보이도록 함
        val headerLayout = requireActivity().findViewById<View>(R.id.headerLayout)
        headerLayout?.visibility = View.VISIBLE
        _binding = null
    }
    
    // 안전한 네비게이션 메서드 - 입금 상세 화면으로 이동
    private fun navigateToDepositDetail(transactionId: String) {
        try {
            // NavController를 직접 사용하는 대신 프래그먼트 트랜잭션으로 이동 구현
            val fragmentManager = requireActivity().supportFragmentManager
            val fragmentTransaction = fragmentManager.beginTransaction()
            
            fragmentTransaction.setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            
            // 새 프래그먼트 인스턴스 생성 및 인자 전달
            val depositDetailFragment = USDDepositTransactionDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("transactionId", transactionId)
                }
            }
            
            // 프래그먼트 교체 및 백스택에 추가
            fragmentTransaction.replace(R.id.fragment_container, depositDetailFragment)
            fragmentTransaction.addToBackStack(null)
            fragmentTransaction.commit()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "화면 전환 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 안전한 네비게이션 메서드 - 출금 상세 화면으로 이동
    private fun navigateToWithdrawalDetail(transactionId: String) {
        try {
            // NavController를 직접 사용하는 대신 프래그먼트 트랜잭션으로 이동 구현
            val fragmentManager = requireActivity().supportFragmentManager
            val fragmentTransaction = fragmentManager.beginTransaction()
            
            fragmentTransaction.setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            
            // 새 프래그먼트 인스턴스 생성 및 인자 전달
            val withdrawalDetailFragment = USDWithdrawalTransactionDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("transactionId", transactionId)
                }
            }
            
            // 프래그먼트 교체 및 백스택에 추가
            fragmentTransaction.replace(R.id.fragment_container, withdrawalDetailFragment)
            fragmentTransaction.addToBackStack(null)
            fragmentTransaction.commit()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "화면 전환 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
