package com.stip.stip.iphome.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.stip.stip.iphome.adapter.IpHomeInfoDetailAdapter
import com.stip.stip.databinding.FragmentIpHomeInfoDetailBinding
import com.stip.stip.iphome.model.IpListingItem
import com.stip.stip.iphome.constants.IpDetailInfo
import com.stip.stip.R

class IpHomeInfoDetailFragment : Fragment() {

    private var _binding: FragmentIpHomeInfoDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var ipHomeInfoDetailAdapter: IpHomeInfoDetailAdapter
    private var currentItem: IpListingItem? = null
    
    private val TAG = "IpHomeInfoDetailFrag"

    companion object {
        private const val ARG_ITEM = "ip_listing_item"

        fun newInstance(item: IpListingItem): IpHomeInfoDetailFragment {
            return IpHomeInfoDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ITEM, item)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentItem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_ITEM, IpListingItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_ITEM) as? IpListingItem
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIpHomeInfoDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentItem?.let { item ->
            // 티커 가져오기
            val ticker = item.ticker
            Log.d(TAG, "현재 티커: $ticker")
            
            // 법인명, 대표자 (IpDetailInfo 활용)
            val companyName = IpDetailInfo.getCompanyNameForTicker(ticker)
            val ceo = IpDetailInfo.getCEOForTicker(ticker)
            
            // 법인명이 IpDetailInfo에 있으면 사용, 아니면 IpListingItem 데이터 사용
            binding.tvValueCorporationName?.text = if(companyName != IpDetailInfo.DEFAULT_VALUE) {
                companyName
            } else {
                item.companyName
            }
            
            // 대표자명이 IpDetailInfo에 있으면 사용, 아니면 IpListingItem 데이터 사용
            binding.tvValueRepresentative?.text = if(ceo != IpDetailInfo.DEFAULT_VALUE) {
                ceo
            } else {
                item.representative ?: "-"
            }
            
            // 어댑터 설정
            ipHomeInfoDetailAdapter = IpHomeInfoDetailAdapter(listOf(item))
            binding.recyclerViewDetailInfo.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = ipHomeInfoDetailAdapter
            }

            // IpDetailInfo에서 각 링크 정보 가져오기
            val ipLink = IpDetailInfo.getIpLinkForTicker(ticker)
            val homepageLink = IpDetailInfo.getHomepageForTicker(ticker)
            val businessPlanLink = IpDetailInfo.getBusinessPlanForTicker(ticker)
            val videoLink = IpDetailInfo.getVideoForTicker(ticker)
            
            // 링크 클릭 리스너 설정 (IpDetailInfo 우선 사용, 없으면 기존 데이터 사용)
            binding.linkDigitalIp.setOnClickListener {
                val link = if(ipLink != IpDetailInfo.DEFAULT_VALUE) ipLink else item.digitalIpLink
                openExternalLink(link, getString(R.string.no_ip_link))
            }

            binding.linkHomepage.setOnClickListener {
                val link = if(homepageLink != IpDetailInfo.DEFAULT_VALUE) homepageLink else item.homepageLink
                openExternalLink(link, getString(R.string.no_homepage_link))
            }

            binding.linkBusinessPlan.setOnClickListener {
                val link = if(businessPlanLink != IpDetailInfo.DEFAULT_VALUE) businessPlanLink else item.businessPlanLink
                openExternalLink(link, getString(R.string.no_business_plan_link))
            }

            binding.linkRelatedVideo.setOnClickListener {
                // 관련영상은 getRelatedVideoForTicker 사용
                val relatedVideoLink = IpDetailInfo.getRelatedVideoForTicker(ticker)
                val link = if(relatedVideoLink != IpDetailInfo.DEFAULT_VALUE) relatedVideoLink else item.relatedVideoLink
                Log.d(TAG, "관련영상 링크 클릭: $ticker, URL: $link")
                openExternalLink(link, getString(R.string.no_related_video_link))
            }

        } ?: run {
            Toast.makeText(context, getString(R.string.error_loading_details), Toast.LENGTH_SHORT).show()
        }
    }

    private fun openExternalLink(url: String?, errorMessage: String) {
        if (!url.isNullOrBlank() && url != IpDetailInfo.DEFAULT_VALUE) {
            try {
                var processedUrl = url.trim()
                
                // URL이 http:// 또는 https://로 시작하지 않으면 https:// 추가
                if (!processedUrl.startsWith("http://") && !processedUrl.startsWith("https://")) {
                    processedUrl = "https://$processedUrl"
                }
                
                Log.d(TAG, "링크 열기 시도: $processedUrl")
                
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(processedUrl))
                
                // 브라우저 선택 옵션 추가 (여러 브라우저가 있을 경우)
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                
                // 처리 가능한 앱이 있는지 확인
                val packageManager = requireContext().packageManager
                val activities = packageManager.queryIntentActivities(intent, 0)
                
                if (activities.isNotEmpty()) {
                    // 브라우저 선택 다이얼로그 표시
                    val chooser = Intent.createChooser(intent, "브라우저 선택")
                    startActivity(chooser)
                    Log.d(TAG, "링크 열기 성공: $processedUrl")
                } else {
                    // 처리 가능한 앱이 없으면 직접 시도
                    startActivity(intent)
                    Log.d(TAG, "직접 링크 열기: $processedUrl")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "링크 열기 실패: $url", e)
                
                // 실패 시 대체 방법 제안
                val message = "링크를 열 수 없습니다. \n링크: $url\n오류: ${e.message}\n\n브라우저에서 직접 접속해주세요."
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.recyclerViewDetailInfo?.adapter = null
        _binding = null
    }
}