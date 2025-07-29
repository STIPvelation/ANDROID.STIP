package com.stip.ipasset.usd.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.stip.ipasset.usd.manager.USDAssetManager
import com.stip.stip.R
import com.stip.stip.databinding.FragmentIpAssetUsdWithdrawalConfirmBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class USDWithdrawalConfirmFragment : Fragment() {

    private var _binding: FragmentIpAssetUsdWithdrawalConfirmBinding? = null
    private val binding get() = _binding!!
    
    // USD 자산 매니저
    private val assetManager = USDAssetManager.getInstance()
    
    // 출금 금액 및 수수료
    private var withdrawalAmount: Double = 0.0
    private var fee: Double = 0.0
    
    // 계좌번호 정보
    private var accountInfo: String = ""
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIpAssetUsdWithdrawalConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 이전 화면에서 전달받은 데이터 가져오기
        arguments?.let { args ->
            withdrawalAmount = args.getDouble("withdrawal_amount", 0.0)
            fee = args.getDouble("fee", 0.0)
            accountInfo = args.getString("account_info", "")
        }
        
        // 툴바 제목 설정 및 뒤로가기 버튼 이벤트 처리
        val toolbar = view.findViewById<MaterialToolbar>(R.id.material_toolbar)
        toolbar.title = "출금신청 확인"
        toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        
        // 금액 정보 표시
        displayWithdrawalInfo()
        
        // 확인 버튼 이벤트 처리
        val confirmButton = view.findViewById<Button>(R.id.confirm_button)
        confirmButton.setOnClickListener {
            // 출금 요청 처리
            processWithdrawal()
        }
    }
    
    /**
     * 출금 정보 화면에 표시
     */
    private fun displayWithdrawalInfo() {
        val formatter = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }
        
        // 출금 계좌 표시
        val bankText = view?.findViewById<TextView>(R.id.text_bank)
        if (accountInfo.isNotEmpty()) {
            bankText?.text = accountInfo
        } else {
            bankText?.text = "계좌번호 정보를 불러오는 중..."
        }
        
        // 출금 금액 표시
        val withdrawalAmountText = view?.findViewById<TextView>(R.id.text_withdrawable)
        withdrawalAmountText?.text = "${formatter.format(withdrawalAmount)} USD"
        
        // 수수료와 총 출금액은 현재 레이아웃에 없으므로 표시하지 않음
        // 필요시 새로운 TextView를 추가하거나 이 부분은 주석 처리
        android.util.Log.d("USDWithdrawal", "Fee: ${formatter.format(fee)} USD")
        
        // 총 출금액
        val totalAmount = withdrawalAmount + fee
        android.util.Log.d("USDWithdrawal", "Total amount: ${formatter.format(totalAmount)}")
    }
    
    /**
     * 출금 요청 처리
     */
    private fun processWithdrawal() {
        // 출금 처리 - 실제로는 서버에 요청을 보내야 함
        // 여기서는 성공 응답이 온다고 가정
        
        try {
            // 애니메이션 트랜지션 설정
            val fragmentManager = requireActivity().supportFragmentManager
            
            // 출금완료 다이얼로그 표시
            showCompletionDialog()
        } catch (e: Exception) {
            // 오류 처리
            Toast.makeText(requireContext(), "출금 처리 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 출금신청 완료 다이얼로그를 표시합니다.
     */
    private fun showCompletionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.fragment_ip_asset_transaction_dialog, null)
        
        // 다이얼로그 내용 설정
        val title = dialogView.findViewById<TextView>(R.id.title)
        val message = dialogView.findViewById<TextView>(R.id.message)
        val confirmButton = dialogView.findViewById<Button>(R.id.confirm_button)
        
        title.text = "출금신청"
        message.text = "출금신청이 완료 되었습니다"
        
        // 다이얼로그 생성 및 표시
        val alertDialog = AlertDialog.Builder(requireContext(), R.style.YourCustomDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        // 확인 버튼 클릭 시 출금 상세 확인 다이얼로그 표시
        confirmButton.setOnClickListener {
            alertDialog.dismiss()
            showWithdrawalDetailConfirmDialog()
        }
        
        alertDialog.show()
    }
    
    /**
     * 출금 상세 확인 다이얼로그를 표시합니다.
     */
    private fun showWithdrawalDetailConfirmDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_ip_asset_usd_withdrawal_detail_confirm, null)
        
        // 다이얼로그 내용 설정
        val amountView = dialogView.findViewById<TextView>(R.id.amount)
        val withdrawalAmountValue = dialogView.findViewById<TextView>(R.id.withdrawal_amount_value)
        val currencyValue = dialogView.findViewById<TextView>(R.id.currency_value)
        val feeValue = dialogView.findViewById<TextView>(R.id.fee_value)
        val bankValue = dialogView.findViewById<TextView>(R.id.bank_value)
        val accountNumberValue = dialogView.findViewById<TextView>(R.id.account_number_value)
        val expectedTimeValue = dialogView.findViewById<TextView>(R.id.expected_time_value)
        
        val formatter = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }
        amountView.text = "$${formatter.format(withdrawalAmount)}"
        withdrawalAmountValue.text = "${formatter.format(withdrawalAmount)} USD"
        currencyValue.text = "USD"
        
        // 계좌번호 정보 표시
        if (accountInfo.isNotEmpty()) {
            bankValue.text = accountInfo // 은행명과 계좌번호
            accountNumberValue.text = "" // 예금주명은 표시하지 않음
        } else {
            bankValue.text = "로딩 중..."
            accountNumberValue.text = ""
        }
        
        // 예상 출금 시간
        expectedTimeValue.text = "즉시"
        
        // 다이얼로그 생성 및 표시
        val alertDialog = AlertDialog.Builder(requireContext(), R.style.YourCustomDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        // 다이얼로그 버튼을 찾아서 클릭 이벤트 설정 (레이아웃에 따라 ID 조정 필요)
        val confirmButton = dialogView.findViewById<Button>(R.id.confirm_button)
        confirmButton?.setOnClickListener {
            alertDialog.dismiss()
            
            // 실제 USD 출금 API 호출
            performUSDWithdrawal()
        }
        
        alertDialog.show()
    }
    
    /**
     * 실제 USD 출금 API 호출
     */
    private fun performUSDWithdrawal() {
        lifecycleScope.launch {
            try {
                // 로딩 표시
                // showLoading()
                
                // USD marketPairId 가져오기 - 포트폴리오 API에서 가져오기
                val memberId = com.stip.stip.signup.utils.PreferenceUtil.getUserId()
                if (memberId.isNullOrBlank()) {
                    Toast.makeText(requireContext(), "사용자 ID가 없습니다.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val portfolioRepository = com.stip.api.repository.PortfolioRepository()
                val portfolioResponse = portfolioRepository.getPortfolioResponse(memberId)
                val marketPairId = portfolioResponse?.wallets?.find { wallet -> wallet.symbol == "USD" }?.marketPairId
                
                if (marketPairId == null) {
                    Toast.makeText(requireContext(), "USD 마켓 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // 출금 API 호출
                val withdrawViewModel = com.stip.ipasset.WithdrawViewModel(
                    com.stip.ipasset.repository.WalletWithdrawRepositoryImpl(
                        com.stip.stip.api.RetrofitClient.createTapiService(com.stip.ipasset.api.WalletWithdrawService::class.java)
                    )
                )
                
                // 계좌번호 추출 (은행명과 계좌번호에서 계좌번호만 추출)
                val accountNumberForAPI = if (accountInfo.isNotEmpty()) {
                    // 은행명 제거하고 계좌번호만 추출
                    accountInfo.substringAfter(" ").trim()
                } else {
                    ""
                }
                
                withdrawViewModel.withdrawCrypto(marketPairId, withdrawalAmount, accountNumberForAPI)
                
                // 성공/실패 메시지 관찰
                withdrawViewModel.successMessage.collect { successMsg ->
                    if (successMsg != null) {
                        android.util.Log.d("USDWithdrawal", "출금 성공: $successMsg")
                        
                        // 출금 성공 후 처리
                        onWithdrawalSuccess()
                        
                        // 메시지 초기화
                        withdrawViewModel.clearMessages()
                    }
                }
                
            } catch (e: Exception) {
                if (e is CancellationException) {
                    // 화면 전환 등으로 인한 취소는 무시
                    return@launch
                }
                Toast.makeText(requireContext(), "출금 처리 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 출금 성공 후 처리
     */
    private fun onWithdrawalSuccess() {
        // 로컬 잔액 업데이트
        val success = assetManager.processWithdrawal(withdrawalAmount)
        if (success) {
            // 데이터 갱신
            assetManager.refreshData()
            
            // 모든 프래그먼트의 UI 갱신을 위해 추가 처리
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                assetManager.refreshData()
                android.util.Log.d("USDWithdrawalConfirm", "Forced additional data refresh for all fragments")
            }, 300)
            
            // 모든 프래그먼트를 종료하고 메인 자산 화면으로 돌아감
            val fragmentManager = requireActivity().supportFragmentManager
            
            // 헤더 레이아웃을 다시 표시
            val headerLayout = requireActivity().findViewById<View>(R.id.headerLayout)
            headerLayout?.visibility = View.VISIBLE
            
            // 백 스택 처리 - IpAssetFragment로 돌아갈 수 있도록 설정
            fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            
            // 트랜잭션 목록 화면으로 이동 (실제 API 데이터로 새로고침)
            val transaction = fragmentManager.beginTransaction()
            val transactionFragment = USDTransactionFragment()
            transaction.replace(R.id.fragment_container, transactionFragment)
            transaction.addToBackStack("USDTransactionFragment")
            transaction.commit()
            
            Toast.makeText(requireContext(), "출금이 성공적으로 처리되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "출금 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
