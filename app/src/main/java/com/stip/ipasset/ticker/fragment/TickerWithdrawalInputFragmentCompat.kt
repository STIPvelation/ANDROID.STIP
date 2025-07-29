package com.stip.ipasset.ticker.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.stip.ipasset.fragment.BaseFragment
import com.stip.ipasset.model.IpAsset
import com.stip.ipasset.ticker.repository.IpAssetRepository
import com.stip.ipasset.ticker.viewmodel.WithdrawalInputViewModel
import com.stip.stip.R
import com.stip.stip.databinding.FragmentIpAssetTickerWithdrawalInputBinding
import com.stip.stip.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

/**
 * 티커 출금 입력을 처리하는 Fragment
 * Navigation Component에 의존하지 않는 버전
 * 출금 금액과 주소를 입력받습니다.
 */
@AndroidEntryPoint
class TickerWithdrawalInputFragmentCompat : BaseFragment<FragmentIpAssetTickerWithdrawalInputBinding>() {
    
    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentIpAssetTickerWithdrawalInputBinding {
        return FragmentIpAssetTickerWithdrawalInputBinding.inflate(inflater, container, false)
    }
    
    // Bundle에서 직접 인자를 받음 (NavArgs 대신)
    private lateinit var ipAsset: IpAsset
    private val currencyCode: String get() = ipAsset.currencyCode

    private val viewModel by viewModels<WithdrawalInputViewModel>()
    
    // 출금 가능한 최대 금액 (저장소에서 가져옴)
    private val repository: IpAssetRepository by lazy { IpAssetRepository.getInstance(requireContext()) }
    
    // Market API 관련
    private val marketRepository: com.stip.stip.api.repository.MarketRepository by lazy { 
        com.stip.stip.api.repository.MarketRepository() 
    }
    
    // API에서 가져온 수수료와 출금한도
    private var apiFee: Double = 0.0
    private var apiMaxValue: Double = 0.0
    
    private val availableAmount: Double
        get() = (repository.getAsset(ipAsset.id)?.amount ?: ipAsset.amount).toDouble()
    private val maxAmount: Double
        get() = apiMaxValue
    private val fee: Double
        get() = apiFee
    
    override fun onResume() {
        super.onResume()
        
        // 티커 화면에서는 헤더 레이아웃 숨기기
        (activity as? MainActivity)?.setHeaderVisibility(false)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Bundle에서 인자 추출
        arguments?.let { args ->
            (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                args.getParcelable(ARG_IP_ASSET, IpAsset::class.java)
            } else {
                @Suppress("DEPRECATION")
                args.getParcelable<IpAsset>(ARG_IP_ASSET)
            })?.let { asset ->
                ipAsset = asset
            }
        }
        
        // 테스트 데이터 설정 (실제 앱에서는 API에서 가져와야 함)
        if (repository.getAsset(ipAsset.id) == null) {
            IpAssetRepository.setTestData(requireContext(), listOf(ipAsset))
        }
    }
    
    /**
     * Market API에서 수수료와 출금한도 정보를 가져옴
     */
    private fun loadMarketInfo() {
        lifecycleScope.launch {
            try {
                val ipListingRepository = com.stip.stip.api.repository.IpListingRepository()
                val marketPairId = ipListingRepository.getPairIdForTicker(currencyCode)
                if (marketPairId != null) {
                    val marketResponse = marketRepository.getMarket(marketPairId)
                    if (marketResponse != null) {
                        apiFee = marketResponse.fee
                        apiMaxValue = marketResponse.maxValue
                        updateUI()
                    }
                }
            } catch (_: Exception) {}
        }
    }
    
    private fun updateUI() {
        val binding = binding ?: return
        
        binding.tvMaxAmount.text = "최대 ${String.format("%,.2f", maxAmount)} ${currencyCode}"
        binding.tvFeeAmount.text = "${String.format("%,.2f", fee)} ${currencyCode}"
    }
    
    // 숫자 포맷터 설정
    private val decimalFormat = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }
    private val amountFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    
    private fun bind() {
        val binding = binding ?: return
        with(binding) {
            // 툴바 설정
            materialToolbar.title = "$currencyCode ${getString(R.string.common_withdrawal_action)}"
            materialToolbar.setNavigationOnClickListener {
                // NavController 대신 FragmentManager 사용
                parentFragmentManager.popBackStack()
            }
            
            // 툴바 제목에서 티커 이름 추출해서 placeholder 설정
            val toolbarTitle = materialToolbar.title.toString()
            val tickerName = toolbarTitle.replace(" 출금하기", "").trim()
            etAmount.hint = "0.00 $tickerName"
            
            // 가용 잔액 및 한도 설정 (소수점 2자리)
            tvAvailableAmount.text = decimalFormat.format(availableAmount) + " $currencyCode"
            tvMaxAmount.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(maxAmount) + " $currencyCode"
            
            // 수수료 정보 표시 (소수점 2자리)
            tvFeeAmount.text = decimalFormat.format(fee) + " $currencyCode"
            updateTotalAmount(0.0)
            
            // 퍼센트 버튼 리스너 설정
            setupPercentageButtons()
            
            // 금액 입력 TextWatcher 설정
            etAmount.addTextChangedListener(object : TextWatcher {
                private var isFormatting = false
                private val decimalFormat = NumberFormat.getNumberInstance(Locale.getDefault()) as DecimalFormat
                
                init {
                    // 소수점 두 자리로 제한하고 3자리마다 콤마 설정
                    decimalFormat.apply {
                        maximumFractionDigits = 2
                        minimumFractionDigits = 0 // 필요한 경우만 소수점 표시
                        isGroupingUsed = true // 3자리마다 콤마 사용
                    }
                }
                
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    // 재귀적인 호출을 방지하기 위한 플래그 확인
                    if (isFormatting || s == null) return
                    
                    isFormatting = true
                    
                    try {
                        // 금액 텍스트 가져오기
                        val amountText = s.toString()
                        
                        // 콤마 제거 후 숫자만 추출
                        val cleanString = amountText.replace(",", "")
                        
                        // 현재 커서 위치 저장
                        val cursorPosition = etAmount.selectionStart
                        
                        // 숫자로 변환 (입력이 비어있거나 잘못된 경우 0으로 처리)
                        val parsed = cleanString.toDoubleOrNull() ?: 0.0
                        
                        // 총 출금액 업데이트 (수수료 포함)
                        updateTotalAmount(parsed)
                        
                        // 3자리마다 콤마 추가 및 소수점 제한 적용한 포맷
                        val formatted = decimalFormat.format(parsed)
                        
                        // 이전/이후 텍스트 길이 차이로 커서 위치 계산
                        val formattedLength = formatted.length
                        val cleanLength = cleanString.length
                        val cursorOffset = formattedLength - cleanLength
                        var newCursorPosition = cursorPosition + cursorOffset
                        if (newCursorPosition > formattedLength) newCursorPosition = formattedLength
                        if (newCursorPosition < 0) newCursorPosition = 0
                        
                        // 텍스트 변경
                        etAmount.removeTextChangedListener(this)
                        etAmount.setText(formatted)
                        etAmount.setSelection(newCursorPosition.coerceAtMost(formatted.length))
                    } catch (e: Exception) {
                        // 예외 발생 시 안전하게 처리
                    } finally {
                        // 리스너 다시 추가
                        etAmount.addTextChangedListener(this)
                        isFormatting = false
                    }
                }
            })

            // 출금신청 버튼 설정
            btnContinue.setOnClickListener {
                if (validateInputs()) {
                    navigateToConfirmation()
                }
            }
        }
    }
    
    // 총출금 금액 업데이트 (수수료 포함)
    private fun updateTotalAmount(withdrawalAmount: Double) {
        val binding = binding ?: return
        val totalAmount = withdrawalAmount + fee
        binding.tvTotalFeeAmount.text = decimalFormat.format(totalAmount) + " $currencyCode"
    }
    
    private fun setupPercentageButtons() {
        val binding = binding ?: return
        with(binding) {
            btnPercent10.setOnClickListener { calculateAmountByPercentage(0.1) }
            btnPercent25.setOnClickListener { calculateAmountByPercentage(0.25) }
            btnPercent50.setOnClickListener { calculateAmountByPercentage(0.5) }
            btnPercent100.setOnClickListener { calculateAmountByPercentage(1.0) }
        }
    }
    
    private fun calculateAmountByPercentage(percentage: Double) {
        val binding = binding ?: return
        with(binding) {
            val calculatedAmount = if (percentage == 1.0) {
                // 100% 버튼을 누를 경우 수수료를 고려하여 최대 출금 가능액을 계산
                availableAmount - fee
            } else {
                availableAmount * percentage
            }
            etAmount.setText(amountFormat.format(calculatedAmount))
        }
    }
    
    private fun validateInputs(): Boolean {
        val binding = binding ?: return false
        with(binding) {
            // 금액 검증
            val amountStr = etAmount.text.toString()
            if (amountStr.isEmpty()) {
                Toast.makeText(context, "금액을 입력해주세요", Toast.LENGTH_SHORT).show()
                return false
            }
            
            val amount = amountStr.replace(",", "").toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(context, "유효한 금액을 입력해주세요", Toast.LENGTH_SHORT).show()
                return false
            }
            
            if (amount < 1.0) {
                Toast.makeText(context, "최소 출금 금액은 1 ${currencyCode} 입니다", Toast.LENGTH_SHORT).show()
                return false
            }
            
            // 출금수량이 수수료보다 커야 함
            if (amount <= fee) {
                Toast.makeText(context, "출금수량은 수수료(${String.format("%.2f", fee)} ${currencyCode})보다 커야 합니다", Toast.LENGTH_SHORT).show()
                return false
            }
            
            // 수수료를 포함한 총 출금액이 가용 잔액을 초과하는지 확인
            val totalWithdrawalAmount = amount + fee
            if (totalWithdrawalAmount > availableAmount) {
                Toast.makeText(context, "수수료를 포함한 총 출금액이 가용 잔액을 초과합니다", Toast.LENGTH_SHORT).show()
                return false
            }
            
            // 주소 검증
            val address = etAddress.text.toString().trim()
            if (address.isEmpty()) {
                Toast.makeText(context, "출금 주소를 입력해주세요", Toast.LENGTH_SHORT).show()
                return false
            }
            
            return true
        }
    }

    private fun navigateToConfirmation() {
        val binding = binding ?: return
        // Fragment Transaction으로 확인 화면으로 이동
        val withdrawalAmountText = binding.etAmount.text.toString().replace(",", "")
        val withdrawalAmount = withdrawalAmountText.toFloatOrNull() ?: 0.0f
        val withdrawalAddress = binding.etAddress.text.toString().trim()
        
        // 다음 프래그먼트로 인자 전달
        val confirmFragment = TickerWithdrawalConfirmFragmentCompat().apply {
            arguments = Bundle().apply {
                putParcelable(TickerWithdrawalConfirmFragmentCompat.ARG_IP_ASSET, ipAsset)
                putFloat(TickerWithdrawalConfirmFragmentCompat.ARG_AMOUNT, withdrawalAmount)
                putFloat(TickerWithdrawalConfirmFragmentCompat.ARG_FEE, fee.toFloat())
                putString(TickerWithdrawalConfirmFragmentCompat.ARG_ADDRESS, withdrawalAddress)
            }
        }
        
        // Fragment 전환
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, confirmFragment)
            .addToBackStack(null)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadMarketInfo()
        
        bind()
    }

    companion object {
        const val ARG_IP_ASSET = "arg_ip_asset"
        
        // 새 인스턴스 생성 헬퍼 메서드
        fun newInstance(ipAsset: IpAsset): TickerWithdrawalInputFragmentCompat {
            return TickerWithdrawalInputFragmentCompat().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_IP_ASSET, ipAsset)
                }
            }
        }
    }
}
