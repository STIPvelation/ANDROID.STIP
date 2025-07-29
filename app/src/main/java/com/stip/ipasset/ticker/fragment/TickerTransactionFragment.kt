package com.stip.ipasset.ticker.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
// Removed dummy data imports
import com.stip.ipasset.model.IpAsset
import com.stip.ipasset.ticker.activity.TickerDepositDetailActivity
import com.stip.ipasset.ticker.activity.TickerWithdrawalDetailActivity
import com.stip.ipasset.ticker.adapter.TickerTransactionAdapter
import com.stip.ipasset.ticker.fragment.TickerWithdrawalInputFragmentCompat
import com.stip.ipasset.ticker.model.TickerDepositTransaction
import com.stip.ipasset.ticker.model.TickerWithdrawalTransaction
import com.stip.stip.MainActivity
import com.stip.stip.R
import com.stip.stip.databinding.FragmentIpAssetTickerTransactionBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale
import com.stip.api.repository.WalletHistoryRepository
import com.stip.api.model.WalletHistoryRecord
import com.stip.stip.signup.utils.PreferenceUtil
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import android.util.Log
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import com.stip.api.repository.PortfolioRepository
import java.math.BigDecimal

@AndroidEntryPoint
class TickerTransactionFragment : Fragment() {

    private var _binding: FragmentIpAssetTickerTransactionBinding? = null
    private val binding get() = _binding!!
    
    private var tickerCode: String? = null
    private var amount: Double = 0.0
    private var usdEquivalent: Double = 0.0
    
    private lateinit var transactionAdapter: TickerTransactionAdapter
    private var currentFilter: TransactionFilter = TransactionFilter.ALL
    
    private val walletHistoryRepository = WalletHistoryRepository()
    
    private var allTransactions: List<Any> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 전달된 데이터 추출
        arguments?.let {
            tickerCode = it.getString(ARG_TICKER_CODE)
            amount = it.getDouble(ARG_AMOUNT, 0.0)
            usdEquivalent = it.getDouble(ARG_USD_EQUIVALENT, 0.0)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIpAssetTickerTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 뒤로가기 버튼 설정
        binding.materialToolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        // 티커 정보 표시
        setupTickerInfo()
        
        // 어댑터 초기화 및 설정
        setupTransactionAdapter()
        
        // 필터 탭 설정
        setupFilterTabs()
        
        // 버튼 리스너 설정
        setupButtonListeners()
        
        // 티커 잔고 정보 업데이트
        updateTickerBalance()
        
        // 입출금 내역 로드
        loadWalletHistoryForTransactions()
    }
    
    override fun onResume() {
        super.onResume()
        // 상세 화면에서 돌아올 때 타이틀을 '총 보유'로 유지
        binding.materialToolbar.title = "총 보유"
        // 타이틀 텍스트 설정이 다른 곳에서 변경되지 않도록 post 사용
        binding.materialToolbar.post {
            binding.materialToolbar.title = "총 보유"
        }
        
        // MainActivity의 헤더 완전히 숨기기
        (activity as? MainActivity)?.setHeaderVisibility(false)
    }
    
    override fun onStop() {
        super.onStop()
    }
    
    private fun setupTickerInfo() {
        tickerCode?.let { code ->
            // API로 대체 예정
            // 임시 데이터 사용
            val asset = IpAsset(id = code, name = code, ticker = code, balance = amount, value = usdEquivalent)
            
            // 툴바 타이틀 설정
            binding.materialToolbar.title = "총 보유"
            
            // 티커 로고 설정
            val tickerInitials = code.take(2)
            binding.currencyIconText.text = tickerInitials
            
            // 티커 색상 설정
            val colorResId = when(code) {
                "JWV" -> R.color.token_jwv
                "MDM" -> R.color.token_mdm
                "CDM" -> R.color.token_cdm
                "IJECT" -> R.color.token_iject
                "WETALK" -> R.color.token_wetalk
                "SLEEP" -> R.color.token_sleep
                "KCOT" -> R.color.token_kcot
                "MSK" -> R.color.token_msk
                "SMT" -> R.color.token_smt
                "AXNO" -> R.color.token_axno
                "KATV" -> R.color.token_katv
                else -> R.color.token_usd
            }
            binding.currencyIconBackground.backgroundTintList = requireContext().getColorStateList(colorResId)

        }
    }
    
    private fun setupButtonListeners() {
        // 입금 버튼 (deposit_button_container 사용)
        binding.depositButtonContainer.setOnClickListener {
            // 입금 화면으로 이동 로직 추가
            navigateToTickerDepositScreen()
        }
        
        // 출금 버튼 (withdraw_button_container 사용)
        binding.withdrawButtonContainer.setOnClickListener {
            // 출금 화면으로 이동 로직 추가
            navigateToTickerWithdrawalScreen()
        }
    }
    
    private fun navigateToTickerDepositScreen() {
        tickerCode?.let { code ->
            try {
                // fragment 인스턴스를 사용
                val fragmentManager = requireActivity().supportFragmentManager
                val fragmentTransaction = fragmentManager.beginTransaction()
                
                // 애니메이션 추가
                fragmentTransaction.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                
                // 티커 입금 프래그먼트로 이동
                val tickerDepositFragment = TickerDepositFragment()
                
                // 임시 자산 객체 생성 (API로 대체 예정)
                val asset = IpAsset(id = code, name = code, ticker = code, balance = amount, value = usdEquivalent)
                
                // 번들에 데이터 전달
                val bundle = Bundle()
                // Android API 33+ 호환성 위해 타입 명시
                bundle.putParcelable("ipAsset", asset as android.os.Parcelable)
                tickerDepositFragment.arguments = bundle
                
                // 프래그먼트 교체 및 백스택에 추가
                fragmentTransaction.replace(R.id.fragment_container, tickerDepositFragment)
                fragmentTransaction.addToBackStack(null)
                fragmentTransaction.commit()
            } catch (e: Exception) {
                android.widget.Toast.makeText(requireContext(), "화면 전환 오류: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            android.widget.Toast.makeText(requireContext(), "티커 정보가 없습니다", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToTickerWithdrawalScreen() {
        tickerCode?.let { code ->
            // 임시 자산 객체 생성 (API로 대체 예정)
            val asset = IpAsset(id = code, name = code, ticker = code, balance = amount, value = usdEquivalent)
            
            try {
                // NavController를 사용하지 않는 TickerWithdrawalInputFragmentCompat 인스턴스 생성
                val withdrawalFragment = TickerWithdrawalInputFragmentCompat.newInstance(asset)
                
                // Fragment Transaction으로 화면 교체
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, withdrawalFragment)
                    .addToBackStack(null)
                    .commit()
            } catch (e: Exception) {
                android.util.Log.e("TickerTransactionFragment", "출금 화면 이동 실패: ${e.message}", e)
                android.widget.Toast.makeText(
                    requireContext(),
                    "화면 전환 중 오류가 발생했습니다: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } ?: run {
            // 티커 코드가 없는 경우
            android.widget.Toast.makeText(
                requireContext(),
                "티커 정보가 없습니다.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    // 거래 내역 어댑터 설정
    private fun setupTransactionAdapter() {
        // 어댑터 초기화
        transactionAdapter = TickerTransactionAdapter { transaction ->
            when (transaction) {
                is TickerDepositTransaction -> {
                    // 입금 내역 상세 화면으로 이동
                    val intent = Intent(requireContext(), TickerDepositDetailActivity::class.java).apply {
                        putExtra("tickerSymbol", transaction.tickerSymbol)
                        putExtra("tickerAmount", transaction.tickerAmount)
                        putExtra("usdAmount", transaction.usdAmount)
                        putExtra("timestamp", transaction.timestamp)
                        putExtra("status", transaction.status)
                        putExtra("txHash", transaction.txHash)
                    }
                    startActivity(intent)
                }
                is TickerWithdrawalTransaction -> {
                    val intent = Intent(requireContext(), TickerWithdrawalDetailActivity::class.java).apply {
                        putExtra("transactionApiId", transaction.id.toString())
                        putExtra("tickerSymbol", transaction.tickerSymbol)
                        putExtra("tickerAmount", transaction.tickerAmount)
                        putExtra("usdAmount", transaction.usdAmount)
                        putExtra("timestamp", transaction.timestamp)
                        putExtra("timestampIso", transaction.timestampIso)
                        putExtra("status", transaction.status)
                        putExtra("txHash", transaction.txHash)
                        putExtra("recipientAddress", transaction.recipientAddress)
                        putExtra("fee", transaction.fee)
                    }
                    startActivity(intent)
                }
            }
        }

        // 리사이클러뷰 설정
        binding.recyclerViewTransactions.apply {
            adapter = transactionAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    // 필터 탭 설정
    private fun setupFilterTabs() {
        // 전체 탭
        binding.tabAll.setOnClickListener {
            currentFilter = TransactionFilter.ALL
            updateTabSelection()
            updateTransactionList()
        }

        // 입금 탭
        binding.tabDeposit.setOnClickListener {
            currentFilter = TransactionFilter.DEPOSIT
            updateTabSelection()
            updateTransactionList()
        }

        // 출금 탭
        binding.tabWithdrawal.setOnClickListener {
            currentFilter = TransactionFilter.WITHDRAWAL
            updateTabSelection()
            updateTransactionList()
        }

        // 반환 탭
        binding.tabReturn.setOnClickListener {
            currentFilter = TransactionFilter.REFUND
            updateTabSelection()
            updateTransactionList()
        }

        // 진행중 탭
        binding.tabInProgress.setOnClickListener {
            currentFilter = TransactionFilter.IN_PROGRESS
            updateTabSelection()
            updateTransactionList()
        }

        // 초기 탭 선택 상태 설정
        updateTabSelection()
    }

    // 탭 선택 상태 업데이트
    private fun updateTabSelection() {
        // 모든 탭을 비선택 상태로 초기화
        binding.tabAll.setBackgroundResource(R.drawable.bg_rounded_transparent_button)
        binding.tabAll.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        
        binding.tabDeposit.setBackgroundResource(R.drawable.bg_rounded_transparent_button)
        binding.tabDeposit.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        
        binding.tabWithdrawal.setBackgroundResource(R.drawable.bg_rounded_transparent_button)
        binding.tabWithdrawal.setTextColor(resources.getColor(android.R.color.darker_gray, null))

        binding.tabReturn.setBackgroundResource(R.drawable.bg_rounded_transparent_button)
        binding.tabReturn.setTextColor(resources.getColor(android.R.color.darker_gray, null))

        binding.tabInProgress.setBackgroundResource(R.drawable.bg_rounded_transparent_button)
        binding.tabInProgress.setTextColor(resources.getColor(android.R.color.darker_gray, null))

        // 선택된 탭 강조
        when (currentFilter) {
            TransactionFilter.ALL -> {
                binding.tabAll.setBackgroundResource(R.drawable.bg_rounded_blue_button)
                binding.tabAll.setTextColor(resources.getColor(android.R.color.white, null))
            }
            TransactionFilter.DEPOSIT -> {
                binding.tabDeposit.setBackgroundResource(R.drawable.bg_rounded_blue_button)
                binding.tabDeposit.setTextColor(resources.getColor(android.R.color.white, null))
            }
            TransactionFilter.WITHDRAWAL -> {
                binding.tabWithdrawal.setBackgroundResource(R.drawable.bg_rounded_blue_button)
                binding.tabWithdrawal.setTextColor(resources.getColor(android.R.color.white, null))
            }
            TransactionFilter.REFUND -> {
                binding.tabReturn.setBackgroundResource(R.drawable.bg_rounded_blue_button)
                binding.tabReturn.setTextColor(resources.getColor(android.R.color.white, null))
            }
            TransactionFilter.IN_PROGRESS -> {
                binding.tabInProgress.setBackgroundResource(R.drawable.bg_rounded_blue_button)
                binding.tabInProgress.setTextColor(resources.getColor(android.R.color.white, null))
            }
        }
    }

    // 트랜잭션 데이터 로드 및 필터링
    private fun loadWalletHistoryForTransactions() {
        viewLifecycleOwner.lifecycleScope.launch {
            val token = PreferenceUtil.getToken()
            val currentTickerCode = tickerCode
            if (token.isNullOrBlank() || currentTickerCode.isNullOrBlank()) {
                Log.e("TickerTransactionFragment", "token 또는 tickerCode가 비어있음: token=${token?.take(10)}..., tickerCode=$currentTickerCode")
                return@launch
            }
            
            Log.d("TickerTransactionFragment", "API 호출 시작: tickerCode=$currentTickerCode")
            
            // marketPairId를 찾기 위해 먼저 market/pairs API 호출
            val marketPairsService = com.stip.stip.api.RetrofitClient.createTapiService(com.stip.stip.api.service.MarketPairsService::class.java)
            val marketPairs = try {
                marketPairsService.getMarketPairs()
            } catch (e: Exception) {
                Log.e("TickerTransactionFragment", "Market pairs API 호출 실패", e)
                emptyList()
            }
            
            Log.d("TickerTransactionFragment", "Market pairs size: ${marketPairs.size}")
            
            // 현재 티커에 해당하는 marketPairId 찾기
            val marketPair = marketPairs.find { it.baseAsset == currentTickerCode }
            val marketPairId = marketPair?.id
            
            Log.d("TickerTransactionFragment", "찾은 marketPair: $marketPair, marketPairId: $marketPairId")
            
            if (marketPairId.isNullOrBlank()) {
                Log.e("TickerTransactionFragment", "Market pair ID를 찾을 수 없음: $currentTickerCode")
                return@launch
            }
            
            // 특정 marketPairId의 거래 내역만 가져오기
            val history = try {
                walletHistoryRepository.getWalletHistory(
                    authorization = "Bearer $token",
                    marketPairId = marketPairId
                )
            } catch (e: Exception) {
                Log.e("TickerTransactionFragment", "Wallet history API 호출 실패", e)
                emptyList()
            }
            
            Log.d("TickerTransactionFragment", "거래내역: ${history.size}")
            history.forEach { record ->
                Log.d("TickerTransactionFragment", "Record: id=${record.id}, symbol=${record.symbol}, type=${record.type}, status=${record.status}, amount=${record.amount}")
            }
            
            // 거래 내역만 변환하여 표시
            allTransactions = history.mapNotNull { record ->
                val transaction = record.toTickerTransaction()
                Log.d("TickerTransactionFragment", "변환된 트랜잭션: $transaction")
                transaction
            }
            Log.d("TickerTransactionFragment", "allTransactions size: ${allTransactions.size}")
            updateTransactionList()
        }
    }

    private fun updateTransactionList() {
        val filtered = when (currentFilter) {
            TransactionFilter.ALL -> allTransactions
            TransactionFilter.DEPOSIT -> allTransactions.filterIsInstance<TickerDepositTransaction>()
            TransactionFilter.WITHDRAWAL -> allTransactions.filterIsInstance<TickerWithdrawalTransaction>()
            TransactionFilter.REFUND -> allTransactions.filter { transaction ->
                when (transaction) {
                    is TickerDepositTransaction -> transaction.status.contains("반환")
                    is TickerWithdrawalTransaction -> transaction.status.contains("반환")
                    else -> false
                }
            }
            TransactionFilter.IN_PROGRESS -> allTransactions.filter { transaction ->
                when (transaction) {
                    is TickerDepositTransaction -> transaction.status.contains("진행중")
                    is TickerWithdrawalTransaction -> transaction.status.contains("진행중")
                    else -> false
                }
            }
        }.filter {
            (it as? TickerDepositTransaction)?.tickerSymbol.equals(tickerCode, ignoreCase = true) ||
            (it as? TickerWithdrawalTransaction)?.tickerSymbol.equals(tickerCode, ignoreCase = true)
        }
        transactionAdapter.submitList(filtered)
        binding.emptyStateContainer.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerViewTransactions.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // 트랜잭션 필터 열거형
    enum class TransactionFilter {
        ALL,       // 전체
        DEPOSIT,   // 입금
        WITHDRAWAL,  // 출금
        REFUND,    // 반환
        IN_PROGRESS // 진행중
    }
    
    private fun WalletHistoryRecord.toTickerTransaction(): Any? {
        val txId = this.id.hashCode().toLong()
        val ts = parseIsoTimestampToMillis(this.timestamp)
        return when (this.type.lowercase()) {
            "deposit" -> TickerDepositTransaction(
                id = txId,
                tickerAmount = this.realAmount,
                tickerSymbol = this.symbol,
                usdAmount = this.amount,
                timestamp = ts,
                timestampIso = this.timestamp,
                status = when (this.status) {
                    "REQUEST" -> "입금 진행중"
                    "APPROVED" -> "입금 완료"
                    "REJECTED" -> "입금 반환"
                    else -> "입금 완료"
                },
                txHash = this.transactionId
            )
            "withdraw" -> TickerWithdrawalTransaction(
                id = txId,
                tickerAmount = this.realAmount,
                tickerSymbol = this.symbol,
                usdAmount = this.usdAmount,
                timestamp = ts,
                timestampIso = this.timestamp,
                status = when (this.status) {
                    "REQUEST" -> "출금 진행중"
                    "APPROVED" -> "출금 완료"
                    "REJECTED" -> "출금 반환"
                    else -> "출금 완료"
                },
                txHash = this.transactionId,
                recipientAddress = this.toAddress,
                fee = this.fee
            )
            else -> null
        }
    }
    
    private fun parseIsoTimestampToMillis(iso: String): Long {
        return try {
            val fixedIso = iso.replace(Regex("\\.(\\d{3})\\d{3}"), ".$1")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            dateFormat.parse(fixedIso)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("TickerTransactionFragment", "timestamp 파싱 실패: $iso", e)
            System.currentTimeMillis()
        }
    }
    
    companion object {
        private const val ARG_TICKER_CODE = "tickerCode"
        private const val ARG_AMOUNT = "amount"
        private const val ARG_USD_EQUIVALENT = "usdEquivalent"
        
        fun newInstance(tickerCode: String, amount: Double, usdEquivalent: Double): TickerTransactionFragment {
            val fragment = TickerTransactionFragment()
            val args = Bundle().apply {
                putString(ARG_TICKER_CODE, tickerCode)
                putDouble(ARG_AMOUNT, amount)
                putDouble(ARG_USD_EQUIVALENT, usdEquivalent)
            }
            fragment.arguments = args
            return fragment
        }
    }

    /**
     * 티커 잔고 정보 업데이트
     */
    private fun updateTickerBalance() {
        lifecycleScope.launch {
            try {
                val memberId = PreferenceUtil.getUserId()
                if (memberId.isNullOrBlank()) {
                    android.util.Log.w("TickerTransactionFragment", "사용자 ID가 없습니다.")
                    return@launch
                }
                
                val portfolioRepository = com.stip.api.repository.PortfolioRepository()
                val portfolioResponse = portfolioRepository.getPortfolioResponse(memberId)
                
                activity?.runOnUiThread {
                    if (portfolioResponse != null) {
                        // 현재 티커의 지갑 찾기
                        val tickerWallet = portfolioResponse.wallets.find { it.symbol == tickerCode }
                        
                        if (tickerWallet != null) {
                            val formatter = java.text.DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }
                            
                            // 보유 수량
                            binding.tickerHoldings.text = "${formatter.format(tickerWallet.balance)} ${tickerCode}"
                            
                            // USD 환산 가치 (evalAmount 사용)
                            if (tickerWallet.evalAmount > BigDecimal.ZERO) {
                                binding.equivalentAmount.text = "≈ $${formatter.format(tickerWallet.evalAmount)}"
                            } else {
                                binding.equivalentAmount.text = "≈ $0.00"
                            }
                            
                            // 전역 변수 업데이트
                            amount = tickerWallet.balance.toDouble()
                            usdEquivalent = tickerWallet.evalAmount.toDouble()
                            
                            android.util.Log.d("TickerTransactionFragment", "${tickerCode} 잔고 업데이트 완료: ${tickerWallet.balance} ${tickerCode} (≈ $${tickerWallet.evalAmount} USD)")
                        } else {
                            // 해당 티커 지갑이 없는 경우
                            binding.tickerHoldings.text = "0.00 ${tickerCode}"
                            binding.equivalentAmount.text = "≈ $0.00"
                            amount = 0.0
                            usdEquivalent = 0.0
                            android.util.Log.w("TickerTransactionFragment", "${tickerCode} 지갑을 찾을 수 없습니다.")
                        }
                    } else {
                        // API 응답이 null인 경우
                        binding.tickerHoldings.text = "0.00 ${tickerCode}"
                        binding.equivalentAmount.text = "≈ $0.00"
                        amount = 0.0
                        usdEquivalent = 0.0
                        android.util.Log.w("TickerTransactionFragment", "포트폴리오 응답이 null입니다.")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TickerTransactionFragment", "${tickerCode} 잔고 업데이트 실패: ${e.message}", e)
                activity?.runOnUiThread {
                    binding.tickerHoldings.text = "0.00 ${tickerCode}"
                    binding.equivalentAmount.text = "≈ $0.00"
                    amount = 0.0
                    usdEquivalent = 0.0
                }
            }
        }
    }
}
