package com.stip.stip.signup.signup.kyc.inform

import android.os.Bundle
import android.text.InputFilter
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.stip.stip.R
import com.stip.stip.databinding.FragmentSignUpKycInformBinding
import com.stip.stip.signup.Constants
import com.stip.stip.signup.base.BaseFragment
import com.stip.stip.signup.getTextString
import com.stip.stip.signup.signup.SignUpViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpKYCInformFragment : BaseFragment<FragmentSignUpKycInformBinding, SignUpKYCInformViewModel>() {
    override val layoutResource: Int
        get() = R.layout.fragment_sign_up_kyc_inform

    override val viewModel: SignUpKYCInformViewModel by activityViewModels()
    private val activityViewModel: SignUpViewModel by activityViewModels()

    override fun initStartView() {
        val address = arguments?.getString(Constants.BUNDLE_KYC_ADDRESS_KEY)
        if (!address.isNullOrBlank()) {
            binding.tvSignUpKycInformCurrentAddressValue.text = address
            viewModel.isKYCInformCheck(
                binding.etNickSignUpKycInformPassportLastName.getTextString(),
                binding.etNickSignUpKycInformPassportFirstName.getTextString(),
                binding.tvSignUpKycInformCurrentAddressValue.text.toString()
            )
        }

        binding.etNickSignUpKycInformPassportLastName.setText(viewModel.lastName)
        binding.etNickSignUpKycInformPassportFirstName.setText(viewModel.firstName)

        setRxTextChangeCallback(binding.etNickSignUpKycInformPassportLastName) {
            viewModel.lastName = it

            viewModel.isKYCInformCheck(
                it,
                binding.etNickSignUpKycInformPassportFirstName.getTextString(),
                binding.tvSignUpKycInformCurrentAddressValue.text.toString()
            )
        }

        setRxTextChangeCallback(binding.etNickSignUpKycInformPassportFirstName) {
            viewModel.firstName = it

            viewModel.isKYCInformCheck(
                binding.etNickSignUpKycInformPassportLastName.getTextString(),
                it,
                binding.tvSignUpKycInformCurrentAddressValue.text.toString()
            )
        }

        binding.etNickSignUpKycInformPassportLastName.filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
            if (!source.matches(Regex("^[a-zA-Z]*$"))) {
                showToast(R.string.common_only_english)
                return@InputFilter "" // 입력값 전체 삭제
            }

            source // 정상적으로 입력
        })

        binding.etNickSignUpKycInformPassportFirstName.filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
            if (!source.matches(Regex("^[a-zA-Z]*$"))) {
                showToast(R.string.common_only_english)
                return@InputFilter "" // 입력값 전체 삭제
            }

            source // 정상적으로 입력
        })
    }

    override fun initDataBinding() {
        with(viewModel) {
            signUpKYCInformCheckLiveData.observe(viewLifecycleOwner) {
                binding.btnConfirm.isEnabled = it
            }
        }
    }

    override fun initAfterBinding() {
        setOnClick(binding.ivBack) {
            findNavController().navigateUp()
        }

        setOnClick(binding.tvSignUpKycInformCurrentAddressValue) {
            findNavController().navigate(
                R.id.action_SignUpKYCInformFragment_to_SignUpKYCAddressFragment,
                null,
                navOptions {
                    popUpTo(R.id.SignUpKYCInformFragment) { inclusive = false } // B 유지
                    launchSingleTop = true
                }
            )
        }

        setOnClick(binding.btnConfirm) {
            activityViewModel.englishFirstName.value = binding.etNickSignUpKycInformPassportFirstName.getTextString()
            activityViewModel.englishLastName.value = binding.etNickSignUpKycInformPassportLastName.getTextString()

            val bundle = Bundle().apply {
                putString(Constants.BUNDLE_KYC_LAST_NAME_KEY, binding.etNickSignUpKycInformPassportLastName.text.toString())
                putString(Constants.BUNDLE_KYC_FIRST_NAME_KEY, binding.etNickSignUpKycInformPassportFirstName.text.toString())
                putString(Constants.BUNDLE_KYC_ADDRESS_KEY, binding.tvSignUpKycInformCurrentAddressValue.text.toString())
            }

            findNavController().navigate(
                R.id.action_SignUpKYCInformFragment_to_SignUpKYCFragment,
                bundle,
                navOptions {
                    popUpTo(R.id.SignUpKYCFragment) { inclusive = false }  // A는 유지
                    launchSingleTop = true
                }
            )
        }
    }
}