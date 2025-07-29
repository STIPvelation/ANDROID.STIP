package com.stip.ipasset.ticker.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.stip.stip.R // R 리소스 사용 위해 필요
import com.stip.stip.databinding.ActivityTickerWithdrawDetailBinding
import com.stip.stip.ipasset.model.TransactionHistory // 실제 모델 경로 확인
import com.stip.stip.ipasset.TransactionDetailViewModel // !!! 실제 사용하는 ViewModel 클래스로 변경 !!!
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 입출금 완료 내역 상세 화면 Fragment
 * R.layout.deposit_withdraw_check 레이아웃 사용
 */
class WithdrawalDetailFragment : Fragment(R.layout.activity_ticker_withdraw_detail) {

    // ViewBinding
    private var _binding: ActivityTickerWithdrawDetailBinding? = null
    private val binding get() = _binding!!

    // Args 및 ViewModel
    private val args: WithdrawalDetailFragmentArgs by navArgs()
    private val viewModel: TransactionDetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = ActivityTickerWithdrawDetailBinding.bind(view)

        // UI 초기 설정
        setupToolbar()
        setupConfirmButton()
        observeTransactionDetails()

        // 데이터 가져오기
        Log.d("WithdrawalDetail", "Args => ID: ${args.transactionId}")
        viewModel.fetchTransactionDetailsById(args.transactionId)
    }

    private fun setupToolbar() {
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupConfirmButton() {
        binding.confirmButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun observeTransactionDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.transactionDetails.collect { transaction ->
                    transaction?.let {
                        Log.d("WithdrawalDetail", "Got transaction: $it")
                        updateUi(it)
                    }
                }
            }
        }
    }

    private fun updateUi(transaction: TransactionHistory) {
        // 로그 확인용
        Log.d("WithdrawalDetail", "amount=${transaction.amount}, ticker=${transaction.ticker}, currencyCode=${transaction.currencyCode}")
        val tickerName = if (transaction.ticker == null || transaction.ticker.equals("IP", true)) transaction.currencyCode else transaction.ticker
        binding.tvAmount.text = formatAmount(transaction.amount, transaction.currencyCode, tickerName)

        // USD 환산 금액 계산
        val amount = transaction.amount / 1_000_000.0
        val executionPrice = transaction.executionPrice
        val usdAmount = amount * executionPrice
        binding.tvAmountUsd.text = "${formatUsdAmount(usdAmount)} USD"

        // 헤더 텍스트 설정 (USD 또는 티커 심볼)
        val headerText = if (tickerName.equals("USD", true)) "USD" else tickerName ?: ""
        binding.tvTitle.text = headerText

        val (statusText, colorRes) = when (transaction.status) {
            TransactionHistory.Status.DEPOSIT_COMPLETED -> "입금 완료" to R.color.color_rise
            TransactionHistory.Status.WITHDRAWAL_COMPLETED -> "출금 완료" to R.color.color_fall
            TransactionHistory.Status.REFUND_COMPLETED -> "환불 완료" to R.color.color_rise
            TransactionHistory.Status.PROCESSING -> "처리중" to R.color.text_gray_808080_100
        }

        binding.tvStatus.text = statusText
        binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

        // 기타 UI 필드 설정
        binding.valueCompletionTime.text = formatTimestamp(transaction.timestamp)
        
        // 입금/출금 타입 표시
        binding.valueType.text = if (transaction.status == TransactionHistory.Status.DEPOSIT_COMPLETED) {
            "입금"
        } else {
            "출금"
        }

        // 네트워크 이름 설정
        binding.valueNetwork.text = "Polygon"
        
        // TXID 설정
        binding.valueTxid.text = transaction.txId ?: "-"

        // 출금 시에는 수신 주소 표시
        val isWithdrawal = transaction.status == TransactionHistory.Status.WITHDRAWAL_COMPLETED
        binding.labelTo.visibility = if (isWithdrawal) View.VISIBLE else View.GONE
        binding.valueTo.visibility = if (isWithdrawal) View.VISIBLE else View.GONE
        binding.valueTo.text = if (isWithdrawal) transaction.toAddress ?: "-" else ""
    }

    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
            }
            val date = Date(timestamp * 1000)
            sdf.format(date)
        } catch (e: Exception) {
            Log.e("WithdrawalDetail", "Timestamp format error", e)
            "N/A"
        }
    }

    private fun formatAmount(amount: Long, currencyCode: String, ticker: String?): String {
        return try {
            // ticker가 "IP"면 currencyCode 사용
            val symbol = if (currencyCode.equals("USD", true)) "USD" else if (ticker == null || ticker.equals("IP", true)) currencyCode else ticker
            val formattedAmount = String.format("%,.2f", amount / 1_000_000.0)
            "$formattedAmount $symbol"
        } catch (e: Exception) {
            "$amount ${ticker ?: currencyCode}"
        }
    }

    private fun formatUsdAmount(usdAmount: Double): String {
        return try {
            String.format("%,.2f", usdAmount)
        } catch (e: Exception) {
            usdAmount.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
