package com.stip.stip.iphome.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.recyclerview.widget.LinearLayoutManager
import com.stip.stip.R
import com.stip.stip.iphome.adapter.LicenseScopeAdapter
import com.stip.stip.iphome.adapter.LicenseScopeItem
import com.stip.stip.iphome.adapter.UsagePlanAdapter // UsagePlanAdapter import 확인
import com.stip.stip.databinding.FragmentIpHomeInfoBinding
import com.stip.stip.iphome.model.IpListingItem // IpListingItem import 확인
import com.stip.stip.iphome.TradingDataHolder
// DialogFragment import 확인

// --- OnUsageItemClickListener 인터페이스 구현 제거 ---
class IpHomeInfoFragment : Fragment() { // <<< 인터페이스 구현 제거

    private var _binding: FragmentIpHomeInfoBinding? = null
    private val binding get() = _binding!!

    private var currentTicker: String? = null
    private var currentItem: IpListingItem? = null
    private var isDetailTabActive: Boolean = false // '상세정보' 탭 활성화 여부

    // --- 어댑터 변수 (필요한 경우 유지, setup 함수 내에서만 사용하면 제거 가능) ---
    // private lateinit var usagePlanAdapter: UsagePlanAdapter
    // private lateinit var licenseScopeAdapter: LicenseScopeAdapter

    companion object {
        private const val ARG_TICKER = "ticker"
        const val TAG = "IpHomeInfoFragment" // Fragment Log Tag

        fun newInstance(ticker: String?): IpHomeInfoFragment {
            val fragment = IpHomeInfoFragment()
            val args = Bundle().apply {
                putString(ARG_TICKER, ticker)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentTicker = it.getString(ARG_TICKER)
            Log.d(TAG, "onCreate: Received ticker = $currentTicker")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIpHomeInfoBinding.inflate(inflater, container, false)
        Log.d(TAG, "onCreateView: Binding inflated")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: View created for ticker = $currentTicker")
        updateUiForTicker(currentTicker) // UI 초기화 및 데이터 로드 (내부에서 Adapter 설정 호출)
        setupTabClickListeners()
        setupShortcutLinks()
        // setupLicenseScopeRecycler() // updateUiForTicker 에서 호출됨
        showInfoTab()
    }

    // Ticker에 해당하는 데이터로 UI 업데이트
    private fun updateUiForTicker(ticker: String?) {
        if (!isAdded || _binding == null) {
            Log.w(TAG, "updateUiForTicker: Fragment not added or binding is null. Ticker: $ticker")
            return
        }
        Log.d(TAG, "updateUiForTicker: Updating UI for ticker = $ticker")
        
        // 먼저 currentItem을 찾아서 설정
        currentItem = TradingDataHolder.ipListingItems.firstOrNull { it.ticker == ticker }
        
        // 회사이름만 표시 - 티커/USD 형식 삭제
        currentItem?.let { item ->
            val displayText = if (item.name.isNotBlank()) {
                item.name
            } else {
                item.ticker
            }
            binding.tvTickerName.text = displayText
        } ?: run {
            binding.tvTickerName.text = ticker ?: "N/A"
        }

        // 티커 로고 설정 (이니셜과 색상) - 현재 ticker 파라미터 사용
        ticker?.let { code ->
            // 티커 이니셜 설정 (첫 두 글자 사용)
            val tickerInitials = code.take(2)
            binding.currencyIconText.text = tickerInitials
            
            // TokenLogos 유틸리티를 사용하여 티커별 색상 설정
            val colorResId = com.stip.iphome.constants.TokenLogos.getColorForTicker(code)
            binding.currencyIconBackground.backgroundTintList = context?.getColorStateList(colorResId)
            
            // TokenIssuanceData 클래스에서 첫 발행일 가져오기
            val firstIssuanceDate = com.stip.stip.iphome.constants.TokenIssuanceData.getFirstIssuanceDateForTicker(code, "정보 없음")
            binding.tvFirstIssuanceDate.text = firstIssuanceDate
            
            // TokenIssuanceData 클래스에서 총 발행 한도 가져오기
            val totalIssuanceLimit = com.stip.stip.iphome.constants.TokenIssuanceData.getTotalIssuanceLimit()
            binding.tvTotalIssuanceLimit.text = totalIssuanceLimit
            
            // PatentRegistrationNumbers 클래스에서 모든 특허 등록번호 가져오기 - 현재 ticker 파라미터 사용
            val allRegistrationNumbers = com.stip.stip.iphome.constants.PatentRegistrationNumbers.getAllRegistrationNumbersForTicker(code)
            Log.d(TAG, "설정된 등록번호 목록: $allRegistrationNumbers (티커: $code)")
            
            if (allRegistrationNumbers.isEmpty()) {
                // 등록번호가 없는 경우 숨김
                binding.registrationNumberBox.visibility = View.GONE
                Log.d(TAG, "등록번호 없음, 숨김 처리: $code")
            } else {
                // 여러 특허 번호를 콤마와 공백으로 구분하여 가로로 표시
                val displayText = allRegistrationNumbers.joinToString(", ")
                binding.registrationNumberBox.text = displayText
                binding.registrationNumberBox.visibility = View.VISIBLE
                Log.d(TAG, "특허 등록번호 설정: $code = $displayText")
            }
        }

        if (currentItem == null) {
            Log.w(TAG, "updateUiForTicker: No IpListingItem found for ticker = $ticker")
            // 데이터 없을 경우 UI 초기화
            binding.recyclerViewInfoDetails.adapter = null
            binding.recyclerViewLicenseScope.adapter = null
            // TODO: 사용자 알림 UI 처리 추가
            return
        }

        // 어댑터 설정 함수 호출
        setupUsagePlanRecycler(currentItem!!) // currentItem이 null 아님 보장
        setupLicenseScopeRecycler(currentItem!!)
    }

    // 바로가기 링크 설정
    private fun setupShortcutLinks() {
        // 블록 조회 링크
        binding.tvLinkBlockInquiry.setOnClickListener {
            val blockchainUrl = com.stip.stip.iphome.constants.BlockchainExplorerUrls.getUrlForTicker(currentTicker)
            Log.d(TAG, "Block Inquiry clicked for ticker: $currentTicker, URL: $blockchainUrl")
            openExternalUrl(blockchainUrl)
        }
        
        // IP 등급 링크
        binding.tvLinkIpRating.setOnClickListener {
            currentItem?.let { item ->
                Log.d(TAG, "IP Rating link clicked for ticker: ${item.ticker}")
                val dialogFragment = RadarChartDialogFragment.newInstance(
                    grade = item.patentGrade,
                    institutionalValues = item.institutionalValues,
                    stipValues = item.stipValues,
                    category = item.category
                )
                dialogFragment.show(childFragmentManager, RadarChartDialogFragment.TAG)
            } ?: run {
                Log.w(TAG, "setupShortcutLinks: currentItem is null, cannot show IP Rating.")
                Toast.makeText(requireContext(), "IP 등급 정보를 표시할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 실시권 링크 (License) - 올바른 함수 사용
        binding.tvLinkLicense.setOnClickListener {
            Log.d(TAG, "=== 실시권 링크 클릭 ===")
            Log.d(TAG, "Current ticker: $currentTicker")
            
            // 실시권 링크 가져오기
            val licenseUrl = com.stip.stip.iphome.constants.IpDetailInfo.getLicenseForTicker(currentTicker)
            Log.d(TAG, "License URL from IpDetailInfo: $licenseUrl")
            
            // 비교를 위해 사업계획 URL도 가져오기
            val businessPlanUrl = com.stip.stip.iphome.constants.IpDetailInfo.getBusinessPlanForTicker(currentTicker)
            Log.d(TAG, "Business Plan URL (for comparison): $businessPlanUrl")
            
            // URL이 사업계획과 다른지 확인
            if (licenseUrl == businessPlanUrl) {
                Log.w(TAG, "WARNING: License URL is same as Business Plan URL!")
            }
            
            if (licenseUrl != com.stip.stip.iphome.constants.IpDetailInfo.DEFAULT_VALUE) {
                Log.d(TAG, "Opening license URL: $licenseUrl")
                openExternalUrl(licenseUrl)
            } else {
                // 폴백: IpListingItem의 linkLicense 필드 사용
                val fallbackUrl = currentItem?.linkLicense ?: "https://stipvelation.com/license"
                Log.d(TAG, "Using fallback license URL: $fallbackUrl")
                openExternalUrl(fallbackUrl)
            }
            Log.d(TAG, "=== 실시권 링크 처리 완료 ===")
        }

        // 영상 보기 링크
        binding.tvLinkViewVideo.setOnClickListener {
            val videoUrl = com.stip.stip.iphome.constants.IpDetailInfo.getVideoForTicker(currentTicker)
            Log.d(TAG, "Video link clicked for ticker: $currentTicker, URL: $videoUrl")
            if (videoUrl != com.stip.stip.iphome.constants.IpDetailInfo.DEFAULT_VALUE) {
                openExternalUrl(videoUrl)
            } else {
                // 폴백: IpListingItem의 linkVideo 필드 사용
                val fallbackUrl = currentItem?.linkVideo ?: "https://stipvelation.com/video"
                Log.d(TAG, "Using fallback video URL: $fallbackUrl")
                openExternalUrl(fallbackUrl)
            }
        }
    }

    // 외부 URL 열기
    private fun openExternalUrl(url: String?) {
        if (url.isNullOrBlank()) {
            Log.w(TAG, "openExternalUrl: URL is null or blank.")
            Toast.makeText(requireContext(), "유효하지 않은 URL입니다.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            Log.d(TAG, "openExternalUrl: Opening URL: $url")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "openExternalUrl: Failed to open URL: $url", e)
            Toast.makeText(requireContext(), "URL을 여는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }



    // 탭 클릭 리스너 설정
    private fun setupTabClickListeners() {
        binding.textViewTabInfo.setOnClickListener { if (isDetailTabActive) showInfoTab() }
        binding.textViewTabDetails.setOnClickListener { if (!isDetailTabActive) showDetailsTab() }
    }

    // '기본정보' 탭 표시 로직
    private fun showInfoTab() {
        if (!isAdded) return
        Log.d(TAG, "showInfoTab: Showing Info Tab")
        isDetailTabActive = false
        updateTabVisuals(isInfoTab = true)

        // 상세정보 Fragment가 있다면 제거 (애니메이션 등 고려 시 다른 방식 사용 가능)
        childFragmentManager.findFragmentById(R.id.info_content_container)?.let { fragment ->
            if (fragment.isAdded) {
                Log.d(TAG, "showInfoTab: Removing existing detail fragment")
                childFragmentManager.commit { remove(fragment) }
            }
        } ?: Log.d(TAG, "showInfoTab: No fragment found to remove.")

        binding.infoContentContainer.visibility = View.GONE
        binding.basicContentGroup.visibility = View.VISIBLE // 기본 정보 그룹 ID 확인 필요
    }

    // '상세정보' 탭 표시 로직
    private fun showDetailsTab() {
        if (!isAdded) return
        Log.d(TAG, "showDetailsTab: Showing Details Tab")
        isDetailTabActive = true
        updateTabVisuals(isInfoTab = false)

        binding.basicContentGroup.visibility = View.GONE
        binding.infoContentContainer.visibility = View.VISIBLE

        currentItem?.let { item ->
            Log.d(
                TAG,
                "showDetailsTab: Replacing with IpHomeInfoDetailFragment for ticker=${item.ticker}"
            )
            replaceFragmentSafely(IpHomeInfoDetailFragment.newInstance(item))
        } ?: Log.w(TAG, "showDetailsTab: currentItem is null, cannot show details tab.")
    }

    // 탭 UI 업데이트
    private fun updateTabVisuals(isInfoTab: Boolean) {
        if (!isAdded || context == null) {
            Log.w(TAG, "updateTabVisuals: Cannot update visuals.")
            return
        }
        val activeColor = ContextCompat.getColor(requireContext(), R.color.tab_text_active)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.tab_text_inactive)

        binding.textViewTabInfo.setTextColor(if (isInfoTab) activeColor else inactiveColor)
        binding.underlineActiveInfo.visibility = if (isInfoTab) View.VISIBLE else View.GONE
        binding.textViewTabDetails.setTextColor(if (!isInfoTab) activeColor else inactiveColor)
        binding.underlineActiveDetails.visibility = if (!isInfoTab) View.VISIBLE else View.GONE
    }

    // Fragment 안전하게 교체
    private fun replaceFragmentSafely(fragment: Fragment) {
        if (!isAdded || _binding == null || !isResumed || childFragmentManager.isStateSaved) {
            Log.w(TAG, "replaceFragmentSafely: Cannot perform fragment transaction.")
            return
        }
        Log.d(TAG, "replaceFragmentSafely: Replacing fragment in R.id.info_content_container")
        childFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.info_content_container, fragment) // 컨테이너 ID 확인 필요
        }
    }

    // 라이선스 범위 RecyclerView 설정
    private fun setupLicenseScopeRecycler(item: IpListingItem) {
        if (!isAdded || context == null) return

        // 현재 티커에 대한 LicenseScopeInfo 데이터 가져오기
        val ticker = item.ticker
        val licenseScopes = com.stip.stip.iphome.constants.LicenseScopeInfo.getLicenseScopesForTicker(ticker)
        
        val licenseScopeItems = listOf(
            LicenseScopeItem(
                title = getString(R.string.license_scope_title),
                licenseScopes = licenseScopes
            )
        )

        val adapter = LicenseScopeAdapter(licenseScopeItems)
        binding.recyclerViewLicenseScope.adapter = adapter
        if (binding.recyclerViewLicenseScope.layoutManager == null) {
            binding.recyclerViewLicenseScope.layoutManager = LinearLayoutManager(requireContext())
        }

        Log.d(TAG, "setupLicenseScopeRecycler: Adapter set for ticker $ticker with ${licenseScopes.size} license scopes")
        Log.d(TAG, "License scopes for $ticker: ${licenseScopes.map { "${it.percentage} - ${it.usageArea}" }}")
    }




    // 사용 계획 RecyclerView 설정
    private fun setupUsagePlanRecycler(item: IpListingItem) {
        if (!isAdded || context == null) {
            Log.w(TAG, "setupUsagePlanRecycler: Cannot setup.")
            return
        }

        Log.d(TAG, "setupUsagePlanRecycler: Setting up UsagePlanAdapter for ticker=${item.ticker}")

        val adapter = UsagePlanAdapter(
            item = item,
            onShowUsagePlanClick = {
                Log.d(TAG, "UsagePlanAdapter - onShowUsagePlanClick invoked!")
                UsagePlanDialogFragment.newInstance(
                    usagePlanData = item.usagePlanData,
                    ticker = item.ticker // ✅ 여기!
                ).show(childFragmentManager, UsagePlanDialogFragment.TAG)
            },
            onShowLicenseAgreementClick = {
                Log.d(TAG, "UsagePlanAdapter - onShowLicenseAgreementClick invoked!")
                LicenseAgreementDialogFragment.newInstance()
                    .show(childFragmentManager, LicenseAgreementDialogFragment.TAG)
            }
        )

        binding.recyclerViewInfoDetails.adapter = adapter

        if (binding.recyclerViewInfoDetails.layoutManager == null) {
            binding.recyclerViewInfoDetails.layoutManager = LinearLayoutManager(requireContext())
        }

        Log.d(TAG, "setupUsagePlanRecycler: Adapter set for recyclerViewInfoDetails")
    }


    // 외부(예: TradingFragment)에서 Ticker 변경 시 호출될 함수
    fun updateTicker(ticker: String?) {
        if (!isAdded || _binding == null) {
            Log.w(TAG, "updateTicker: Cannot update. New ticker: $ticker")
            return
        }
        Log.d(TAG, "updateTicker: Updating to new ticker = $ticker")
        currentTicker = ticker
        updateUiForTicker(ticker) // UI 및 데이터 업데이트 (내부에서 어댑터 설정 포함)

        // 탭 상태에 따라 컨텐츠 갱신
        if (isDetailTabActive) {
            showDetailsTab()
        } else {
            showInfoTab()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: Cleaning up binding for ticker = $currentTicker")
        _binding?.recyclerViewInfoDetails?.adapter = null // 안전하게 null 처리
        _binding?.recyclerViewLicenseScope?.adapter = null // 안전하게 null 처리
        _binding = null
    }
}