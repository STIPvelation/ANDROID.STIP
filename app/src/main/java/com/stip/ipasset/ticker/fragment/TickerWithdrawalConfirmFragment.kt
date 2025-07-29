package com.stip.ipasset.ticker.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.stip.ipasset.ticker.repository.IpAssetRepository
import com.stip.stip.MainViewModel
import com.stip.stip.R
import com.stip.stip.databinding.FragmentIpAssetTickerWithdrawalConfirmBinding
import com.stip.ipasset.fragment.BaseFragment
import com.stip.ipasset.model.IpAsset
import dagger.hilt.android.AndroidEntryPoint
import com.stip.dummy.TransactionDummyData
import com.stip.dummy.TransactionDummyDataExtended
import com.stip.dummy.TickerWithdrawalTransaction
import com.stip.dummy.Transaction
import com.stip.dummy.TransactionType
import com.stip.dummy.TransactionStatus
import java.util.Date
import java.util.UUID

/**
 * 티커 출금 확인을 처리하는 Fragment
 * 출금 정보를 확인하고 최종 승인합니다.
 */
@AndroidEntryPoint
class TickerWithdrawalConfirmFragment : BaseFragment<FragmentIpAssetTickerWithdrawalConfirmBinding>() {
    private val args by navArgs<TickerWithdrawalConfirmFragmentArgs>()
    private val ipAsset: IpAsset get() = args.ipAsset
    private val currencyCode: String get() = ipAsset.currencyCode
    
    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentIpAssetTickerWithdrawalConfirmBinding {
        return FragmentIpAssetTickerWithdrawalConfirmBinding.inflate(inflater, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind()
    }

    private fun bind() {
        val binding = binding ?: return
        with(binding) {
            // 툴바 설정 - 티커 이름 포함 및 중앙 정렬
            materialToolbar.title = "$currencyCode 출금신청"
            materialToolbar.setTitleCentered(true)
            materialToolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }

            // 출금 정보 화면에 데이터 바인딩 - 소수점 2자리로 표시
            tvTickerValue.text = currencyCode
            tvAmountValue.text = String.format("%.2f", args.withdrawalAmount)
            tvFeeValue.text = String.format("%.2f", args.fee)
            tvAddressValue.text = args.address
            
            // 총 출금 수량 (출금액 + 수수료) - 소수점 2자리로 표시
            val totalAmount = args.withdrawalAmount + args.fee
            tvTotalAmountValue.text = String.format("%.2f", totalAmount)
            tvTotalAmountCurrency.text = " $currencyCode"
            
            // 주소 복사 리스너 (TextView에 포함된 drawableEnd 아이콘)
            tvAddressValue.setOnClickListener {
                val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Withdrawal Address", args.address)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "주소가 복사되었습니다", Toast.LENGTH_SHORT).show()
            }
            
            // 출금 확인 버튼 리스너 - 확인 다이얼로그 표시
            btnConfirmWithdrawal.setOnClickListener {
                // 확인 버튼 클릭 시 다이얼로그 표시
                showCompletionDialog()
            }
        }
    }

    private fun showCompletionDialog() {
        // 사용자 지정 레이아웃 생성
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_completion, null)
        
        // 출금액 및 수수료 가져오기
        val withdrawalAmount = args.withdrawalAmount
        val fee = args.fee
        
        // 총 출금액 계산 (출금액 + 수수료) - 이 금액이 사용자의 잔액에서 차감됨
        val totalAmount = withdrawalAmount + fee
        
        // 여기서 실제 데이터베이스/서버 호출을 통해 출금 처리 및 잔액 업데이트
        // 실제 구현에서는 ViewModel이나 Repository를 통해 처리하는 것이 좋음
        processWithdrawal(totalAmount)
        
        // 다이얼로그 생성 및 사용자 지정 레이아웃 설정
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
            
        // 확인 버튼 찾아서 리스너 설정
        val confirmButton = dialogView.findViewById<android.widget.Button>(R.id.btn_confirm)
        confirmButton.setOnClickListener {
            alertDialog.dismiss()
            
            // 메인 ViewModel을 통해 앱 전체 데이터(자산 및 거래 내역) 새로고침 트리거
            val mainViewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[com.stip.stip.MainViewModel::class.java]
            mainViewModel.refreshAppData()
            
            // 이전 화면으로 돌아가기
            findNavController().popBackStack()
        }
        
        // 다이얼로그 표시
        alertDialog.show()
    }
    
    /**
     * 티커 출금 처리 및 잔액 업데이트
     * 출금액과 수수료를 합한 총 금액이 사용자의 잔액에서 차감됨
     * 거래 내역에도 출금 기록 추가
     */
    private fun processWithdrawal(totalAmount: Float) {
        // 로컬 데이터 저장소를 가져와서 자산 데이터 업데이트
        val repository = IpAssetRepository.getInstance(requireContext())
        val currentAsset = repository.getAsset(args.ipAsset.id)
        if (currentAsset != null) {
            // 출금 금액만큼 자산 금액 차감 (수수료 포함)
            val newAmount = currentAsset.balance - totalAmount
            repository.updateAsset(currentAsset.id, newAmount)
            Log.d("TickerWithdrawalConfirm", "자산 업데이트: ${currentAsset.currencyCode}, 새 금액: $newAmount")
            
            // 거래내역에 출금 기록 추가 - 중앙화된 더미 데이터 사용
            val currentTimeMillis = System.currentTimeMillis()
            val currentTimestamp = currentTimeMillis / 1000
            
            // ISO 8601 형식의 timestamp 생성
            val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", java.util.Locale.getDefault())
            isoFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val timestampIso = isoFormat.format(java.util.Date(currentTimeMillis))
            
            val tickerWithdrawalTransaction =
                com.stip.ipasset.ticker.model.TickerWithdrawalTransaction(
                    id = currentTimeMillis,
                    tickerAmount = totalAmount.toDouble(),
                    tickerSymbol = currentAsset.currencyCode,
                    usdAmount = totalAmount.toDouble(),
                    timestamp = currentTimestamp,
                    timestampIso = timestampIso,
                    status = "출금 dhksfy",
                    txHash = "0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                    recipientAddress = args.address,
                    fee = args.fee.toDouble()
                )
            
            // 중앙화된 더미 데이터에 트랜잭션 추가
            TransactionDummyDataExtended.addTickerTransaction(tickerWithdrawalTransaction)
            
            // 앱 전체 데이터 갱신 요청
            val mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
            mainViewModel.refreshAppData() 
            
            Log.d("TickerWithdrawalConfirm", "출금 처리 완료: ${currentAsset.currencyCode} $totalAmount")
        } else {
            Toast.makeText(requireContext(), "자산 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            Log.e("TickerWithdrawal", "출금 실패: 자산 정보를 찾을 수 없음 (ID: ${args.ipAsset.id})")
        }
    }

}

