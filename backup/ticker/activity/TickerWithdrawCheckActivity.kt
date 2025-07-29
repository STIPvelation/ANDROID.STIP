package com.stip.ipasset.ticker.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.stip.stip.R
import com.stip.stip.databinding.ActivityTickerWithdrawDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 티커 출금 완료 상세화면
 */
class TickerWithdrawCheckActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTickerWithdrawDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTickerWithdrawDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 인텐트에서 트랜잭션 데이터 추출
        val transactionId = intent.getLongExtra("transactionId", -1L)
        val tickerSymbol = intent.getStringExtra("tickerSymbol") ?: ""
        val tickerAmount = intent.getDoubleExtra("tickerAmount", 0.0)
        val usdAmount = intent.getDoubleExtra("usdAmount", 0.0)
        val timestamp = intent.getLongExtra("timestamp", 0L)
        val status = intent.getStringExtra("status") ?: "출금 완료"
        val txHash = intent.getStringExtra("txHash")
        val recipientAddress = intent.getStringExtra("recipientAddress")

        setupUI(tickerSymbol, tickerAmount, usdAmount, timestamp, status, txHash, recipientAddress)
        setupListeners()
    }

    private fun setupUI(tickerSymbol: String, tickerAmount: Double, usdAmount: Double, 
                       timestamp: Long, status: String, txHash: String?, recipientAddress: String?) {
        // 타이틀 설정
        binding.tvTitle.text = tickerSymbol

        // 상태 및 금액 표시
        binding.tvStatus.text = status
        binding.tvStatus.setTextColor(getColor(R.color.withdrawal_blue))

        // 금액 표시
        binding.tvAmount.text = String.format("%,.8f %s", tickerAmount, tickerSymbol)
        binding.tvAmountUsd.text = String.format("$%,.2f USD", usdAmount)

        // 완료 시간
        val date = Date(timestamp * 1000)
        val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
        }
        binding.valueCompletionTime.text = dateFormat.format(date)

        // 유형 (출금)
        binding.valueType.text = "출금"

        // 네트워크 (티커에 따라 다름)
        binding.valueNetwork.text = when (tickerSymbol) {
            "BTC" -> "Bitcoin"
            "ETH" -> "Ethereum"
            "MATIC" -> "Polygon"
            else -> tickerSymbol
        }

        // 거래 ID
        binding.valueTxid.text = txHash ?: "처리 중"

        // 받는 주소 (출금 시에만 표시)
        if (!recipientAddress.isNullOrEmpty()) {
            binding.labelTo.visibility = View.VISIBLE
            binding.valueTo.visibility = View.VISIBLE
            binding.valueTo.text = recipientAddress
        }
    }

    private fun setupListeners() {
        // 뒤로가기
        binding.ivBack.setOnClickListener {
            finish()
        }

        // 확인 버튼
        binding.confirmButton.setOnClickListener {
            finish()
        }

        // TX ID 복사
        binding.btnCopyTxid.setOnClickListener {
            val txid = binding.valueTxid.text.toString()
            if (txid != "처리 중") {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("TX ID", txid)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "거래 ID가 복사되었습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
