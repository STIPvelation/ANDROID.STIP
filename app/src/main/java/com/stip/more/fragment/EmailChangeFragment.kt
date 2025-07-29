package com.stip.stip.more.fragment

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.stip.stip.R
import com.stip.stip.databinding.FragmentMoreMemberEmailEditBinding
import com.stip.stip.more.viewmodel.MemberInfoEditViewModel
import kotlinx.coroutines.launch

/**
 * 모던한 디자인의 이메일 변경 프래그먼트
 * 이메일 입력 검증 및 변경 기능 제공
 */
class EmailChangeFragment : Fragment() {

    private var _binding: FragmentMoreMemberEmailEditBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MemberInfoEditViewModel by viewModels()

    private var currentEmail: String? = null
    private var onEmailChangeListener: ((String) -> Unit)? = null

    companion object {
        private const val ARG_CURRENT_EMAIL = "current_email"

        @JvmStatic
        fun newInstance(currentEmail: String) =
            EmailChangeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CURRENT_EMAIL, currentEmail)
                }
            }
    }

    /**
     * 이메일 변경 리스너 설정
     * 이메일 변경 성공 시 호출됨
     */
    fun setOnEmailChangeListener(listener: (String) -> Unit) {
        onEmailChangeListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentEmail = it.getString(ARG_CURRENT_EMAIL)
            Log.d("EmailChangeFragment", "Received current email: $currentEmail")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreMemberEmailEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentEmail = arguments?.getString(ARG_CURRENT_EMAIL)

        setupUI()
        setupListeners()
        setupObservers()
    }

    private fun setupUI() {
        // 툴바 뒤로가기 버튼 설정
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        // 현재 이메일 표시
        currentEmail?.let {
            binding.tvCurrentEmail.text = it
        } ?: run {
            binding.tvCurrentEmail.text = "등록된 이메일이 없습니다"
        }
        
        // 입력 필드 및 버튼 초기 설정
        binding.etEmail.hint = "새 이메일 주소를 입력하세요"
        binding.etEmail.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        binding.tvTitle.text = getString(R.string.prompt_enter_email)
        binding.tvInfo.visibility = View.VISIBLE
        binding.btnRequestCode.text = "이메일 변경"

        // 버튼 초기 상태 설정
        binding.btnVerifyCode.visibility = View.GONE
        updateButtonState(false)
    }

    /**
     * 리스너 설정
     * 이메일 입력 감지 및 버튼 이벤트 처리
     */
    private fun setupListeners() {
        // 이메일 입력 감지
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 오류 메시지가 표시 중이라면 감추기
                if (binding.errorContainer.visibility == View.VISIBLE) {
                    binding.errorContainer.visibility = View.GONE
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // 입력값이 있을 때만 버튼 활성화
                updateButtonState(s.toString().isNotBlank())
            }
        })

        // 이메일 변경 버튼 클릭 이벤트
        binding.btnRequestCode.setOnClickListener {
            if (!it.isEnabled) return@setOnClickListener

            val newEmail = binding.etEmail.text.toString().trim()

            // 기본 입력 값 검증
            if (newEmail == currentEmail) {
                showErrorMessage("현재 이메일과 동일합니다. 변경할 이메일을 입력하세요.")
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                showErrorMessage("유효한 이메일 주소를 입력해주세요")
                return@setOnClickListener
            }

            // 키보드 숨기기
            hideKeyboard()

            // 버튼 비활성화 및 로딩 표시
            binding.btnRequestCode.isEnabled = false
            binding.btnRequestCode.text = "변경 중..."

            // 이메일 변경 API 호출
            viewModel.updateEmail(newEmail)
        }
    }

    /**
     * 관찰자 설정
     * 이메일 변경 성공/실패 결과 처리
     */
    private fun setupObservers() {
        // 성공 메시지 관찰
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            // 성공 시 이메일 변경 콜백 호출 및 화면 종료
            binding.etEmail.text.toString().trim().let { newEmail ->
                lifecycleScope.launch {
                    // 성공 알림 표시
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    // 콜백 호출 및 화면 종료
                    onEmailChangeListener?.invoke(newEmail)
                    parentFragmentManager.popBackStack()
                }
            }
        }

        // 오류 메시지 관찰
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            // 오류 메시지 표시
            showErrorMessage(message)

            // 버튼 상태 복구
            binding.btnRequestCode.isEnabled = true
            binding.btnRequestCode.text = "이메일 변경"
        }
    }

    /**
     * 버튼 상태 업데이트
     */
    private fun updateButtonState(enabled: Boolean) {
        binding.btnRequestCode.isEnabled = enabled
        // 버튼 상태에 따라 시각적 처리 추가
        if (enabled) {
            binding.btnRequestCode.alpha = 1.0f
        } else {
            binding.btnRequestCode.alpha = 0.6f
        }
    }

    /**
     * 오류 메시지 표시
     */
    private fun showErrorMessage(message: String) {
        binding.tvCodeError.text = message
        binding.errorContainer.visibility = View.VISIBLE
    }

    /**
     * 키보드 숨기기
     */
    private fun hideKeyboard() {
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocus = requireActivity().currentFocus
        currentFocus?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    /**
     * 키보드 보여주기
     */
    private fun showKeyboard(view: View) {
        view.requestFocus()
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onResume() {
        super.onResume()
        // 화면이 포커스를 받으면 이메일 입력란에 포커스 설정
        binding.etEmail.requestFocus()
        showKeyboard(binding.etEmail)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}