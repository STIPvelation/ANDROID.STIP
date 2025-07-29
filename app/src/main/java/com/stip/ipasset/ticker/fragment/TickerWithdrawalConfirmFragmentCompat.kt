package com.stip.ipasset.ticker.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.asLiveData
import com.stip.ipasset.fragment.BaseFragment
import com.stip.ipasset.model.IpAsset
import com.stip.stip.MainActivity
import com.stip.ipasset.ticker.viewmodel.WithdrawalConfirmViewModel
import com.stip.ipasset.WithdrawViewModel
import com.stip.stip.R
import com.stip.stip.databinding.FragmentIpAssetTickerWithdrawalConfirmBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.DecimalFormat

/**
 * 티커 출금 정보 확인 Fragment
 * Navigation Component에 의존하지 않는 버전
 */
@AndroidEntryPoint
class TickerWithdrawalConfirmFragmentCompat : BaseFragment<FragmentIpAssetTickerWithdrawalConfirmBinding>() {

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentIpAssetTickerWithdrawalConfirmBinding {
        return FragmentIpAssetTickerWithdrawalConfirmBinding.inflate(inflater, container, false)
    }
    
    // Bundle에서 직접 인자 가져오기
    private lateinit var ipAsset: IpAsset
    private var amount: Float = 0.0f
    private var fee: Float = 0.0f
    private var address: String = ""
    private val currencyCode: String get() = ipAsset.currencyCode
    
    // ViewModel 생성
    private val viewModel by viewModels<WithdrawalConfirmViewModel>()
    
    // 실제 API 연동을 위한 WithdrawViewModel 추가
    private val withdrawViewModel by viewModels<WithdrawViewModel>()
    
    override fun onResume() {
        super.onResume()
        
        // 티커 화면에서는 헤더 레이아웃 숨기기
        (activity as? MainActivity)?.setHeaderVisibility(false)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Bundle에서 인자 추출
        arguments?.let { args ->
            // API 레벨 33+ 호환성 수정
            (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                args.getParcelable(ARG_IP_ASSET, IpAsset::class.java)
            } else {
                @Suppress("DEPRECATION")
                args.getParcelable<IpAsset>(ARG_IP_ASSET)
            })?.let { asset ->
                ipAsset = asset
            }
            amount = args.getFloat(ARG_AMOUNT, 0.0f)
            fee = args.getFloat(ARG_FEE, 0.0f)
            address = args.getString(ARG_ADDRESS, "")
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind()
        observeViewModel()
    }
    
    /**
     * ViewModel 상태 관찰
     */
    private fun observeViewModel() {
        // 로딩 상태 관찰
        withdrawViewModel.isLoading.asLiveData().observe(viewLifecycleOwner) { isLoading ->
            updateLoadingState(isLoading)
        }
        
        // 성공 메시지 관찰
        withdrawViewModel.successMessage.asLiveData().observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrEmpty()) {
                onWithdrawalSuccess()
            }
        }
        
        // 에러 메시지 관찰
        withdrawViewModel.errorMessage.asLiveData().observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrEmpty()) {
                onWithdrawalError(message)
            }
        }
    }
    
    /**
     * 로딩 상태 업데이트
     */
    private fun updateLoadingState(isLoading: Boolean) {
        val binding = binding ?: return
        with(binding) {
            btnConfirmWithdrawal.isEnabled = !isLoading
            if (isLoading) {
                btnConfirmWithdrawal.text = "출금 처리 중..."
            } else {
                btnConfirmWithdrawal.text = "출금하기"
            }
        }
    }
    
    /**
     * 출금 성공 시 처리
     */
    private fun onWithdrawalSuccess() {
        Log.d("WithdrawalConfirmCompat", "출금 성공")
        showTransactionDialog()
        withdrawViewModel.clearMessages()
    }
    
    /**
     * 출금 실패 시 처리
     */
    private fun onWithdrawalError(errorMessage: String?) {
        val safeErrorMessage = errorMessage ?: "알 수 없는 오류가 발생했습니다"
        Log.e("WithdrawalConfirmCompat", "출금 실패: $safeErrorMessage")
        Toast.makeText(requireContext(), "출금 실패: $safeErrorMessage", Toast.LENGTH_LONG).show()
        
        // 에러 메시지 초기화
        withdrawViewModel.clearMessages()
    }
    
    /**
     * 실제 API 출금 처리
     */
    private fun performWithdrawal() {
        Log.d("WithdrawalConfirmCompat", "출금 API 호출 시작")
        logDebugInfo()
        
        lifecycleScope.launch {
            try {
                // 티커로부터 marketPairId 동적으로 가져오기
                val marketPairId = getMarketPairIdForTicker(currencyCode)
                if (marketPairId != null) {
                    withdrawViewModel.withdrawCrypto(
                        marketPairId = marketPairId,
                        amount = amount.toDouble(),
                        address = address
                    )
                } else {
                    Log.e("WithdrawalConfirmCompat", "marketPairId를 찾을 수 없음: $currencyCode")
                    Toast.makeText(requireContext(), "티커 정보를 찾을 수 없습니다: $currencyCode", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("WithdrawalConfirmCompat", "출금 요청 중 예외 발생", e)
                Toast.makeText(requireContext(), "출금 요청 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
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
            Log.e("WithdrawalConfirmCompat", "marketPairId 조회 실패: ${e.message}", e)
            null
        }
    }
    
    /**
     * 디버깅 정보 로그
     */
    private fun logDebugInfo() {
        Log.d("WithdrawalConfirmCompat", "출금 요청 정보:")
        Log.d("WithdrawalConfirmCompat", "  - 티커: $currencyCode")
        Log.d("WithdrawalConfirmCompat", "  - 금액: $amount")
        Log.d("WithdrawalConfirmCompat", "  - 주소: $address")
        Log.d("WithdrawalConfirmCompat", "  - 수수료: $fee")
        Log.d("WithdrawalConfirmCompat", "  - 출금수량: $amount, 수수료: $fee, 총 차감액: ${amount + fee}")
    }
    
    private fun bind() {
        val binding = binding ?: return
        with(binding) {
            // 툴바 설정
            materialToolbar.title = "$currencyCode ${getString(R.string.common_withdrawal_action)}"
            materialToolbar.setNavigationOnClickListener {
                // NavController 대신 FragmentManager 사용
                parentFragmentManager.popBackStack()
            }
            
            // 출금 상세 정보 표시
            val decimalFormat = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }
            
            // XML 레이아웃의 ID와 일치하도록 변경
            tvAmountValue.text = "${decimalFormat.format(amount.toDouble())} $currencyCode"
            tvFeeValue.text = "${decimalFormat.format(fee.toDouble())} $currencyCode"
            tvAddressValue.text = address
            tvTickerValue.text = currencyCode
            
            // 출금 주소 복사 기능 추가
            tvAddressValue.setOnClickListener {
                copyAddressToClipboard(address)
            }
            
            // 총 출금액 (출금액 + 수수료)
            val total = amount + fee
            tvTotalAmountValue.text = "${decimalFormat.format(total.toDouble())} $currencyCode"
            
            // 출금 신청 버튼 (XML의 btnConfirmWithdrawal와 일치)
            btnConfirmWithdrawal.setOnClickListener {
                performWithdrawal()
            }
        }
    }
    
    private fun processWithdrawal() {
        // 기존 더미 처리 로직 - 이제 사용하지 않음
        showLoadingIndicator(true)
        
        // 출금 진행 로직 (실제로는 API 호출 등이 여기에 구현됨)
        // 여기서는 간단하게 2초 딜레이 후 성공으로 처리
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            // 자산 업데이트 처리 (출금이므로 음수로 전달)
            val isSuccess = com.stip.dummy.AssetDummyData.updateAssetAmount(
                code = currencyCode, 
                amount = -amount // 출금이므로 음수 처리
            )
            
            showLoadingIndicator(false)
            
            if (isSuccess) {
                // 출금 신청 완료 후 트랜잭션 다이얼로그 표시
                showTransactionDialog()
            } else {
                // 잔액 부족 등의 오류 처리
                Toast.makeText(requireContext(), "출금 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }, 2000)
    }
    
    /**
     * 주소를 클립보드에 복사하는 함수
     */
    private fun copyAddressToClipboard(address: String) {
        val clipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("출금 주소", address)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(requireContext(), "출금 주소가 복사되었습니다", Toast.LENGTH_SHORT).show()
    }

    private fun showLoadingIndicator(isLoading: Boolean) {
        val binding = binding ?: return
        with(binding) {
            // 프로그레스 바가 XML에 없어서 임시로 처리
            if (isLoading) {
                btnConfirmWithdrawal.isEnabled = false
                btnConfirmWithdrawal.text = "처리 중..."
            } else {
                btnConfirmWithdrawal.isEnabled = true
                btnConfirmWithdrawal.text = getString(R.string.common_withdrawal_action)
            }
        }
    }
    
    private fun navigateToWithdrawalDetail() {
        Toast.makeText(requireContext(), "출금 신청이 완료되었습니다.", Toast.LENGTH_SHORT).show()
        
        // 메인 화면으로 이동 (모든 백스택 제거)
        parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
    
    private fun showTransactionDialog() {
        // 출금 완료 트랜잭션 다이얼로그를 표시
        val dialog = TickerTransactionDialogFragment.newInstance(
            title = "출금 신청 완료",
            message = "$currencyCode ${amount.toString()} 출금이 정상적으로 신청되었습니다.",
            confirmButtonText = "확인",
            confirmListener = { navigateToWithdrawalDetail() }
        )
        dialog.show(parentFragmentManager, "transaction_dialog")
    }
    
    companion object {
        const val ARG_IP_ASSET = "arg_ip_asset"
        const val ARG_AMOUNT = "arg_amount"
        const val ARG_FEE = "arg_fee"
        const val ARG_ADDRESS = "arg_address"
        
        // 새 인스턴스 생성 헬퍼 메서드
        fun newInstance(
            ipAsset: IpAsset,
            amount: Float,
            fee: Float,
            address: String
        ): TickerWithdrawalConfirmFragmentCompat {
            return TickerWithdrawalConfirmFragmentCompat().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_IP_ASSET, ipAsset)
                    putFloat(ARG_AMOUNT, amount)
                    putFloat(ARG_FEE, fee)
                    putString(ARG_ADDRESS, address)
                }
            }
        }
    }
}
