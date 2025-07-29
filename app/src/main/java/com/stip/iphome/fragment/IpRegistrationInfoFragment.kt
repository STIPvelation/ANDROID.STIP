package com.stip.stip.iphome.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.stip.stip.R
import com.stip.stip.databinding.ItemRegistrationNumberBinding
import com.stip.stip.iphome.constants.IpDetailInfo
import com.stip.stip.iphome.constants.PatentRegistrationNumbers
import com.stip.stip.iphome.model.IpListingItem

/**
 * IP 등록 정보 표시를 위한 프래그먼트
 * 특허 등록번호, 사업자 등록번호, 주소, 연락처 등의 정보를 표시합니다.
 */
class IpRegistrationInfoFragment : Fragment() {
    private var _binding: ItemRegistrationNumberBinding? = null
    private val binding get() = _binding!!
    
    private var currentTicker: String? = null
    private var ipItem: IpListingItem? = null
    
    companion object {
        private const val TAG = "IpRegistrationInfoFragment"
        private const val ARG_TICKER = "arg_ticker"
        private const val ARG_IP_ITEM = "arg_ip_item"
        
        fun newInstance(ticker: String?, ipItem: IpListingItem?): IpRegistrationInfoFragment {
            val fragment = IpRegistrationInfoFragment()
            val args = Bundle()
            args.putString(ARG_TICKER, ticker)
            args.putParcelable(ARG_IP_ITEM, ipItem)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentTicker = it.getString(ARG_TICKER)
            ipItem = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(ARG_IP_ITEM, IpListingItem::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable(ARG_IP_ITEM)
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ItemRegistrationNumberBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }
    
    private fun setupUI() {
        Log.d(TAG, "Setting up UI with ticker: $currentTicker")
        
        // 특허 등록번호 표시
        val registrationNumber = PatentRegistrationNumbers.getRegistrationNumberForTicker(currentTicker)
        binding.tvValueRegistrationNumber.text = registrationNumber
        
        // 업종 표시
        val businessType = IpDetailInfo.getBusinessTypeForTicker(currentTicker)
        binding.tvValueBusinessType.text = businessType
        
        // 연락처 정보
        binding.tvValueContactEmail.text = getString(R.string.value_contact_email)
        
        // 주소 표시
        val address = IpDetailInfo.getAddressForTicker(currentTicker)
        binding.tvValueAddressDetail.text = address
        
        // SNS 관련 표시 (기존 SNS 영역 활용)
        setupSnsLinks()
    }
    
    private fun setupSnsLinks() {
        // 이 메서드는 기존의 소셜 미디어 버튼을 활용하여 우리가 필요한 링크를 표시합니다
        // 홈페이지 링크를 Twitter 자리에 표시
        val homepageUrl = IpDetailInfo.getHomepageForTicker(currentTicker)
        if (homepageUrl != IpDetailInfo.DEFAULT_VALUE) {
            binding.tvValueSnsTwitter.text = "홈페이지"
            binding.tvValueSnsInstagram.text = homepageUrl
            binding.tvValueSnsInstagram.setOnClickListener {
                openUrl(homepageUrl)
            }
        } else {
            binding.tvValueSnsTwitter.text = "홈페이지"
            binding.tvValueSnsInstagram.text = IpDetailInfo.DEFAULT_VALUE
        }
        
        // IP 링크를 Kakaotalk 자리에 표시
        val ipLinkUrl = IpDetailInfo.getIpLinkForTicker(currentTicker)
        if (ipLinkUrl != IpDetailInfo.DEFAULT_VALUE) {
            binding.tvValueSnsKakaotalk.text = "IP 링크"
            binding.tvValueSnsTelegram.text = ipLinkUrl
            binding.tvValueSnsTelegram.setOnClickListener {
                openUrl(ipLinkUrl)
            }
        } else {
            binding.tvValueSnsKakaotalk.text = "IP 링크"
            binding.tvValueSnsTelegram.text = IpDetailInfo.DEFAULT_VALUE
        }
        
        // 사업계획서 링크를 LinkedIn 자리에 표시
        val businessPlanUrl = IpDetailInfo.getBusinessPlanForTicker(currentTicker)
        if (businessPlanUrl != IpDetailInfo.DEFAULT_VALUE) {
            binding.tvValueSnsLinkedin.text = "사업계획"
            binding.tvValueSnsWechat.text = businessPlanUrl
            binding.tvValueSnsWechat.setOnClickListener {
                openUrl(businessPlanUrl)
            }
        } else {
            binding.tvValueSnsLinkedin.text = "사업계획"
            binding.tvValueSnsWechat.text = IpDetailInfo.DEFAULT_VALUE
        }
        
        // 법인명과 대표자를 다른 텍스트 영역에 표시할 수도 있습니다
        // 이 예시는 생략합니다
    }
    
    private fun openUrl(url: String) {
        try {
            Log.d(TAG, "Opening URL: $url")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL: $url", e)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
