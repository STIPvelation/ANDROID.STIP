package com.stip.ipasset.usd.activity

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.stip.stip.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsdWithdrawalDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvTransactionType: TextView
    private lateinit var tvAmount: TextView
    private lateinit var tvKrwAmount: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvBankValue: TextView
    private lateinit var btnConfirm: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usd_withdrawal_detail)
        
        // Initialize views
        btnBack = findViewById(R.id.btn_back)
        tvTransactionType = findViewById(R.id.tv_transaction_type)
        tvAmount = findViewById(R.id.tv_amount)
        tvKrwAmount = findViewById(R.id.tv_krw_amount)
        tvDate = findViewById(R.id.tv_date) 
        tvStatus = findViewById(R.id.tv_status)
        tvBankValue = findViewById(R.id.tv_bank_value)
        btnConfirm = findViewById(R.id.btn_confirm)

        // Get transaction data from intent
        val amount = intent.getDoubleExtra("amount", 0.37)
        val krwAmount = intent.getDoubleExtra("krwAmount", 500.0)
        val bankAccount = intent.getStringExtra("bankAccount") ?: "110-123-456789 신한은행"
        val status = intent.getStringExtra("status") ?: "USD 출금 완료"
        val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())
        
        // Format date
        val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
        }
        val formattedDate = dateFormat.format(Date(timestamp))
        
        // Setup UI with data
        setupUI(amount, krwAmount, bankAccount, status, formattedDate)
        
        // Setup listeners
        btnBack.setOnClickListener {
            finish()
        }
        
        btnConfirm.setOnClickListener {
            finish()
        }
    }
    
    private fun setupUI(
        amount: Double,
        krwAmount: Double,
        bankAccount: String,
        status: String,
        formattedDate: String
    ) {
        // Setup transaction type
        tvTransactionType.text = status
        
        // Setup date
        tvDate.text = formattedDate
        
        // Setup USD amount with new format: "0.37 USD" (콤마 포맷 추가)
        tvAmount.text = String.format("%,.2f USD", amount)
        
        // Setup KRW amount with new format: "500 KRW" (콤마 포맷 추가)
        tvKrwAmount.text = String.format("%,.0f KRW", krwAmount)
        
        // Setup status (transaction type)
        tvStatus.text = "USD 출금"
        
        // Setup bank account info
        tvBankValue.text = bankAccount
    }
}
