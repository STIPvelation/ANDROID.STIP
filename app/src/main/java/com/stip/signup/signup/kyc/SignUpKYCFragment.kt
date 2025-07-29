package com.stip.stip.signup.signup.kyc

import android.os.Bundle
import android.util.Patterns
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.stip.stip.signup.Constants
import com.stip.stip.R
import com.stip.stip.signup.base.BaseFragment
import com.stip.stip.signup.customview.KYCInformBottomSheet
import com.stip.stip.databinding.FragmentSignUpKycBinding
import com.stip.stip.signup.getTextString
import com.stip.stip.signup.signup.SignUpViewModel
import com.stip.stip.signup.utils.PreferenceUtil
import com.stip.stip.signup.utils.Utils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpKYCFragment: BaseFragment<FragmentSignUpKycBinding, SignUpKYCViewModel>() {
    override val layoutResource: Int
        get() = R.layout.fragment_sign_up_kyc

    override val viewModel: SignUpKYCViewModel by activityViewModels()
    private val activityViewModel: SignUpViewModel by activityViewModels()

    override fun initStartView() {
        setRxTextChangeCallback(binding.etSignUpEmail) {
            viewModel.email.value = it
        }

        val lastName = arguments?.getString(Constants.BUNDLE_KYC_LAST_NAME_KEY)
        val firstName = arguments?.getString(Constants.BUNDLE_KYC_FIRST_NAME_KEY)
        val address = arguments?.getString(Constants.BUNDLE_KYC_ADDRESS_KEY)
        if (!lastName.isNullOrBlank() && !firstName.isNullOrBlank()) {
            binding.tvSignUpNameValue.text = "$lastName$firstName"
            viewModel.name.value = "$lastName$firstName"
        } else if (!viewModel.name.value.isNullOrBlank()) {  // 기존 값 유지
            binding.tvSignUpNameValue.text = viewModel.name.value
        }

        if (!address.isNullOrBlank()) {
            binding.tvSignUpKycAddressValue.text = address
            viewModel.address.value = address
        } else if (!viewModel.address.value.isNullOrBlank()) { // 기존 값 유지
            binding.tvSignUpKycAddressValue.text = viewModel.address.value
        }

        // ✅ 기존 데이터 유지 로직 추가
        if (!viewModel.email.value.isNullOrBlank()) {
            binding.etSignUpEmail.setText(viewModel.email.value)
        }
        if (!viewModel.purpose.value.isNullOrBlank()) {
            binding.tvSignUpSignUpKycPurposeValue.text = viewModel.purpose.value
        }
        if (!viewModel.source.value.isNullOrBlank()) {
            binding.tvSignUpKycSourceValue.text = viewModel.source.value
        }
        if (!viewModel.job.value.isNullOrBlank()) {
            binding.tvSignUpKycJobValue.text = viewModel.job.value
        }
    }

    override fun initDataBinding() {
        with(viewModel) {
            isLoading.observe(viewLifecycleOwner) {
                if (it) {
                    showProgress()
                } else {
                    hideProgress()
                }
            }

            isAccount.observe(viewLifecycleOwner) { isYes ->
                /** 선택시 폰트 변경 */
                binding.btnAccountOpenYes.isChecked = isYes
                binding.btnAccountOpenNo.isChecked = !isYes

                if (isYes) {
                    binding.btnAccountOpenYes.setTextColor(ContextCompat.getColor(binding.root.context, R.color.white))
                    binding.btnAccountOpenNo.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_gray_B0B8C1_100))
                } else {
                    binding.btnAccountOpenYes.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_gray_B0B8C1_100))
                    binding.btnAccountOpenNo.setTextColor(ContextCompat.getColor(binding.root.context, R.color.white))
                }
            }

            isButtonEnabled.observe(viewLifecycleOwner) {
                binding.btnConfirm.isEnabled = it
            }
        }
    }

    override fun initAfterBinding() {
        setOnClick(binding.ivBack) {
            findNavController().navigateUp()
        }

        setOnClick(binding.tvSignUpNameValue) {
            //findNavController().navigate(R.id.action_SignUpKYCFragment_to_SignUpKYCInformFragment)

            val bundle = Bundle().apply {
                putString(Constants.BUNDLE_KYC_ADDRESS_KEY, viewModel.address.value)
                // 이름이 null이 아니고 공백이 있는지 확인
                val name = viewModel.name.value
                if (!name.isNullOrBlank()) {
                    val spaceIndex = name.indexOf(" ")
                    if (spaceIndex != -1) {
                        // 공백이 있을 경우 성과 이름 분리
                        putString(Constants.BUNDLE_KYC_LAST_NAME_KEY, name.substring(0, spaceIndex))
                        putString(Constants.BUNDLE_KYC_FIRST_NAME_KEY, name.substring(spaceIndex + 1))
                    } else {
                        // 공백이 없을 경우, 전체 이름을 성으로 처리하고, 첫 이름은 빈 문자열 처리
                        putString(Constants.BUNDLE_KYC_LAST_NAME_KEY, name)
                        putString(Constants.BUNDLE_KYC_FIRST_NAME_KEY, "")
                    }
                }
            }
            findNavController().navigate(
                R.id.action_SignUpKYCFragment_to_SignUpKYCInformFragment,
                bundle,
                navOptions {
                    launchSingleTop = true
                }
            )
        }

        setOnClick(binding.tvSignUpKycAddressValue) {
            val bundle = Bundle().apply {
                putString(Constants.BUNDLE_KYC_ADDRESS_KEY, viewModel.address.value)
                // 이름이 null이 아니고 공백이 있는지 확인
                val name = viewModel.name.value
                if (!name.isNullOrBlank()) {
                    val spaceIndex = name.indexOf(" ")
                    if (spaceIndex != -1) {
                        // 공백이 있을 경우 성과 이름 분리
                        putString(Constants.BUNDLE_KYC_LAST_NAME_KEY, name.substring(0, spaceIndex))
                        putString(Constants.BUNDLE_KYC_FIRST_NAME_KEY, name.substring(spaceIndex + 1))
                    } else {
                        // 공백이 없을 경우, 전체 이름을 성으로 처리하고, 첫 이름은 빈 문자열 처리
                        putString(Constants.BUNDLE_KYC_LAST_NAME_KEY, name)
                        putString(Constants.BUNDLE_KYC_FIRST_NAME_KEY, "")
                    }
                }
            }
            findNavController().navigate(
                R.id.action_SignUpKYCFragment_to_SignUpKYCInformFragment,
                bundle,
                navOptions {
                    launchSingleTop = true
                }
            )
        }

        setOnClick(binding.btnAccountOpenYes) {
            viewModel.isAccount.value = true
        }

        setOnClick(binding.btnAccountOpenNo) {
            viewModel.isAccount.value = false
        }

        setOnClick(binding.tvSignUpSignUpKycPurposeValue) {
            val purposeUseList = resources.getStringArray(R.array.kyc_purpose_of_use)
            KYCInformBottomSheet(
                getString(R.string.sign_up_kyc_purpose_select),
                purposeUseList.toList(), {
                    binding.tvSignUpSignUpKycPurposeValue.text = it
                    viewModel.purpose.value = it
                }
            ).show(parentFragmentManager, "kycInform")
        }

        setOnClick(binding.tvSignUpKycSourceValue) {
            val sourceList = resources.getStringArray(R.array.kyc_source_capital)
            KYCInformBottomSheet(
                getString(R.string.sign_up_kyc_source),
                sourceList.toList(), {
                    binding.tvSignUpKycSourceValue.text = it
                    viewModel.source.value = it
                }
            ).show(parentFragmentManager, "kycInform")
        }

        setOnClick(binding.tvSignUpKycJobValue) {
            val jobList = resources.getStringArray(R.array.kyc_job_select)
            KYCInformBottomSheet(
                getString(R.string.sign_up_kyc_job_select),
                jobList.toList(), {
                    binding.tvSignUpKycJobValue.text = it
                    viewModel.job.value = it
                }
            ).show(parentFragmentManager, "kycInform")
        }

        setOnClick(binding.btnConfirm) {
            val emailValue = binding.etSignUpEmail.getTextString()
            if (emailValue.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
                showToast(R.string.common_invalid_email)
            } else {
                PreferenceUtil.putString(Constants.PREF_KEY_OCR_EMAIL_VALUE, binding.etSignUpEmail.getTextString())

                activityViewModel.email.value = binding.etSignUpEmail.getTextString()
                activityViewModel.isDirectAccount.value = viewModel.isButtonEnabled.value
                activityViewModel.usagePurpose.value = viewModel.purpose.value?.let {
                    Utils.getKycCode(binding.root.context,
                        it, R.array.kyc_purpose_of_use, R.array.kyc_purpose_of_use_code)
                }
                activityViewModel.sourceOfFunds.value = viewModel.source.value?.let {
                    Utils.getKycCode(binding.root.context,
                        it, R.array.kyc_source_capital, R.array.kyc_source_capital_code)
                }
                activityViewModel.job.value = viewModel.job.value?.let {
                    Utils.getKycCode(binding.root.context,
                        it, R.array.kyc_job_select, R.array.kyc_job_select_code)
                }

                findNavController().navigate(R.id.action_SignUpKYCFragment_to_SignUpPinNumberFragment)
            }
        }
    }
}