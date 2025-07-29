package com.stip.stip.more.fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.stip.stip.MainViewModel
import com.stip.stip.R
import com.stip.stip.databinding.FragmentMoreBinding
import com.stip.stip.more.*
import com.stip.stip.more.fragment.ipentertainment.IPAuctionFragment
import com.stip.stip.more.fragment.ipentertainment.IPDonationFragment
import com.stip.stip.more.fragment.ipentertainment.IPSwapFragment
import com.stip.stip.more.fragment.ipentertainment.IPToonFragment

class MoreFragment : Fragment() {

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    private val activityViewModel: MainViewModel by activityViewModels()
    private val TAG = "MoreFragment_Debug"

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            saveProfileImageUri(it)
            loadProfileWithGlide(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView called")
        _binding = FragmentMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")
        setupClickListeners()
        loadSavedProfileImage()
        setAppVersionInfo()
        
        // 회원정보 관찰 및 표시
        activityViewModel.memberInfo.observe(viewLifecycleOwner) { info ->
            if (info != null) {
                // 회원 이름 표시
                binding.textViewName.text = info.name

                // 회원 ID(9자리 공통번호) 표시
                if (info.name.isNotBlank()) {
                    binding.textViewId.text = info.name
                    binding.textViewId.visibility = View.VISIBLE
                    Log.d(TAG, "회원 ID 표시: ${info.id}")
                } else {
                    // 회원 ID가 없는 경우 UI에서 숨김 처리
                    binding.textViewId.visibility = View.GONE
                    Log.d(TAG, "회원 ID 없음 - 화면에서 숨김")
                }
            } else {
                // 비로그인 상태일 때
                binding.textViewName.text = "로그인이 필요합니다"
                binding.textViewId.visibility = View.GONE
                Log.d(TAG, "비로그인 상태 - 회원 ID 숨김")
            }
        }
        
        // API를 통해 회원정보 로드
        refreshMemberInfo()

        // 프로필 이미지 클릭 이벤트
        binding.profileImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }
    override fun onResume() {
        super.onResume()
        activityViewModel.updateHeaderTitle(getString(R.string.header_more))
        activityViewModel.updateNavigationIcon(0)
        Log.d(TAG, "onResume: Header title set to 'IP웹툰'")
        
        // 화면으로 돌아올 때마다 회원정보 새로고침
        refreshMemberInfo()
    }

    private fun setupClickListeners() {
        binding.profileSection.setOnClickListener {
            navigateTo(MoreMemberInfoFragment(), "회원 정보")
        }
        // 빠른 메뉴 버튼 클릭 리스너
        binding.textViewPriceAlert.setOnClickListener {
            navigateTo(MorePriceAlertFragment(), "시세 알림")
        }
        binding.textViewModeSettings.setOnClickListener {
            navigateTo(MoreModeSettingFragment(), "모드 설정")
        }
        binding.textViewSecurityAuth.setOnClickListener {
            navigateTo(MoreSecurityFragment(), "보안 / 인증")
        }
        
        // IP 엔터테인먼트 카드 클릭 리스너
        binding.cardIpToon.setOnClickListener {
            navigateTo(IPToonFragment(), "IP 툰")
        }
        binding.cardIpAuction.setOnClickListener {
            navigateTo(IPAuctionFragment(), "IP 옥션")
        }
        binding.cardIpSwap.setOnClickListener {
            navigateTo(IPSwapFragment(), "IP 스왑")
        }
        binding.cardIpDonation.setOnClickListener {
            navigateTo(IPDonationFragment(), "IP 기부")
        }
        
        // 정책 및 약관 카드 클릭 리스너
        // 메인 정책 카드
        binding.cardPolicy.setOnClickListener {
            // 정책 및 약관 화면으로 이동
            navigateTo(MorePolicyFragment(), "정책 및 약관")
        }
        
        // 정책 카드에 클릭 리스너 설정
        setupPolicyCardClickListeners()
        
        binding.cardStipvelation.setOnClickListener {
            // STIPvelation 외부 링크로 이동
            Toast.makeText(requireContext(), "STIPvelation 사이트로 이동합니다", Toast.LENGTH_SHORT).show()
            
            try {
                val stipvelationUrl = "https://stipvelation.com/"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(stipvelationUrl))
                startActivity(intent)
                Log.d(TAG, "STIPvelation 웹사이트로 이동: $stipvelationUrl")
            } catch (e: Exception) {
                Log.e(TAG, "STIPvelation 웹사이트 이동 실패", e)
                Toast.makeText(requireContext(), "웹사이트 이동 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        
        // 고객센터 버튼 클릭 리스너
        binding.cardCustomerCenter.setOnClickListener {
            navigateTo(MoreCustomerCenterFragment(), "고객센터")
        }
    }

    private fun navigateTo(fragment: Fragment, logName: String) {
        Log.d(TAG, "$logName 클릭됨! 화면 이동 시작")
        try {
            // 안전성 검사 추가
            if (!isAdded || requireActivity().isFinishing || requireActivity().isDestroyed) {
                Log.e(TAG, "$logName 화면 이동 실패: Fragment not attached or activity finishing")
                return
            }
            
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true) // 프래그먼트 트랜잭션 처리 개선
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
                
            Log.d(TAG, "$logName replace 완료")
        } catch (e: Exception) {
            Log.e(TAG, "$logName 화면 이동 실패!", e)
            Toast.makeText(context, "화면 전환 오류: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * 특정 정책 제목으로 MorePolicyFragment로 이동하는 함수
     */
    /**
     * 모든 정책 카드에 클릭 리스너 설정하는 함수
     */
    private fun setupPolicyCardClickListeners() {
        // 각 정책 카드에 대한 클릭 리스너 정의
        val policyCards = mapOf(
            binding.cardEsgPolicy to "ESG 정책",
            binding.cardStartupPolicy to "스타트업 및 중소기업 지원 정책",
            binding.cardAmlPolicy to "자금세탁방지(AML) 및 내부통제 정책",
            binding.cardDipPolicy to "DIP 가치평가 기준 정책",
            binding.cardFeePolicy to "수수료 투명성 정책"
        )
        
        // 각 카드에 클릭 리스너 설정
        policyCards.forEach { (card, policyTitle) ->
            card.setOnClickListener {
                Log.d(TAG, "$policyTitle 클릭되었습니다")
                navigateToMorePolicyWithTitle(policyTitle)
            }
        }
    }
    
    private fun navigateToMorePolicyWithTitle(policyTitle: String) {
        Log.d(TAG, "$policyTitle 정책으로 이동 시도")
        try {
            val morePolicyFragment = MorePolicyFragment()
            morePolicyFragment.arguments = Bundle().apply {
                putString("selected_policy_title", policyTitle)
            }
            
            parentFragmentManager.commit {
                replace(R.id.fragment_container, morePolicyFragment)
                addToBackStack(null)
            }
            Log.d(TAG, "$policyTitle 정책 화면으로 이동 완료")
        } catch (e: Exception) {
            Log.e(TAG, "$policyTitle 정책 화면 이동 실패!", e)
            Toast.makeText(context, "화면 전환 오류: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveProfileImageUri(uri: Uri) {
        val pref = requireContext().getSharedPreferences("profile", Context.MODE_PRIVATE)
        pref.edit().putString("profile_uri", uri.toString()).apply()
    }

    private fun loadSavedProfileImage() {
        val pref = requireContext().getSharedPreferences("profile", Context.MODE_PRIVATE)
        val uriString = pref.getString("profile_uri", null)
        uriString?.let {
            loadProfileWithGlide(Uri.parse(it))
        }
    }
    private fun loadProfileWithGlide(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .transform(CircleCrop())
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .into(binding.profileImage)
    }
    
    //새로고침
    private fun refreshMemberInfo() {
        // 로그인 상태 확인
        if (activityViewModel.isAuthenticated) {
            Log.d(TAG, "회원정보 API 조회")
            activityViewModel.loadMemberInfo()
        }
    }

    /**
     * 앱 버전 정보를 표시하는 함수
     */
    private fun setAppVersionInfo() {
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val versionName = packageInfo.versionName
            binding.textViewAppVersion.text = "앱 버전 $versionName"
        } catch (e: Exception) {
            Log.e(TAG, "앱 버전 정보를 가져오는데 실패했습니다: ${e.message}")
            binding.textViewAppVersion.text = "앱 버전 정보 없음"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called")
        _binding = null
    }
}
