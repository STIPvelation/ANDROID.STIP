package com.stip.stip.signup.signup.auth

import android.text.Html
import android.util.Log
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.stip.stip.R
import com.stip.stip.signup.base.BaseFragment
import com.stip.stip.signup.customview.AgencyTermsBottomSheet
import com.stip.stip.signup.customview.TermsBottomSheet
import com.stip.stip.databinding.FragmentSignUpAuthBinding
import com.stip.stip.signup.model.SignUpAgreeData
import com.stip.stip.signup.signup.SignUpViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpAuthFragment: BaseFragment<FragmentSignUpAuthBinding, SignUpAuthViewModel>() {
    override val layoutResource: Int
        get() = R.layout.fragment_sign_up_auth


    override val viewModel: SignUpAuthViewModel by viewModels()
    private val activityViewModel: SignUpViewModel by activityViewModels()
    lateinit var signUpAgreeAdapter: SignUpAgreeAdapter

    override fun initStartView() {
        val result = String.format(binding.root.context.getString(R.string.sign_up_title))
        binding.tvSignUpTitle.text = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(result, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(result)
        }
        Log.d("TAG", "initStartView: $result")

        // 이용약관 리스트 처리
        val signUpAgreeTitle = resources.getStringArray(R.array.sign_up_agree_title_array)
        val signUpAgreeContent = resources.getStringArray(R.array.sign_up_agree_content_array)
        val signUpAgreeDataList = mutableListOf<SignUpAgreeData>()
        var contentIndex = 0
        signUpAgreeTitle.forEachIndexed { index, title ->
            if (index == 0) {
                // 0번째 title → contentList 0,1,2번 content 넣기 (통신사 이용약관)
                signUpAgreeDataList.add(
                    SignUpAgreeData(
                        isCheck = false,
                        title = title,
                        content = null, // 단일 content는 빈 문자열
                        contentList = signUpAgreeContent.slice(0..2) // 0~2번 content
                    )
                )
                contentIndex += 3 // 다음 content는 3번부터 시작
            } else {
                // 나머지 title → content만 넣기
                signUpAgreeDataList.add(
                    SignUpAgreeData(
                        isCheck = false,
                        title = title,
                        content = signUpAgreeContent.getOrNull(contentIndex) ?: "", // content 값
                        contentList = null // 리스트는 사용 안 함
                    )
                )
                contentIndex += 1 // 다음 content 1개씩 증가
            }
        }

        signUpAgreeAdapter = SignUpAgreeAdapter(
            signUpAgreeDataList, { list ->
                // list isCheck 확인 후 전체 동의 일때 버튼 활성화 처리
                val isAllAgree = list.all { it.isCheck }
                viewModel.statusAllAgree(isAllAgree)
            }, {
                if (it.contentList.isNullOrEmpty()) {
                    TermsBottomSheet(
                        it.title,
                        it.content ?: ""
                    ).show(parentFragmentManager, "terms")
                } else {
                    AgencyTermsBottomSheet(
                        it.title,
                        it.contentList
                    ).show(parentFragmentManager, "terms")
                }
            }
        )
        binding.rvTerms.adapter = signUpAgreeAdapter
        binding.rvTerms.addItemDecoration(SignUpAgreeDecoration(binding.root.context))

        // 이용약관 동의 항목 초기화
        viewModel.statusAllAgree(binding.ivCheckBoxAll.isChecked)
        signUpAgreeAdapter.setAllAgreeClick(binding.ivCheckBoxAll.isChecked)
    }

    override fun initDataBinding() {
        with(activityViewModel) {
            isLoginLiveData.observe(viewLifecycleOwner) {
                if (it) {
                    findNavController().navigate(
                        R.id.action_SignUpAuthFragment_to_SignUpAuthNiceFragment,
                        null,
                        navOptions {
                            popUpTo(R.id.SignUpAuthFragment) {
                                inclusive = true
                            }
                        }
                    )
                }
            }
        }

        with(viewModel) {
            isLoading.observe(viewLifecycleOwner) {
                if (it) {
                    showLoadingDialog()
                } else {
                    hideLoadingDialog()
                }
            }

            signUpAuthAllAgreeCheckLiveData.observe(viewLifecycleOwner) {
                binding.ivCheckBoxAll.isChecked = it
                binding.btnOk.isEnabled = it
            }
        }
    }

    override fun initAfterBinding() {
        setOnClick(binding.ivCheckBoxAll) {
            viewModel.statusAllAgree(binding.ivCheckBoxAll.isChecked)
            signUpAgreeAdapter.setAllAgreeClick(binding.ivCheckBoxAll.isChecked)
        }

        setOnClick(binding.btnOk) {
            findNavController().navigate(R.id.action_SignUpAuthFragment_to_SignUpAuthNiceFragment)
        }

        // ✅ 강제 뒤로가기
        setOnClick(binding.ivArrowReturn) {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
}