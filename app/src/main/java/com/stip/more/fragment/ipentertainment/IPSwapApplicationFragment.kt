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
import com.stip.stip.MainViewModel
import com.stip.stip.R
import com.stip.stip.databinding.FragmentMoreIpSwapApplicationBinding
import com.stip.stip.more.fragment.ipentertainment.model.SwapModel

class IPSwapApplicationFragment : Fragment() {
    private var _binding: FragmentMoreIpSwapApplicationBinding? = null
    private val binding get() = _binding!!
    private val activityViewModel: MainViewModel by activityViewModels()
    
    // SwapModel 데이터를 저장할 변수
    private var swapItem: SwapModel? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreIpSwapApplicationBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 이전 화면에서 전달받은 SwapModel 데이터 가져오기 - 최신 API 사용
        swapItem = arguments?.getParcelable("swapItem", SwapModel::class.java)
        
        setupViews(swapItem)
        setupListeners()
        setupBackNavigation()
    }
    
    private fun setupBackNavigation() {
        // 1. 백 버튼 처리를 위한 콜백 등록
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        
        // 2. 툴바의 백 버튼에 클릭 리스너 직접 설정
        binding.root.findViewById<View>(R.id.buttonBack)?.setOnClickListener {
            navigateBack()
        }
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
        
        // 2. Activity의 supportFragmentManager를 사용 - IPSwapDetailFragment에서 사용한 것과 동일
        val fragmentManager = activity?.supportFragmentManager
        if (fragmentManager != null && fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
            return
        }
        
        // 3. 부모 Fragment의 백스택 사용 (백업)
        if (isAdded && parentFragmentManager.backStackEntryCount > 0) {
            parentFragmentManager.popBackStack()
            return
        }
        
        // 4. Activity finish (최후의 수단)
        activity?.let { act ->
            act.onBackPressedDispatcher.onBackPressed()
        }
    }
    
    private fun setupViews(swapItem: SwapModel?) {
        // IP 번호 필드에 이전 화면에서 받은 정보 미리 채우기
        binding.etIpNumberInput.setText("US-2357-4566")
    }
    
    private fun setupListeners() {
        // 업로드 버튼
        binding.btnUpload.setOnClickListener {
            if (isFormValid()) {
                // 실제로는 여기서 API 호출을 통해 서버에 스왑 신청 정보를 전송할 것
                Toast.makeText(requireContext(), "스왑 신청이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(requireContext(), "모든 필드를 채워주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun isFormValid(): Boolean {
        // 필수 입력 필드 유효성 검사
        return binding.etIpCertificate.text?.isNotEmpty() == true &&
                binding.etContact.text?.isNotEmpty() == true &&
                binding.etIpNumberInput.text?.isNotEmpty() == true &&
                binding.etSwapReason.text?.isNotEmpty() == true &&
                binding.checkboxAgree.isChecked
    }
    
    override fun onResume() {
        super.onResume()
        // Update header title and back button
        activityViewModel.updateHeaderTitle("스왑 신청")
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
