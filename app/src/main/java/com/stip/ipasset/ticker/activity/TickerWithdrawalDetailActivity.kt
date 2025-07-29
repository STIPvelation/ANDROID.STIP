package com.stip.ipasset.ticker.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stip.api.repository.WalletHistoryRepository
import com.stip.stip.databinding.ActivityTickerWithdrawDetailBinding
import com.stip.stip.signup.utils.PreferenceUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TickerWithdrawalDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTickerWithdrawDetailBinding
    private lateinit var walletHistoryRepository: WalletHistoryRepository
    private var transactionApiId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTickerWithdrawDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        walletHistoryRepository = WalletHistoryRepository()
        transactionApiId = intent.getStringExtra("transactionApiId")

        setupClickListeners()
        displayTransactionDetails()

        loadLatestTransactionStatus()
    }

    private fun setupClickListeners() {
        // 뒤로 가기 버튼
        binding.ivBack.setOnClickListener {
            finish()
        }

        // 공유하기 버튼 - 레이아웃에 아직 추가되지 않음
        // 임시로 주석 처리
        /* 향후 ivShare가 추가되면 활성화
        binding.ivShare.setOnClickListener {
            Toast.makeText(this, "공유 기능은 준비중입니다.", Toast.LENGTH_SHORT).show()
        }
        */

        // 트랜잭션 ID 복사
        binding.btnCopyTxid.setOnClickListener {
            val txid = binding.valueTxid.text.toString()
            copyToClipboard("Transaction ID", txid)
            Toast.makeText(this, "거래 ID가 복사되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 수신자 주소 복사 버튼은 레이아웃에 없음
        // valueTo를 사용하여 주소 표시
        // 임시로 주석 처리
        /*
        binding.btnCopyTo.setOnClickListener {
            val address = binding.valueTo.text.toString()
            copyToClipboard("Recipient Address", address)
            Toast.makeText(this, "수신자 주소가 복사되었습니다.", Toast.LENGTH_SHORT).show()
        }
        */

        // 확인 버튼
        binding.confirmButton.setOnClickListener {
            finish()
        }
    }

    private fun displayTransactionDetails() {
        // Intent에서 데이터 가져오기
        val tickerSymbol = intent.getStringExtra("tickerSymbol") ?: ""
        val tickerAmount = intent.getDoubleExtra("tickerAmount", 0.0)
        val usdAmount = intent.getDoubleExtra("usdAmount", 0.0)
        val timestamp = intent.getLongExtra("timestamp", 0L)
        val status = intent.getStringExtra("status") ?: ""
        val txHash = intent.getStringExtra("txHash") ?: ""
        val recipientAddress = intent.getStringExtra("recipientAddress") ?: ""
        val fee = intent.getDoubleExtra("fee", 0.0)

        // 화면 타이틀 설정
        binding.tvTitle.text = "출금 내역 상세"

        // 상태 메시지
        binding.tvStatus.text = status

        // 금액 정보
        binding.tvAmount.text = "${tickerAmount.toString()} ${tickerSymbol}"
        binding.tvAmountUsd.text = "≈ ${String.format("%.2f", usdAmount)} USD"

        // 수수료 - 해당 필드는 레이아웃에 없음
        // 임시로 주석 처리
        // binding.valueFee.text = "${fee} ${tickerSymbol}"

        // 시간 포맷팅
        val timestampIso = intent.getStringExtra("timestampIso")
        binding.valueCompletionTime.text = if (!timestampIso.isNullOrBlank()) {
            formatTimestamp(timestampIso)
        } else {
            val date = Date(timestamp * 1000)
            val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm ('UTC' +09:00)", Locale.getDefault())
            sdf.format(date)
        }

        // 유형
        binding.valueType.text = "출금"

        // 네트워크
        binding.valueNetwork.text = "Polygon"
        
        // 수신자 주소
        binding.valueTo.text = recipientAddress
        
        // 트랜잭션 ID
        binding.valueTxid.text = txHash
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clipData)
    }
    
    /**
     * API에서 최신 트랜잭션 상태 조회
     */
    private fun loadLatestTransactionStatus() {
        if (transactionApiId.isNullOrBlank()) {
            Log.w("TickerWithdrawalDetail", "트랜잭션 API ID가 없어서 상태 업데이트를 건너뜁니다.")
            return
        }
        
        lifecycleScope.launch {
            try {
                val token = PreferenceUtil.getToken()
                if (token.isNullOrBlank()) {
                    Log.w("TickerWithdrawalDetail", "토큰이 없어서 상태 업데이트를 건너뜁니다.")
                    return@launch
                }
                
                Log.d("TickerWithdrawalDetail", "트랜잭션 상태 조회 시작: $transactionApiId")
                
                // API에서 거래 내역 조회
                val history = walletHistoryRepository.getWalletHistory(
                    authorization = "Bearer $token"
                )
                
                // 해당 ID와 일치하는 트랜잭션 찾기
                val latestTransaction = history.find { it.id == transactionApiId }
                
                if (latestTransaction != null) {
                    Log.d("TickerWithdrawalDetail", "최신 트랜잭션 상태: ${latestTransaction.status}")
                    
                    // 상태 매핑 및 UI 업데이트
                    val updatedStatus = when (latestTransaction.status) {
                        "REQUEST" -> "출금 진행중"
                        "APPROVED" -> "출금 완료"
                        "REJECTED" -> "출금 반려"
                        else -> "출금 완료"
                    }
                    
                    // UI 업데이트
                    binding.tvStatus.text = updatedStatus
                    
                    Log.d("TickerWithdrawalDetail", "상태 업데이트 완료: $updatedStatus")
                } else {
                    Log.w("TickerWithdrawalDetail", "해당 ID의 트랜잭션을 찾을 수 없음: $transactionApiId")
                }
                
            } catch (e: Exception) {
                Log.e("TickerWithdrawalDetail", "상태 업데이트 중 오류 발생", e)
                // 오류 발생 시에는 기존 상태 유지
            }
        }
    }

    /**
     * ISO 형식 타임스탬프를 포맷팅하는 함수
     */
    private fun formatTimestamp(timestampIso: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val outputFormat = SimpleDateFormat("yyyy.MM.dd HH:mm ('UTC' +09:00)", Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
            }
            val date = inputFormat.parse(timestampIso)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            Log.e("TickerWithdrawalDetail", "Timestamp formatting error: $timestampIso", e)
            // fallback: 직접 파싱 시도
            try {
                val year = timestampIso.substring(0, 4)
                val month = timestampIso.substring(5, 7)
                val day = timestampIso.substring(8, 10)
                val hour = timestampIso.substring(11, 13)
                val minute = timestampIso.substring(14, 16)
                "$year.$month.$day $hour:$minute (UTC +09:00)"
            } catch (e2: Exception) {
                "날짜 오류"
            }
        }
    }
}
