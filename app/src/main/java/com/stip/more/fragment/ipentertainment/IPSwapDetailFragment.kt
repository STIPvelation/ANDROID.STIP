package com.stip.stip.more.fragment.ipentertainment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.stip.stip.MainViewModel
import com.stip.stip.R
import com.stip.stip.databinding.FragmentMoreIpSwapDetailBinding
import com.stip.stip.more.fragment.ipentertainment.model.SwapModel

class IPSwapDetailFragment : Fragment() {
    private var _binding: FragmentMoreIpSwapDetailBinding? = null
    private val binding get() = _binding!!
    private val activityViewModel: MainViewModel by activityViewModels()
    
    // 네비게이션 args를 통해 SwapModel 데이터 받기 - 네비게이션 컴포넌트 사용 시
    private var swapItem: SwapModel? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreIpSwapDetailBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 이전 화면에서 전달받은 SwapModel 데이터 가져오기
        try {
            // Navigation Component 사용 시 args 값 가져오기 시도
            val args = IPSwapDetailFragmentArgs.fromBundle(requireArguments())
            swapItem = args.swapItem
        } catch (e: Exception) {
            // 오류 발생 시 Bundle에서 직접 가져오기 - 최신 API 사용
            swapItem = arguments?.getParcelable("swapItem", SwapModel::class.java)
        }
        
        // null이 아니면 화면 설정
        swapItem?.let {
            setupViews(it)
            setupListeners(it)
        }
        
        setupBackNavigation()
    }
    
    private fun setupBackNavigation() {
        // 백 버튼 처리를 위한 콜백 등록
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }
    
    private fun navigateBack() {
        // 여러 방법으로 뒤로가기 시도
        try {
            // 1. Navigation Component 사용 시
            if (findNavController().navigateUp()) {
                return
            }
        } catch (e: Exception) {
            // Navigation 컴포넌트를 사용하지 않는 경우 무시
        }
        
        // 2. Activity의 supportFragmentManager를 사용
        val fragmentManager = activity?.supportFragmentManager
        if (fragmentManager != null && fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
            return
        }
        
        // 3. 부모 Fragment의 백스택 사용
        if (isAdded && parentFragmentManager.backStackEntryCount > 0) {
            parentFragmentManager.popBackStack()
            return
        }
        
        // 4. Activity finish (최후의 수단)
        activity?.let { act ->
            act.onBackPressedDispatcher.onBackPressed()
        }
    }
    
    private fun setupViews(swapItem: SwapModel) {
        with(binding) {
            // IP 번호와 타이틀 설정
            textIpNumber.text = "US-2357-4566" // 고정 IP 번호를 사용하여 iOS 화면과 동일하게 표시
            textIpTitle.text = "스타벅스 아프리카 대륙 독점권"
            
            // 기본 정보 필드 설정
            textIpNumberValue.text = "US-2357-4566"
            textIpRegistrationOffice.text = "미국 상표청"
            textRemainingPeriod.text = "20년"
            textContactPerson.text = "버라사 흔아 이름"
            textContactEmail.text = "legal@starbucks.com"
            
            // 스왑 정보 설정
            textIpDescription.text = "아프리카 대륙 전체에 대한 스타벅스 브랜드 독점권입니다."
        }
    }
    
    private fun setupListeners(swapItem: SwapModel) {
        // 스왑 신청 버튼
        binding.btnSwapRequest.setOnClickListener {
            // 스왑 신청 화면으로 이동 - 프래그먼트 트랜잭션 사용
            val applicationFragment = IPSwapApplicationFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("swapItem", swapItem)
                }
            }
            
            // 현재 프래그먼트가 표시되는 컨테이너의 ID 가져오기
            val containerId = (view?.parent as? ViewGroup)?.id ?: android.R.id.content
            
            // activity의 supportFragmentManager를 사용하여 일관된 백스택 관리
            // 이렇게 하면 메인 프래그먼트와 동일한 레벨의 백스택에 추가되어 뒤로가기가 올바르게 작동함
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(containerId, applicationFragment)
                ?.addToBackStack("swapApplication")
                ?.commit()
        }
        
        // 관심 IP로 등록 버튼
        binding.btnAddToInterest.setOnClickListener {
            Toast.makeText(requireContext(), "관심 IP로 등록되었습니다.", Toast.LENGTH_SHORT).show()
            // 실제로는 관심 IP 등록 API 호출
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Update header title and back button
        activityViewModel.updateHeaderTitle("스왑 상세")
        activityViewModel.updateNavigationIcon(R.drawable.ic_arrow_back)
        
        // Set up navigation listener for the header back button
        activityViewModel.updateNavigationClickListener {
            navigateBack()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
