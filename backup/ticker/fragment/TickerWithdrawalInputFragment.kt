package com.stip.ipasset.ticker.fragment

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.stip.dummy.FeeAndLimitsDummyData
import com.stip.ipasset.repository.IpAssetRepository
import com.stip.stip.R
import com.stip.stip.databinding.FragmentIpAssetTickerWithdrawalInputBinding
import com.stip.stip.ipasset.fragment.BaseFragment
import com.stip.stip.ipasset.model.IpAsset
import com.stip.stip.ipasset.WithdrawalInputViewModel
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
    private val args by navArgs<com.stip.ipasset.ticker.fragment.TickerWithdrawalInputFragmentArgs>()
    private val ipAsset: IpAsset get() = args.ipAsset
    private val currencyCode: String get() = ipAsset.currencyCode

    private val viewModel by viewModels<WithdrawalInputViewModel>()
    
    // 출금 가능한 최대 금액 (저장소에서 가져옴)
    private val repository: IpAssetRepository by lazy { IpAssetRepository.getInstance(requireContext()) }
    private val availableAmount: Double
        get() = (repository.getAsset(ipAsset.id)?.amount ?: ipAsset.amount).toDouble()
    private val maxAmount: Double
        get() = FeeAndLimitsDummyData.getMaxWithdrawalAmount(currencyCode)
    private val fee: Double
        get() = FeeAndLimitsDummyData.getWithdrawalFee(currencyCode)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 테스트 데이터 설정 (실제 앱에서는 API에서 가져와야 함)
        if (repository.getAsset(ipAsset.id) == null) {
            IpAssetRepository.setTestData(listOf(ipAsset))
        }
    }
    
    // 숫자 포맷터 설정
    private val decimalFormat = DecimalFormat("#,##0.00")
    private val amountFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    
    private fun bind() = with(viewBinding) {
        // 툴바 설정
        materialToolbar.title = "$currencyCode ${getString(R.string.common_withdrawal_action)}"
        materialToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        
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
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // 금액이 변경될 때 총출금 금액 업데이트
                val amountText = s?.toString() ?: ""
                val amount = amountText.replace(",", "").toDoubleOrNull() ?: 0.0
                updateTotalAmount(amount)
                
                // 콤마(,) 포맷팅 적용
                if (s != null && !amountText.isEmpty()) {
                    // 현재 텍스트 변경 중 재귀적인 호출을 방지하기 위한 플래그
                    if (!s.toString().contains(",") || s.toString().matches(Regex("[0-9]+,[0-9]{3}.*"))) {
                        // 콤마 제거 후 숫자만 추출
                        val cleanString = s.toString().replace(",", "")
                        val parsed = cleanString.toDoubleOrNull() ?: 0.0
                        
                        // 3자리마다 콤마 추가한 포맷 적용
                        val formatted = NumberFormat.getNumberInstance(Locale.getDefault()).format(parsed)
                        
                        // 이전 리스너 제거 -> 텍스트 변경 -> 리스너 다시 추가
                        etAmount.removeTextChangedListener(this)
                        etAmount.setText(formatted)
                        etAmount.setSelection(formatted.length)
                        etAmount.addTextChangedListener(this)
                    }
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
    
    // 총출금 금액 업데이트 (수수료 포함)
    private fun updateTotalAmount(withdrawalAmount: Double) {
        val totalAmount = withdrawalAmount + fee
        viewBinding.tvTotalFeeAmount.text = decimalFormat.format(totalAmount) + " $currencyCode"
    }
    
    private fun setupPercentageButtons() = with(viewBinding) {
        btnPercent10.setOnClickListener { calculateAmountByPercentage(0.1) }
        btnPercent25.setOnClickListener { calculateAmountByPercentage(0.25) }
        btnPercent50.setOnClickListener { calculateAmountByPercentage(0.5) }
        btnPercent100.setOnClickListener { calculateAmountByPercentage(1.0) }
    }
    
    private fun calculateAmountByPercentage(percentage: Double) = with(viewBinding) {
        val calculatedAmount = if (percentage == 1.0) {
            // 100% 버튼을 누를 경우 수수료를 고려하여 최대 출금 가능액을 계산
            availableAmount - fee
        } else {
            availableAmount * percentage
        }
        etAmount.setText(amountFormat.format(calculatedAmount))
    }
    
    private fun validateInputs(): Boolean = with(viewBinding) {
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

    private fun navigateToConfirmation() {
        // 입력한 데이터를 NavDirections를 통해 확인 화면으로 이동
        val withdrawalAmountText = viewBinding.etAmount.text.toString().replace(",", "")
        val withdrawalAmount = withdrawalAmountText.toFloatOrNull() ?: 0.0f
        val withdrawalAddress = viewBinding.etAddress.text.toString().trim()
        
        val action = TickerWithdrawalInputFragmentDirections
            .actionTickerWithdrawalInputFragmentToTickerWithdrawalConfirmFragment(
                ipAsset,
                withdrawalAmount,
                fee.toFloat(),
                withdrawalAddress
            )
        findNavController().navigate(action)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind()
    }

    override fun inflate(inflater: LayoutInflater, container: ViewGroup?): FragmentIpAssetTickerWithdrawalInputBinding {
        return FragmentIpAssetTickerWithdrawalInputBinding.inflate(inflater, container, false)
    }
}
