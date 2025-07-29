package com.stip.ipasset

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.stip.stip.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 출금 Activity
 * API: https://tapi.sharetheip.com/api/wallet/withdraw
 */
@AndroidEntryPoint
class WithdrawActivity : AppCompatActivity() {

    private val withdrawViewModel: WithdrawViewModel by viewModels()
    
    // UI 요소들
    private lateinit var toolbar: MaterialToolbar
    private lateinit var etAmount: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnContinue: Button
    private lateinit var btnPercent10: Button
    private lateinit var btnPercent25: Button
    private lateinit var btnPercent50: Button
    private lateinit var btnPercent100: Button
    private lateinit var tvAvailableAmount: TextView
    private lateinit var tvMaxAmount: TextView
    private lateinit var tvFeeAmount: TextView
    private lateinit var tvTotalFeeAmount: TextView
    
    // 현재 암호화폐 심볼 (예: WETALK)
    private var currentSymbol: String = "WETALK"
    private var availableAmount: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_ip_asset_ticker_withdrawal_input)
        
        initViews()
        setupObservers()
        setupClickListeners()
    }

    /**
     * UI 요소들 초기화
     */
    private fun initViews() {
        toolbar = findViewById(R.id.material_toolbar)
        etAmount = findViewById(R.id.et_amount)
        etAddress = findViewById(R.id.et_address)
        btnContinue = findViewById(R.id.btn_continue)
        btnPercent10 = findViewById(R.id.btn_percent_10)
        btnPercent25 = findViewById(R.id.btn_percent_25)
        btnPercent50 = findViewById(R.id.btn_percent_50)
        btnPercent100 = findViewById(R.id.btn_percent_100)
        tvAvailableAmount = findViewById(R.id.tv_available_amount)
        tvMaxAmount = findViewById(R.id.tv_max_amount)
        tvFeeAmount = findViewById(R.id.tv_fee_amount)
        tvTotalFeeAmount = findViewById(R.id.tv_total_fee_amount)
        
        // 초기 데이터 설정
        availableAmount = 40.0 // 예시 값
        updateAvailableAmount()
    }

    /**
     * 클릭 리스너 설정
     */
    private fun setupClickListeners() {
        // 툴바 뒤로가기 버튼
        toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // 퍼센트 버튼들
        btnPercent10.setOnClickListener { setPercentAmount(10) }
        btnPercent25.setOnClickListener { setPercentAmount(25) }
        btnPercent50.setOnClickListener { setPercentAmount(50) }
        btnPercent100.setOnClickListener { setPercentAmount(100) }
        
        // 출금 신청 버튼
        btnContinue.setOnClickListener {
            val amount = etAmount.text.toString().trim()
            val address = etAddress.text.toString().trim()
            
            if (validateInput(currentSymbol, amount, address)) {
                val amountDouble = amount.toDouble()
                performWithdraw(currentSymbol, amountDouble, address)
            }
        }
    }

    /**
     * 퍼센트 금액 설정
     */
    private fun setPercentAmount(percent: Int) {
        val amount = availableAmount * percent / 100.0
        etAmount.setText(String.format("%.2f", amount))
    }

    /**
     * 사용 가능한 금액 표시 업데이트
     */
    private fun updateAvailableAmount() {
        tvAvailableAmount.text = String.format("%.2f %s", availableAmount, currentSymbol)
    }

    /**
     * ViewModel 옵저버 설정
     */
    private fun setupObservers() {
        lifecycleScope.launch {
            withdrawViewModel.isLoading.collect { isLoading ->
                btnContinue.isEnabled = !isLoading
                if (isLoading) {
                    Log.d("WithdrawActivity", "출금 요청 중...")
                } else {
                    Log.d("WithdrawActivity", "출금 요청 완료")
                }
            }
        }

        lifecycleScope.launch {
            withdrawViewModel.successMessage.collect { message ->
                message?.let {
                    Log.d("WithdrawActivity", "출금 성공: $it")
                    Toast.makeText(this@WithdrawActivity, "출금 성공: $it", Toast.LENGTH_LONG).show()
                    // 성공 시 화면 종료
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            withdrawViewModel.errorMessage.collect { message ->
                message?.let {
                    Log.e("WithdrawActivity", "출금 실패: $it")
                    Toast.makeText(this@WithdrawActivity, "출금 실패: $it", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * 입력값 검증
     */
    private fun validateInput(symbol: String, amountText: String, address: String): Boolean {
        if (symbol.isEmpty()) {
            Toast.makeText(this, "암호화폐 심볼을 입력하세요", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (amountText.isEmpty()) {
            Toast.makeText(this, "출금 금액을 입력하세요", Toast.LENGTH_SHORT).show()
            return false
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "올바른 금액을 입력하세요", Toast.LENGTH_SHORT).show()
            return false
        }

        // 최소 출금 금액 체크
        if (amount < 1.0) {
            Toast.makeText(this, "최소 출금 금액은 1.00 입니다", Toast.LENGTH_SHORT).show()
            return false
        }

        // 사용 가능한 금액 체크
        if (amount > availableAmount) {
            Toast.makeText(this, "출금 가능한 금액을 초과했습니다", Toast.LENGTH_SHORT).show()
            return false
        }

        if (address.isEmpty()) {
            Toast.makeText(this, "출금 주소를 입력하세요", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    /**
     * 출금 요청 실행
     */
    private fun performWithdraw(symbol: String, amount: Double, address: String) {
        Log.d("WithdrawActivity", "출금 요청: symbol=$symbol, amount=$amount, address=$address")
        
        lifecycleScope.launch {
            try {
                // 티커로부터 marketPairId 동적으로 가져오기
                val marketPairId = getMarketPairIdForTicker(symbol)
                if (marketPairId != null) {
                    withdrawViewModel.clearMessages()
                    withdrawViewModel.withdrawCrypto(marketPairId, amount, address)
                } else {
                    Log.e("WithdrawActivity", "marketPairId를 찾을 수 없음: $symbol")
                    Toast.makeText(this@WithdrawActivity, "티커 정보를 찾을 수 없습니다: $symbol", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("WithdrawActivity", "marketPairId 조회 실패: ${e.message}", e)
                Toast.makeText(this@WithdrawActivity, "티커 정보 조회 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 티커로부터 marketPairId 가져오기
     */
    private suspend fun getMarketPairIdForTicker(ticker: String): String? {
        return try {
            val ipListingRepository = com.stip.stip.api.repository.IpListingRepository()
            val marketPairId = ipListingRepository.getPairIdForTicker(ticker)
            marketPairId
        } catch (e: Exception) {
            Log.e("WithdrawActivity", "marketPairId 조회 실패: ${e.message}", e)
            null
        }
    }
}