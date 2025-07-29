package com.stip.ipasset.usd.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.stip.ipasset.usd.model.USDDepositTransaction
import com.stip.stip.R
import com.stip.stip.databinding.FragmentIpAssetUsdDepositTransactionDetailBinding
import com.stip.stip.MainActivity

/**
 * USD 입금 트랜잭션 상세 화면 Fragment
 */
class USDDepositTransactionDetailFragment : Fragment() {

    private var _binding: FragmentIpAssetUsdDepositTransactionDetailBinding? = null
    private val binding get() = _binding!!
    
    // Navigation arguments
    private val args by navArgs<USDDepositTransactionDetailFragmentArgs>()
    private lateinit var transaction: USDDepositTransaction

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIpAssetUsdDepositTransactionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Transaction ID를 사용하여 실제 트랜잭션 데이터를 가져오는 로직 구현
        // 지금은 Navigation args로 전달된 ID만 확인
        setupBackButton()
        setupConfirmButton()
        displayTransactionDetails()
    }
    
    override fun onResume() {
        super.onResume()
        // 헤더 숨기기
        (activity as? MainActivity)?.setHeaderVisibility(false)
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            // NavController 대신 일반적인 FragmentManager 사용
            requireActivity().supportFragmentManager.popBackStack()
        }
    }
    
    private fun setupConfirmButton() {
        binding.btnConfirm.setOnClickListener {
            // 확인 버튼을 누르면 현재 화면 닫기
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun displayTransactionDetails() {
        // 여기에 트랜잭션 상세 정보를 화면에 표시하는 로직 구현
        // 실제 구현 시에는 ViewModel에서 데이터를 가져와 바인딩
        // 지금은 Navigation args로 전달된 transactionId를 사용해 데이터를 가져온다고 가정
        val transactionId = args.transactionId
        
        // TODO: 트랜잭션 ID를 사용하여 실제 데이터 로드
        // 예시: viewModel.loadTransactionDetails(transactionId)
        
        // 임시 데이터 바인딩 (실제 구현 시 변경 필요)
        binding.tvTransactionType.text = "입금 완료"
        binding.tvAmount.text = "100.00 USD"
        binding.tvKrwAmount.text = "135,000 KRW"
        
        // 날짜와 시간 필드는 레이아웃을 확장하여 추가 필요
        // TODO: 날짜/시간 표시를 위한 TextView를 layout에 추가
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
