package com.stip.ipasset.usd.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.Editable
import android.text.TextWatcher
import android.text.InputType
import androidx.lifecycle.Observer
import com.stip.stip.R
import com.stip.stip.databinding.FragmentIpAssetUsdWithdrawalInputBinding
import com.stip.ipasset.fragment.BaseFragment
import com.stip.stip.MainActivity
// import com.stip.ipasset.usd.adapter.NumericKeypadAdapter  // 커스텀 키패드 주석처리
import com.stip.ipasset.usd.manager.USDAssetManager
import com.stip.api.repository.PortfolioRepository
import com.stip.stip.api.repository.MemberRepository
import com.stip.stip.signup.utils.PreferenceUtil
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * USD 출금 입력 화면
 */
class USDWithdrawalInputFragment : BaseFragment<FragmentIpAssetUsdWithdrawalInputBinding>() {



    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentIpAssetUsdWithdrawalInputBinding {
        return FragmentIpAssetUsdWithdrawalInputBinding.inflate(inflater, container, false)
    }

    // 커스텀 키패드 관련 변수들 (주석처리)
    /*
    // 현재 입력값 저장 변수
    private var currentInput = "1"
    
    // 소수점 입력 모드 추적
    private var decimalMode = false
    
    // 소수점 자리수 추적
    private var decimalDigits = 0
    */

    // USD 자산 매니저
    private val assetManager = USDAssetManager.getInstance()
    
    // Market API 관련
    private val marketRepository: com.stip.stip.api.repository.MarketRepository by lazy { 
        com.stip.stip.api.repository.MarketRepository() 
    }
    
    // Member API 관련
    private val memberRepository: MemberRepository by lazy { 
        MemberRepository() 
    }

    override fun onResume() {
        super.onResume()
        // 헤더 숨기기
        (activity as? MainActivity)?.setHeaderVisibility(false)
        
        // 화면에 돌아올 때 데이터 새로고침
        assetManager.refreshData()
        // 출금가능 금액 다시 포맷팅
        formatWithdrawableAmount()
        
        // Market API에서 수수료와 출금한도 가져오기
        loadMarketInfo()
        
        // 사용자 계좌번호 정보 가져오기
        loadUserAccountInfo()
        
        // 커스텀 키패드 초기화 (주석처리)
        /*
        // 초기 입력값 리셋
        currentInput = "1"
        decimalMode = false
        decimalDigits = 0
        updateDisplay()
        */
    }
    
    /**
     * Market API에서 수수료와 출금한도 정보를 가져옴
     */
    private fun loadMarketInfo() {
        lifecycleScope.launch {
            try {
                // 로딩 상태 표시
                // binding.progressBar.visibility = View.VISIBLE
                
                // 포트폴리오 API에서 USD의 marketPairId 가져오기
                val memberId = com.stip.stip.signup.utils.PreferenceUtil.getUserId()
                if (memberId.isNullOrBlank()) {
                    android.util.Log.w("USDWithdrawal", "사용자 ID가 없습니다. 기본값 사용")
                    setDefaultMarketValues()
                    return@launch
                }
                
                val portfolioRepository = com.stip.api.repository.PortfolioRepository()
                val portfolioResponse = portfolioRepository.getPortfolioResponse(memberId)
                val usdWallet = portfolioResponse?.wallets?.find { it.symbol.equals("USD", ignoreCase = true) }
                val marketPairId = usdWallet?.marketPairId
                
                if (marketPairId != null) {
                    android.util.Log.d("USDWithdrawal", "포트폴리오에서 USD marketPairId 찾음: $marketPairId")
                    android.util.Log.d("USDWithdrawal", "Market API 호출 시작: marketPairId=$marketPairId")
                    
                    // Market API 호출
                    val marketResponse = marketRepository.getMarket(marketPairId)
                    
                    if (marketResponse != null) {
                        // API에서 가져온 값으로 USDAssetManager 업데이트
                        assetManager.setFee(marketResponse.fee)
                        assetManager.setWithdrawalLimit(marketResponse.maxValue)
                        
                        android.util.Log.d("USDWithdrawal", "Market API 성공: fee=${marketResponse.fee}, maxValue=${marketResponse.maxValue}")
                        
                        // UI 즉시 업데이트 (Fragment가 연결되어 있는 경우에만)
                        if (isAdded) {
                            updateUIWithMarketData(marketResponse.fee, marketResponse.maxValue)
                        }
                    } else {
                        android.util.Log.d("USDWithdrawal", "Market API 응답이 null입니다. 기본값 사용")
                        // 기본값 설정
                        setDefaultMarketValues()
                    }
                } else {
                    android.util.Log.d("USDWithdrawal", "포트폴리오에서 USD marketPairId를 찾을 수 없습니다. 기본값 사용")
                    // 기본값 설정
                    setDefaultMarketValues()
                }
            } catch (e: Exception) {
                android.util.Log.e("USDWithdrawal", "Market API 호출 실패: ${e.message}", e)
                // 에러 발생 시 기본값 설정
                setDefaultMarketValues()
            } finally {
            }
        }
    }
    
    /**
     * 기본 마켓 값 설정 (API 호출 실패 시)
     */
    private fun setDefaultMarketValues() {
        if (isAdded) {
            android.util.Log.w("USDWithdrawal", "API 호출 실패로 기본값을 설정하지 않습니다.")
            
            // UI에 로딩 상태 표시
            binding.textLimit.text = "로딩 중..."
            binding.feeAmount.text = "로딩 중..."
            binding.totalWithdrawalAmount.text = "로딩 중..."
        }
    }
    
    /**
     * 사용자 계좌번호 정보 조회
     */
    private fun loadUserAccountInfo() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("USDWithdrawal", "사용자 계좌번호 정보 조회 시작")
                
                val memberInfo = memberRepository.getMemberInfo()
                
                if (memberInfo != null) {
                    android.util.Log.d("USDWithdrawal", "사용자 정보 조회 성공: ${memberInfo.name}")
                    android.util.Log.d("USDWithdrawal", "은행코드: ${memberInfo.bankCode}, 계좌번호: ${memberInfo.accountNumber}")
                    
                    // UI 업데이트
                    if (isAdded) {
                        updateAccountInfoUI(memberInfo.bankCode, memberInfo.accountNumber, memberInfo.name)
                    }
                } else {
                    android.util.Log.w("USDWithdrawal", "사용자 정보가 없습니다.")
                    if (isAdded) {
                        setDefaultAccountInfo()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("USDWithdrawal", "사용자 계좌번호 정보 조회 실패", e)
                if (isAdded) {
                    setDefaultAccountInfo()
                }
            }
        }
    }
    
    /**
     * 계좌번호 정보 UI 업데이트
     */
    private fun updateAccountInfoUI(bankCode: String, accountNumber: String, userName: String) {
        try {
            // 은행명 가져오기
            val bankName = getBankName(bankCode)
            
            // 계좌번호 포맷팅
            val formattedAccountNumber = com.stip.stip.signup.utils.Utils.formatAccountNumber(bankCode, accountNumber)
            
            // UI 업데이트시
            binding.textBank.text = "$bankName $formattedAccountNumber"
            
            android.util.Log.d("USDWithdrawal", "계좌번호 UI 업데이트 완료: $bankName $formattedAccountNumber")
        } catch (e: Exception) {
            android.util.Log.e("USDWithdrawal", "계좌번호 UI 업데이트 실패", e)
            setDefaultAccountInfo()
        }
    }
    
    /**
     * 기본 계좌번호 정보 설정
     */
    private fun setDefaultAccountInfo() {
        binding.textBank.text = "로딩 중..."
    }
    
    /**
     * 은행코드로 은행명 가져오기
     */
    private fun getBankName(bankCode: String): String {
        return when (bankCode) {
            "002" -> "산업은행"
            "003" -> "기업은행"
            "004" -> "국민은행"
            "007" -> "수협은행"
            "011" -> "농협은행"
            "012" -> "농협중앙회"
            "020" -> "우리은행"
            "023" -> "SC제일은행"
            "027" -> "한국씨티은행"
            "031" -> "대구은행"
            "032" -> "부산은행"
            "034" -> "광주은행"
            "035" -> "제주은행"
            "037" -> "전북은행"
            "039" -> "경남은행"
            "045" -> "새마을금고"
            "048" -> "신협"
            "050" -> "상호저축은행"
            "054" -> "HSBC은행"
            "055" -> "도이치은행"
            "057" -> "JP모간체이스은행"
            "060" -> "BOA은행"
            "081" -> "하나은행"
            "088" -> "신한은행"
            "089" -> "케이뱅크"
            "090" -> "카카오뱅크"
            "092" -> "토스뱅크"
            else -> "기타은행"
        }
    }
    
    /**
     * 마켓 데이터로 UI 업데이트
     */
    private fun updateUIWithMarketData(fee: Double, maxValue: Double) {
        try {
            val formatter = java.text.DecimalFormat("#,##0.00")
            
            // 출금 한도 업데이트
            binding.textLimit.text = "${formatter.format(maxValue)} USD"
            
            // 수수료 업데이트
            binding.feeAmount.text = "${formatter.format(fee)} USD"
            
            // 현재 입력된 출금 금액에 대한 총 출금액 다시 계산
            val currentAmount = binding.withdrawalInput.text.toString().toDoubleOrNull() ?: 1.0
            updateFeeAndTotal(currentAmount)
            
            android.util.Log.d("USDWithdrawal", "UI 업데이트 완료: fee=$fee, maxValue=$maxValue")
        } catch (e: Exception) {
            android.util.Log.e("USDWithdrawal", "UI 업데이트 실패: ${e.message}", e)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 커스텀 키패드 초기화 (주석처리)
        // updateDisplay()

        // 먼저 초기 데이터 포맷팅 시도
        formatWithdrawableAmount()
        
        // 출금가능 금액, 출금 한도 관찰 설정
        observeAssetData()
        
        // 뒤로 가기 버튼 설정
        binding.materialToolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // 출금 가능 금액 정보 버튼 클릭 시 다이얼로그 표시
        binding.btnWithdrawableInfo.setOnClickListener {
            showWithdrawalAvailableInfoDialog()
        }

        // 출금 한도 정보 버튼 클릭 시 다이얼로그 표시
        binding.btnWithdrawalLimitInfo.setOnClickListener {
            showWithdrawalLimitInfoDialog()
        }

        // 커스텀 키패드 설정 (주석처리)
        // setupKeypad()
        
        // EditText 설정 (시스템 키보드 사용)
        setupEditText()
        
        // 퍼센트 버튼 클릭 리스너 설정
        setupPercentageButtons()
        
        // 커스텀 키패드 표시/숨김 (주석처리)
        /*
        // 초기에는 키패드 숨김
        binding.rvKeypad.visibility = View.GONE
        
        // 출금금액 입력 컨테이너 클릭 시 키패드 표시
        binding.withdrawalInputContainer.setOnClickListener {
            // 키패드가 이미 표시되어 있으면 숨기고, 숨겨져 있으면 표시
            if (binding.rvKeypad.visibility == View.VISIBLE) {
                binding.rvKeypad.visibility = View.GONE
            } else {
                binding.rvKeypad.visibility = View.VISIBLE
                // 토스트 메시지 제거 - 사용자 요청에 따라
            }
        }
        */

        // 출금 확인 버튼 텍스트 수정 및 클릭 이벤트 설정
        binding.btnWithdrawalApply.text = "출금신청"
        binding.btnWithdrawalApply.setOnClickListener {
            navigateToConfirmScreen()
        }
    }

    /**
     * EditText 설정 (시스템 키보드 사용)
     */
    private fun setupEditText() {
        // EditText를 숫자 입력으로 설정
        binding.withdrawalInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        
        // 초기값 설정
        binding.withdrawalInput.setText("1.00")
        
        // 텍스트 변경 리스너 설정
        binding.withdrawalInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                if (input.isNotEmpty()) {
                    // 소수점 2자리 제한 처리
                    val processedInput = limitDecimalPlaces(input)
                    if (processedInput != input) {
                        // 입력값이 제한을 초과했으면 수정된 값으로 교체
                        binding.withdrawalInput.removeTextChangedListener(this)
                        binding.withdrawalInput.setText(processedInput)
                        binding.withdrawalInput.setSelection(processedInput.length)
                        binding.withdrawalInput.addTextChangedListener(this)
                        return
                    }
                    
                    try {
                        val amount = input.toDoubleOrNull() ?: 1.0
                        
                        // 출금 한도 검증
                        val withdrawalLimit = assetManager.withdrawalLimit.value
                        if (withdrawalLimit != null && amount > withdrawalLimit) {
                            // 출금 한도를 초과한 경우 한도 값으로 제한
                            val formatter = java.text.DecimalFormat("#,##0.00")
                            val limitedAmount = withdrawalLimit
                            
                            // 입력값을 한도 값으로 교체
                            binding.withdrawalInput.removeTextChangedListener(this)
                            binding.withdrawalInput.setText(formatter.format(limitedAmount))
                            binding.withdrawalInput.setSelection(formatter.format(limitedAmount).length)
                            binding.withdrawalInput.addTextChangedListener(this)
                            
                            // 토스트 메시지 표시
                            android.widget.Toast.makeText(requireContext(), "출금 한도를 초과하여 ${formatter.format(limitedAmount)} USD로 제한됩니다.", android.widget.Toast.LENGTH_SHORT).show()
                            
                            updateFeeAndTotal(limitedAmount)
                        } else {
                            updateFeeAndTotal(amount)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("USDWithdrawal", "Error parsing input: ${e.message}")
                    }
                }
            }
        })
    }
    
    /**
     * 소수점 2자리까지만 허용하는 함수
     */
    private fun limitDecimalPlaces(input: String): String {
        if (input.isEmpty()) return input
        
        // 소수점이 있는지 확인
        val decimalIndex = input.indexOf('.')
        if (decimalIndex == -1) {
            // 소수점이 없으면 그대로 반환
            return input
        }
        
        // 소수점 이하 자릿수 확인
        val decimalPlaces = input.length - decimalIndex - 1
        if (decimalPlaces <= 2) {
            // 2자리 이하면 그대로 반환
            return input
        }
        
        // 2자리를 초과하면 2자리까지만 잘라서 반환
        return input.substring(0, decimalIndex + 3)
    }

    /**
     * 출금 가능 금액 정보 다이얼로그 표시
     */
    private fun showWithdrawalAvailableInfoDialog() {
        val dialogFragment = USDInfoDialogFragment()
        val args = Bundle()
        args.putString("dialog_layout", "dialog_ip_asset_usd_withdrawal_available_info")
        dialogFragment.arguments = args
        dialogFragment.show(childFragmentManager, "withdrawal_available_info_dialog")
    }

    /**
     * 출금 한도 정보 다이얼로그 표시
     */
    private fun showWithdrawalLimitInfoDialog() {
        val dialogFragment = USDInfoDialogFragment()
        val args = Bundle()
        args.putString("dialog_layout", "dialog_ip_asset_usd_withdrawal_limit_info")
        dialogFragment.arguments = args
        dialogFragment.show(childFragmentManager, "withdrawal_limit_info_dialog")
    }
    
    // 커스텀 키패드 관련 메서드들 (주석처리)
    /*
    /**
     * 키패드 설정 및 처리 기능 구현
     */
    private fun setupKeypad() {
        val keypadAdapter = NumericKeypadAdapter { key ->
            when (key) {
                "<" -> deleteLastDigit()
                "완료" -> completeInput()
                else -> addDigit(key)
            }
        }
        
        // 키패드에 어댑터 설정
        binding.rvKeypad.adapter = keypadAdapter
    }
    
    /**
     * 숫자 추가
     */
    private fun addDigit(digit: String) {
        when (digit) {
            "." -> handleDecimalPoint()
            else -> handleNumericDigit(digit)
        }
        updateDisplay()
    }
    
    /**
     * 소수점 입력 처리
     */
    private fun handleDecimalPoint() {
        // 이미 소수점 모드면 무시
        if (decimalMode) return
        
        decimalMode = true
        decimalDigits = 0
        
        // 소수점 추가
        if (currentInput.isEmpty()) {
            currentInput = "0."
        } else {
            currentInput += "."
        }
    }
    
    /**
     * 숫자 입력 처리
     */
    private fun handleNumericDigit(digit: String) {
        // 초기 상태이면 초기화
        if (currentInput == "1" && !decimalMode) {
            currentInput = digit
            return
        }
        
        // 소수점 모드인 경우
        if (decimalMode) {
            // 소수점 이하 최대 2자리까지만 허용
            if (decimalDigits < 2) {
                currentInput += digit
                decimalDigits++
            }
        } else {
            // 일반 숫자 모드
            currentInput += digit
        }
    }
    
    /**
     * 마지막 숫자 삭제
     */
    private fun deleteLastDigit() {
        if (currentInput.isEmpty()) {
            return
        }
        
        // 콤마 제거
        val rawValue = currentInput.replace(",", "")
        
        // 마지막 문자가 소수점인지 확인
        if (rawValue.endsWith(".")) {
            decimalMode = false
            decimalDigits = 0
        } 
        // 소수점 이하 자리를 지울 경우
        else if (decimalMode && rawValue.contains(".")) {
            if (decimalDigits > 0) {
                decimalDigits--
            }
        }
        
        // 마지막 문자 삭제
        if (rawValue.length > 1) {
            currentInput = rawValue.substring(0, rawValue.length - 1)
        } else {
            // 하나의 숫자만 남았을 경우 "1"로 설정
            currentInput = "1"
            decimalMode = false
            decimalDigits = 0
        }
        
        // 숫자만 남았는데 모두 지워졌다면 "1"로 설정
        if (currentInput.isEmpty()) {
            currentInput = "1"
            decimalMode = false
            decimalDigits = 0
        }
        
        updateDisplay()
    }
    
    /**
     * 완료 버튼 처리
     */
    private fun completeInput() {
        // 키패드 숨기기
        binding.rvKeypad.visibility = View.GONE
        // 선택적: 입력 완료 피드백
        android.widget.Toast.makeText(requireContext(), "입력이 완료되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 화면 표시 업데이트 - 입력 중인 금액을 있는 그대로 표시하면서 필요한 포맷팅만 적용
     * 소수점 2자리까지 항상 표시하도록 수정
     */
    private fun updateDisplay() {
        try {
            var displayText: String
            var amount = 0.0
            
            // 1. 입력값 처리
            if (currentInput.isEmpty()) {
                displayText = "1.00"
                amount = 1.0
            } else {
                val cleanInput = currentInput.replace(",", "")
                amount = cleanInput.toDoubleOrNull() ?: 1.0
                
                // 소수점 입력 중인 경우 특수 처리
                if (decimalMode && decimalDigits < 2) {
                    // 소수점만 입력된 경우 (예: "123.")
                    if (cleanInput.endsWith(".")) {
                        val wholeNumber = formatWholeNumber(cleanInput.dropLast(1))
                        displayText = wholeNumber + "."
                    } 
                    // 소수점 이하 1자리만 입력된 경우 (예: "123.4")
                    else if (cleanInput.contains(".")) {
                        val parts = cleanInput.split(".")
                        val wholeNumber = formatWholeNumber(parts[0])
                        displayText = wholeNumber + "." + parts[1]
                    } else {
                        // 정수만 있는 경우
                        displayText = formatWholeNumber(cleanInput)
                    }
                } else {
                    // 소수점 완성되었거나 정수인 경우 - 항상 소수점 2자리 표시
                    val formatter = java.text.DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }
                    displayText = formatter.format(amount)
                    // 소수점 모드와 자릿수 업데이트
                    if (amount % 1 != 0.0) {
                        decimalMode = true
                        decimalDigits = 2
                    }
                }
            }
            
            // 2. 최대 출금 가능 금액 체크
            val fee = assetManager.fee.value ?: 1.0
            val totalWithdrawable = assetManager.withdrawableAmount.value ?: 10000.0
            val maxAmount = totalWithdrawable - fee
            
            // 최대 금액 초과 시 제한 (maxAmount가 0.0이 아닐 때만 체크)
            if (maxAmount > 0.0 && amount > maxAmount) {
                val formatter = java.text.DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }
                displayText = formatter.format(maxAmount)
                currentInput = maxAmount.toString()
                decimalMode = true
                decimalDigits = 2
            }
            
            // 3. UI에 표시
            binding.withdrawalInput.text = displayText
            
            // 4. 수수료 및 총 출금액 업데이트 (최종 계산용으로는 완성된 숫자 사용)
            val finalAmount = if (decimalMode && decimalDigits == 0) {
                // 소수점만 입력된 경우 (예: "10.")
                amount
            } else if (decimalMode && decimalDigits < 2) {
                // 소수점 자리가 부족한 경우, 계산을 위해 0 추가
                val amountStr = currentInput + "0".repeat(2 - decimalDigits)
                amountStr.toDoubleOrNull() ?: amount
            } else {
                amount
            }
            
            updateFeeAndTotal(finalAmount)
            
            android.util.Log.d("USDWithdrawal", "Input: $currentInput, Display: $displayText, Amount: $finalAmount")
            
        } catch (e: Exception) {
            // 오류 발생 시 기본값으로 리셋
            binding.withdrawalInput.text = "1.00"
            currentInput = "1"
            decimalMode = false
            decimalDigits = 0
            android.util.Log.e("USDWithdrawal", "Error updating display: ${e.message}")
        }
    }
    
    /**
     * 정수 부분에만 천 단위 콤마 적용
     */
    private fun formatWholeNumber(number: String): String {
        if (number.isEmpty()) return "0"
        
        return try {
            val parsed = number.toLong()
            java.text.DecimalFormat("#,###").format(parsed)
        } catch (e: Exception) {
            number
        }
    }
    */
    
    /**
     * 퍼센트 버튼 설정
     */
    private fun setupPercentageButtons() {
        // 10% 버튼
        binding.btn10Percent.setOnClickListener {
            setPercentageAmount(0.1)
        }
        
        // 25% 버튼
        binding.btn25Percent.setOnClickListener {
            setPercentageAmount(0.25)
        }
        
        // 50% 버튼
        binding.btn50Percent.setOnClickListener {
            setPercentageAmount(0.5)
        }
        
        // MAX 버튼 - 수수료를 제외한 최대 출금 가능 금액으로 설정
        binding.btnMax.setOnClickListener {
            setMaxAmount()
        }
    }
    
    /**
     * 퍼센트에 해당하는 금액 설정
     */
    private fun setPercentageAmount(percentage: Double) {
        val withdrawableAmount = assetManager.withdrawableAmount.value
        val fee = assetManager.fee.value
        val withdrawalLimit = assetManager.withdrawalLimit.value
        
        // API에서 값을 가져오지 못한 경우 처리
        if (withdrawableAmount == null || fee == null) {
            android.util.Log.w("USDWithdrawal", "API 데이터가 없어 퍼센트 버튼을 사용할 수 없습니다.")
            android.widget.Toast.makeText(requireContext(), "잠시 후 다시 시도해주세요.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        // 출금 가능한 금액의 비율 계산 (수수료 고려)
        val maxAmount = withdrawableAmount - fee
        val percentAmount = maxAmount * percentage
        
        // 최소 금액 보장 (1 USD)
        var finalAmount = percentAmount.coerceAtLeast(1.0)
        
        // 출금 한도 확인
        if (withdrawalLimit != null && finalAmount > withdrawalLimit) {
            finalAmount = withdrawalLimit
            android.util.Log.d("USDWithdrawal", "출금 한도로 제한됨: $finalAmount USD")
        }
        
        // 소수점 자리를 포함한 금액으로 설정
        val formattedAmount = String.format("%.2f", finalAmount)
        binding.withdrawalInput.setText(formattedAmount)
        
        android.util.Log.d("USDWithdrawal", "Selected ${percentage * 100}%: $finalAmount USD")
    }
    
    /**
     * 최대 출금 가능 금액 설정
     */
    private fun setMaxAmount() {
        val withdrawableAmount = assetManager.withdrawableAmount.value
        val fee = assetManager.fee.value
        val withdrawalLimit = assetManager.withdrawalLimit.value
        
        // API에서 값을 가져오지 못한 경우 처리
        if (withdrawableAmount == null || fee == null) {
            android.util.Log.w("USDWithdrawal", "API 데이터가 없어 MAX 버튼을 사용할 수 없습니다.")
            android.widget.Toast.makeText(requireContext(), "잠시 후 다시 시도해주세요.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        // 출금 가능 금액과 출금 한도 중 작은 값 선택
        val maxFromWithdrawable = withdrawableAmount - fee
        val maxAmount = if (withdrawalLimit != null) {
            minOf(maxFromWithdrawable, withdrawalLimit)
        } else {
            maxFromWithdrawable
        }
        
        // 소수점 자리를 포함한 최대 금액으로 설정
        val formattedAmount = String.format("%.2f", maxAmount)
        binding.withdrawalInput.setText(formattedAmount)
        
        android.util.Log.d("USDWithdrawal", "Selected MAX: $maxAmount USD (출금가능: $maxFromWithdrawable, 한도: ${withdrawalLimit ?: "설정되지 않음"})")
    }
    
    /**
     * 수수료 및 총 출금액 업데이트
     */
    private fun updateFeeAndTotal(amount: Double) {
        try {
            // 수수료 계산
            val fee = assetManager.fee.value
            
            // API에서 수수료를 가져오지 못한 경우 처리
            if (fee == null) {
                android.util.Log.w("USDWithdrawal", "수수료 정보가 없어 계산할 수 없습니다.")
                binding.feeAmount.text = "계산 중..."
                binding.totalWithdrawalAmount.text = "계산 중..."
                return
            }
            
            // 총 출금액 계산
            val total = amount + fee
            
            // 포맷터
            val formatter = java.text.DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }
            
            // UI 업데이트
            binding.feeAmount.text = "${formatter.format(fee)} USD"
            binding.totalWithdrawalAmount.text = formatter.format(total)
        } catch (e: Exception) {
            android.util.Log.e("USDWithdrawal", "Error updating fee and total", e)
        }
    }
    
    /**
     * 출금 확인 화면으로 이동
     */
    private fun navigateToConfirmScreen() {
        try {
            // 입력된 출금 금액 가져오기
            val withdrawalAmount = binding.withdrawalInput.text.toString().toDoubleOrNull() ?: 0.0
            
            // 최소 금액 확인
            if (withdrawalAmount < 1.0) {
                android.widget.Toast.makeText(requireContext(), "최소 출금 금액은 1 USD입니다.", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            
            // 수수료 가져오기
            val fee = assetManager.fee.value
            if (fee == null) {
                android.widget.Toast.makeText(requireContext(), "수수료 정보를 가져오는 중입니다. 잠시 후 다시 시도해주세요.", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            
            // 출금 가능 금액 확인
            val withdrawable = assetManager.withdrawableAmount.value
            if (withdrawable == null) {
                android.widget.Toast.makeText(requireContext(), "출금 가능 금액 정보를 가져오는 중입니다. 잠시 후 다시 시도해주세요.", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            
            if (withdrawalAmount + fee > withdrawable) {
                android.widget.Toast.makeText(requireContext(), "출금 가능 금액을 초과했습니다.", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            
            // 출금 한도 확인 (Market API에서 가져온 maxValue)
            val withdrawalLimit = assetManager.withdrawalLimit.value
            if (withdrawalLimit != null && withdrawalAmount > withdrawalLimit) {
                android.widget.Toast.makeText(requireContext(), "출금 한도를 초과했습니다. (최대 ${String.format("%.0f", withdrawalLimit)} USD)", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            
            // 애니메이션 트랜지션 설정
            val fragmentManager = requireActivity().supportFragmentManager
            val fragmentTransaction = fragmentManager.beginTransaction()
            
            fragmentTransaction.setCustomAnimations(
                com.stip.stip.R.anim.slide_in_right,
                com.stip.stip.R.anim.slide_out_left,
                com.stip.stip.R.anim.slide_in_left,
                com.stip.stip.R.anim.slide_out_right
            )
            
            // 확인 화면 프래그먼트 생성
            val confirmFragment = USDWithdrawalConfirmFragment()
            
            // 금액 정보 전달
            val args = Bundle()
            args.putDouble("withdrawal_amount", withdrawalAmount)
            args.putDouble("fee", fee)
            
            // 계좌번호 정보 전달
            val accountInfo = binding.textBank.text.toString()
            args.putString("account_info", accountInfo)
            
            confirmFragment.arguments = args
            
            fragmentTransaction.replace(com.stip.stip.R.id.fragment_container, confirmFragment)
            fragmentTransaction.addToBackStack(null)
            fragmentTransaction.commit()
            
            android.util.Log.d("USDWithdrawal", "Navigating to confirm screen with amount: $withdrawalAmount, fee: $fee")
            
        } catch (e: Exception) {
            android.widget.Toast.makeText(requireContext(), "처리 중 오류가 발생했습니다: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            android.util.Log.e("USDWithdrawal", "Error navigating to confirm screen", e)
        }
    }
    
    /**
     * USDAssetManager의 데이터 변화 관찰
     */
    private fun observeAssetData() {
        // 한 번만 formatter 인스턴스 생성
        val formatter = java.text.DecimalFormat("#,##0.00")
        
        // 출금 가능 금액 관찰
        assetManager.withdrawableAmount.observe(viewLifecycleOwner) { amount ->
            val formattedAmount = formatter.format(amount)
            binding.textWithdrawable.text = "$formattedAmount USD"
        }

        // 출금 한도 관찰
        assetManager.withdrawalLimit.observe(viewLifecycleOwner) { limit ->
            val formattedLimit = formatter.format(limit)
            binding.textLimit.text = "$formattedLimit USD"
        }
        
        // 출금 수수료 관찰
        assetManager.fee.observe(viewLifecycleOwner) { fee ->
            val formattedFee = formatter.format(fee)
            binding.feeAmount.text = "$formattedFee USD"
        }
    }
    
    /**
     * 출금가능 금액을 소수점 2자리로 포맷팅 (초기값 설정용)
     */
    private fun formatWithdrawableAmount() {
        // 명시적으로 USDAssetManager에 데이터 리프레시 요청
        assetManager.refreshData()
        
        // 현재 값을 즉시 반영 (LiveData 업데이트 전에)
        val formatter = java.text.DecimalFormat("#,##0.00")
        
        // 기본값을 사용하여 UI 초기화 - LiveData 변경 전에 표시되는 값
        val withdrawableAmount = assetManager.withdrawableAmount.value ?: 10000.0
        val formattedWithdrawable = formatter.format(withdrawableAmount)
        binding.textWithdrawable.text = "$formattedWithdrawable USD"
        
        android.util.Log.d("USDWithdrawalInputFragment", "Initial withdrawable amount: $formattedWithdrawable USD")
    }
}
