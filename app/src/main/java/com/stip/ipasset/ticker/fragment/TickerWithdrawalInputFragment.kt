package com.stip.ipasset.ticker.fragment

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stip.stip.MainViewModel
import kotlinx.coroutines.launch
// Removed dummy data import
import com.stip.ipasset.ticker.repository.IpAssetRepository
import com.stip.stip.R
import com.stip.stip.databinding.FragmentIpAssetTickerWithdrawalInputBinding
import com.stip.ipasset.fragment.BaseFragment
import com.stip.ipasset.model.IpAsset
import com.stip.ipasset.ticker.viewmodel.WithdrawalInputViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

/**
 * 티커 출금 입력을 처리하는 Fragment
 * 출금 금액과 주소를 입력받습니다.
 */
@AndroidEntryPoint
class TickerWithdrawalInputFragment : BaseFragment<FragmentIpAssetTickerWithdrawalInputBinding>() {
    
    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentIpAssetTickerWithdrawalInputBinding {
        return FragmentIpAssetTickerWithdrawalInputBinding.inflate(inflater, container, false)
    }
    
    // MainViewModel 인스턴스 가져오기
    private val mainViewModel: MainViewModel by activityViewModels()
    
    private val args by navArgs<TickerWithdrawalInputFragmentArgs>()
    
    // 항상 최신 데이터를 가져오도록 수정
    private val ipAsset: IpAsset 
        get() {
            // 저장소에서 먼저 최신 데이터를 조회
            val repository = IpAssetRepository.getInstance(requireContext())
            return repository.getAsset(args.ipAsset.id) ?: args.ipAsset
        }
        
    private val currencyCode: String get() = args.ipAsset.currencyCode // currencyCode는 변경되지 않으므로 args에서 직접 가져옴

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
    
    // 항상 최신 데이터를 가져오도록 수정
    private fun getLatestAvailableAmount(): Double {
        // 저장소에서 최신 데이터를 강제로 조회
        val asset = repository.getAsset(ipAsset.id)
        return (asset?.amount ?: ipAsset.amount).toDouble()
    }
    
    // 호환성을 위해 기존 프로퍼티 유지
    private val availableAmount: Double
        get() = getLatestAvailableAmount()
    private val maxAmount: Double
        get() = apiMaxValue
    private val fee: Double
        get() = apiFee
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                // 티커로부터 marketPairId 가져오기
                val ipListingRepository = com.stip.stip.api.repository.IpListingRepository()
                val marketPairId = ipListingRepository.getPairIdForTicker(currencyCode)
                
                if (marketPairId != null) {
                    Log.d("TickerWithdrawal", "Market API 호출 시작: marketPairId=$marketPairId")
                    
                    // Market API 호출
                    val marketResponse = marketRepository.getMarket(marketPairId)
                    
                    Log.d("TickerWithdrawal", "Market API 응답: $marketResponse")
                    
                    if (marketResponse != null) {
                        // API에서 가져온 값으로 업데이트
                        apiFee = marketResponse.fee
                        apiMaxValue = marketResponse.maxValue
                        Log.d("TickerWithdrawal", "한도 및 수수료 응답 fee: ${marketResponse.fee}, maxValue: ${marketResponse.maxValue}")
                        // UI 업데이트
                        updateUI()
                    } else {
                        Log.d("TickerWithdrawal", "Market API 응답이 null")
                    }
                } else {
                    Log.d("TickerWithdrawal", "marketPairId를 찾을 수 없음: $currencyCode")
                }
            } catch (e: Exception) {
                Log.e("TickerWithdrawal", "Market API 호출 실패: ${e.message}", e)
            }
        }
    }
    
    // 화면에 돌아올 때마다 최신 데이터로 새로고침
    override fun onResume() {
        super.onResume()

        try {
            // Market API에서 최신 수수료와 출금한도 가져오기
            loadMarketInfo()
            
            // 리포지토리에서 최신 데이터를 강제로 다시 가져옴
            val repository = IpAssetRepository.getInstance(requireContext())
            val assetId = args.ipAsset.id
            val refreshedAsset = repository.getAsset(assetId)
            
            // 로그 추가하여 디버깅
            android.util.Log.d("TickerWithdrawal", "onResume - Asset ID: $assetId")
            android.util.Log.d("TickerWithdrawal", "onResume - 이전 잔액: ${args.ipAsset.amount}, 현재 잔액: ${refreshedAsset?.amount}")
            
            // UI 즉시 업데이트 (포커스가 다른 곳에 있더라도)
            if (refreshedAsset != null && refreshedAsset.amount != args.ipAsset.amount) {
                android.util.Log.d("TickerWithdrawal", "잔액이 변경되었습니다! UI 업데이트 진행")
                
                // 화면 애니메이션으로 변경 강조
                val binding = binding ?: return
                binding.tvAvailableAmount.apply {
                    // 색상 값 직접 사용 (노란색 하이라이트)
                    setBackgroundColor(0xFFFFEB3B.toInt())
                    postDelayed({
                        setBackgroundColor(0x00000000) // 투명색
                    }, 1000)
                }
            }
            
            // 항상 최신 데이터로 UI 새로고침
            updateUI()
        } catch (e: Exception) {
            android.util.Log.e("TickerWithdrawal", "onResume 오류: ${e.message}")
            e.printStackTrace()
            // 오류가 발생해도 UI는 업데이트
            updateUI()
        }
    }
    
    private fun updateUI() {
        val binding = binding ?: return
        
        // ✅ ipAsset getter를 통해 최신 데이터 사용 (중복 호출 방지)
        val currentAsset = ipAsset
        val currentAmount = currentAsset.amount
        
        // 로깅 추가
        android.util.Log.d("TickerWithdrawal", "✅ updateUI - Asset ID: ${currentAsset.id}, 최신 잔액: $currentAmount, 화폐: ${currentAsset.currencyCode}")
        android.util.Log.d("TickerWithdrawal", "🔍 리포지토리 데이터 직접 확인: ${repository.getAsset(currentAsset.id)?.amount}")
        
        // 이전 값과 비교하여 변경 감지
        val previousAmount = binding.tvAvailableAmount.tag as? Double
        
        // 금액 표시 업데이트
        binding.tvAvailableAmount.text = "${String.format("%,.2f", currentAmount)} ${currencyCode}"
        binding.tvMaxAmount.text = "최대 ${String.format("%,.2f", maxAmount)} ${currencyCode}"
        binding.tvFeeAmount.text = "${String.format("%,.2f", fee)} ${currencyCode}"
        
        // 태그에 현재 금액 저장 (다음 업데이트에서 변경 감지용)
        binding.tvAvailableAmount.tag = currentAmount
        
        // 첫 로드가 아니고, 이전과 금액이 다를 경우에만 애니메이션 효과 추가
        if (previousAmount != null && currentAmount != previousAmount) {
            android.util.Log.d("TickerWithdrawal", "💰 금액 변경 감지! 이전: $previousAmount, 현재: $currentAmount")
            
            // 색상 애니메이션 (노란색 -> 원래 색상)
            val colorAnimator = android.animation.ValueAnimator.ofObject(
                android.animation.ArgbEvaluator(),
                android.graphics.Color.YELLOW,  // 직접 노란색 사용
                binding.tvAvailableAmount.currentTextColor
            )
            colorAnimator.duration = 1500
            colorAnimator.addUpdateListener { animator ->
                val color = animator.animatedValue as Int
                binding.tvAvailableAmount.setTextColor(color)
            }
            colorAnimator.start()
        } else {
            android.util.Log.d("TickerWithdrawal", "ℹ️ 금액 변경 없음 또는 첫 로드 - 이전: $previousAmount, 현재: $currentAmount")
        }
    }
    
    // 숫자 포맷터 설정 - 항상 소수점 2자리 표시
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
                findNavController().popBackStack()
            }
            
            // UI 업데이트 (가용 잔액, 한도, 수수료 등)
            updateUI()
            updateTotalAmount(0.0)
            
            // 퍼센트 버튼 리스너 설정
            setupPercentageButtons()
            
            // 금액 입력 TextWatcher 설정 - 소수점 2자리 포맷팅 적용
            etAmount.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    // 금액이 변경될 때 총출금 금액 업데이트
                    val amountText = s?.toString() ?: ""
                    if (amountText.isEmpty()) {
                        updateTotalAmount(0.0)
                        return
                    }
                    
                    // 현재 커서 위치 저장
                    val cursorPosition = binding.etAmount.selectionStart
                    
                    // 재귀적 호출 방지를 위한 체크
                    if (amountText.contains(".00$") || 
                        (!amountText.endsWith(".") && amountText.contains(".") && amountText.split(".")[1].length == 2)) {
                        // 이미 올바른 형식이면 처리하지 않음
                        val amount = amountText.replace(",", "").toDoubleOrNull() ?: 0.0
                        updateTotalAmount(amount)
                        return
                    }
                    
                    // 콤마와 소수점 처리
                    val cleanString = amountText.replace(",", "")
                    val parsed = cleanString.toDoubleOrNull() ?: 0.0
                    
                    // 소수점 입력 중인지 확인
                    if (cleanString.endsWith(".")) {
                        // 소수점만 있는 경우 그대로 유지 (예: "123.")
                        val wholeNumberFormatted = NumberFormat.getNumberInstance(Locale.getDefault()).format(parsed)
                        binding.etAmount.removeTextChangedListener(this)
                        binding.etAmount.setText("$wholeNumberFormatted.")
                        // 커서 위치를 소수점 바로 뒤로
                        binding.etAmount.setSelection(("$wholeNumberFormatted.").length)
                        binding.etAmount.addTextChangedListener(this)
                    } else if (cleanString.contains(".") && cleanString.split(".")[1].length <= 2) {
                        // 소수점 이하 1-2자리 있는 경우 (예: "123.4" 또는 "123.45")
                        val parts = cleanString.split(".")
                        val wholeNumber = parts[0].toLongOrNull() ?: 0L
                        val wholeNumberFormatted = NumberFormat.getNumberInstance(Locale.getDefault()).format(wholeNumber)
                        binding.etAmount.removeTextChangedListener(this)
                        binding.etAmount.setText("$wholeNumberFormatted.${parts[1]}")
                        // 커서 위치 계산 및 설정
                        val newCursorPos = if (cursorPosition > wholeNumberFormatted.length + 1) {
                            wholeNumberFormatted.length + 1 + (cursorPosition - cleanString.indexOf(".") - 1)
                        } else {
                            cursorPosition + (wholeNumberFormatted.length - parts[0].length)
                        }
                        binding.etAmount.setSelection(newCursorPos.coerceAtMost("$wholeNumberFormatted.${parts[1]}".length))
                        binding.etAmount.addTextChangedListener(this)
                    } else {
                        // 소수점이 없거나 소수점 이하 숫자가 많은 경우 - 소수점 2자리로 포맷팅
                        val twoDecimalFormat = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }
                        binding.etAmount.removeTextChangedListener(this)
                        binding.etAmount.setText(twoDecimalFormat.format(parsed))
                        binding.etAmount.setSelection(twoDecimalFormat.format(parsed).length)
                        binding.etAmount.addTextChangedListener(this)
                    }
                    
                    // 금액 업데이트
                    updateTotalAmount(parsed)
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
            
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(context, "유효한 금액을 입력해주세요", Toast.LENGTH_SHORT).show()
                return false
            }
            
            if (amount < 1.0) {
                Toast.makeText(context, "\ucd5c\uc18c \ucd9c\uae08 \uae08\uc561\uc740 1 ${currencyCode} \uc785\ub2c8\ub2e4", Toast.LENGTH_SHORT).show()
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
        
        // 항상 최신 자산 데이터 사용
        val latestAsset = ipAsset // ipAsset getter는 이미 최신 데이터를 반환함
        android.util.Log.d("TickerWithdrawal", "navigateToConfirmation - 최신 자산 데이터 잔액: ${latestAsset.amount}")
        
        // 입력한 데이터를 NavDirections를 통해 확인 화면으로 이동
        val withdrawalAmountText = binding.etAmount.text.toString().replace(",", "")
        val withdrawalAmount = withdrawalAmountText.toFloatOrNull() ?: 0.0f
        val withdrawalAddress = binding.etAddress.text.toString().trim()
        
        val action = TickerWithdrawalInputFragmentDirections
            .actionTickerWithdrawalInputFragmentToTickerWithdrawalConfirmFragment(
                latestAsset, // 여기에서 최신 자산 데이터를 전달
                withdrawalAmount,
                fee.toFloat(),
                withdrawalAddress
            )
        findNavController().navigate(action)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        
        // Market API에서 수수료와 출금한도 가져오기
        loadMarketInfo()
        
        bind()
        
        // 전체 앱 데이터 새로고침 이벤트 구독
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.refreshAppDataEvent.collect { refreshTriggered ->
                    if (refreshTriggered) {
                        android.util.Log.d("TickerWithdrawal", "✅ 앱 데이터 새로고침 이벤트 감지")
                        updateUI()
                    }
                }
            }
        }
    }
    
    // No need for additional inflate method as getViewBinding is already implemented
}
