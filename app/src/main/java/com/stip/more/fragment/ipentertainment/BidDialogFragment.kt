package com.stip.stip.more.fragment.ipentertainment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.stip.stip.R
import com.stip.stip.databinding.DialogIpBiddingBinding
import com.stip.stip.more.fragment.ipentertainment.data.AuctionModel
import com.stip.stip.more.fragment.ipentertainment.data.IpType
import java.text.DecimalFormat

class BidDialogFragment : DialogFragment() {
    private var _binding: DialogIpBiddingBinding? = null
    private val binding get() = _binding!!
    
    private var auction: AuctionModel? = null
    private var onBidSuccess: ((Long) -> Unit)? = null
    
    companion object {
        fun newInstance(auction: AuctionModel, onBidSuccess: (Long) -> Unit): BidDialogFragment {
            val fragment = BidDialogFragment()
            fragment.auction = auction
            fragment.onBidSuccess = onBidSuccess
            return fragment
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FullScreenDialog)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogIpBiddingBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupListeners()
    }
    
    private fun setupUI() {
        auction?.let { auction ->
            // IP 타입 태그 설정
            val tagBackgroundColor = when (auction.ipType) {
                IpType.PATENT -> R.color.ip_patent
                IpType.TRADEMARK -> R.color.ip_trademark
                IpType.COPYRIGHT -> R.color.ip_copyright
                IpType.DESIGN -> R.color.ip_design
                IpType.CHARACTER -> R.color.ip_character
                IpType.FRANCHISE -> R.color.ip_franchise
                IpType.MUSIC, IpType.MOVIE, IpType.OTHER -> R.color.ip_other
            }
            binding.ipTypeTag.setBackgroundColor(resources.getColor(tagBackgroundColor, null))
            binding.ipTypeTag.text = auction.ipType.displayName
            
            // 등록 번호 설정
            binding.registrationNumberText.text = "번호: ${auction.registrationNumber}"
            
            // 현재 가격 설정 (달러 표시로 변환)
            binding.currentPrice.text = formatPriceInDollars(auction.currentPrice)
            
            // 최소 입찰 금액 표시 (현재 가격 + 1)
            val minBidAmount = auction.currentPrice + 1
            binding.minBidAmountText.text = "Minimum bid: ${formatPriceInDollars(minBidAmount)}"
            
            // IP 권리 안내문 설정
            binding.ipRightsNotice.text = 
                "낙찰 후 ${auction.ipType.displayName} 권리 이전과 관련하여 별도의 계약서 작성 및 등록이 필요하며, 관련 비용은 낙찰자 부담입니다."
            
            // 버튼 색상 설정
            binding.submitBidButton.backgroundTintList = 
                resources.getColorStateList(tagBackgroundColor, null)
        }
    }
    
    private fun setupListeners() {
        // 닫기 버튼
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        
        // 입력 금액 천 단위 구분자 포맷팅
        binding.bidAmountInput.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            private val formatter = DecimalFormat("#,###")
            
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                
                isFormatting = true
                
                val inputText = s.toString().replace(",", "")
                if (inputText.isNotEmpty()) {
                    try {
                        val number = inputText.toLong()
                        s.replace(0, s.length, formatter.format(number))
                    } catch (e: Exception) {
                        // 숫자 변환 실패 시 입력값 유지
                    }
                }
                
                isFormatting = false
            }
        })
        
        // 입찰하기 버튼
        binding.submitBidButton.setOnClickListener {
            validateAndSubmitBid()
        }
    }
    
    private fun validateAndSubmitBid() {
        val bidAmountText = binding.bidAmountInput.text.toString().replace(",", "")
        
        if (bidAmountText.isEmpty()) {
            Toast.makeText(requireContext(), "입찰 금액을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            // 사용자가 입력한 달러 금액을 long 값으로 변환
            val bidAmount = bidAmountText.toLong()
            auction?.let { auction ->
                // 달러 기준 최소 입찰금액 (현재 가격에서 1000으로 나누고 1 더함)
                val minBidAmountInDollars = auction.currentPrice / 1000 + 1
                
                if (bidAmount < minBidAmountInDollars) {
                    Toast.makeText(
                        requireContext(),
                        "최소 입찰금액 $${minBidAmountInDollars} 이상 입력해주세요.", 
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                
                // 제출된 달러 금액을 원래 스케일로 변환하여 전달 (1000 곱함)
                val scaledBidAmount = bidAmount * 1000
                
                // 입찰 성공 처리
                onBidSuccess?.invoke(scaledBidAmount)
                dismiss()
                
                // 입찰 성공 토스트 메시지
                Toast.makeText(requireContext(), "입찰에 성공했습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "올바른 금액을 입력해주세요.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun formatPriceInDollars(price: Long): String {
        // Format consistently with other price displays
        return String.format("$%,d", price)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
