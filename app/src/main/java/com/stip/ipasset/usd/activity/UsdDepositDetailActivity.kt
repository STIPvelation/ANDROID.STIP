package com.stip.ipasset.usd.activity

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.stip.stip.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsdDepositDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvTransactionType: TextView
    private lateinit var tvAmount: TextView
    private lateinit var tvKrwAmount: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnConfirm: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_ip_asset_usd_deposit_transaction_detail)
        
        // Initialize views
        btnBack = findViewById(R.id.btn_back)
        tvTransactionType = findViewById(R.id.tv_transaction_type)
        tvAmount = findViewById(R.id.tv_amount)
        tvKrwAmount = findViewById(R.id.tv_krw_amount)
        tvDate = findViewById(R.id.tv_date) 
        tvStatus = findViewById(R.id.tv_status)
        btnConfirm = findViewById(R.id.btn_confirm)

        // Get transaction data from intent
        val usdAmount = intent.getDoubleExtra("usdAmount", 0.15)
        val krwAmount = intent.getDoubleExtra("krwAmount", 200.0)
        val depositorName = intent.getStringExtra("depositorName") ?: "홍길동"
        val status = intent.getStringExtra("status") ?: "KRW 입금 완료"
        val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())
        
        // Format date
        val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(timestamp))
        
        // Setup UI with data
        setupUI(usdAmount, krwAmount, depositorName, status, formattedDate)
        
        // Setup listeners
        btnBack.setOnClickListener {
            finish()
        }
        
        btnConfirm.setOnClickListener {
            finish()
        }
    }
    
    private fun setupUI(
        usdAmount: Double,
        krwAmount: Double,
        depositorName: String,
        status: String,
        formattedDate: String
    ) {
        // Setup transaction type
        tvTransactionType.text = status
        
        // Setup date
        tvDate.text = formattedDate
        
        // Setup USD amount with new format: "0.15 USD" (콤마 포맷 추가)
        tvAmount.text = String.format("%,.2f USD", usdAmount)
        
        // Setup KRW amount with new format: "200 KRW" (콤마 포맷 추가)
        tvKrwAmount.text = String.format("%,.0f KRW", krwAmount)
        
        // Setup status (transaction type)
        tvStatus.text = "USD 입금"
    }
}
