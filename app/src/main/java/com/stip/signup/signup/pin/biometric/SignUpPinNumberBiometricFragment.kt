package com.stip.stip.signup.signup.pin.biometric

import android.util.Log
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.stip.stip.R
import com.stip.stip.databinding.FragmentSignUpPinNumberBiometricBinding
import com.stip.stip.signup.Constants
import com.stip.stip.signup.Constants.TAG
import com.stip.stip.signup.base.BaseFragment
import com.stip.stip.signup.biometric.BiometricAuthHelper
import com.stip.stip.signup.getFragmentActivity
import com.stip.stip.signup.utils.PreferenceUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpPinNumberBiometricFragment : BaseFragment<FragmentSignUpPinNumberBiometricBinding, SignUpPinNumberBiometricViewModel>(),
    BiometricAuthHelper.AuthCallback {
    override val layoutResource: Int
        get() = R.layout.fragment_sign_up_pin_number_biometric

    override val viewModel: SignUpPinNumberBiometricViewModel by viewModels()
    private lateinit var biometricAuthHelper: BiometricAuthHelper

    override fun initStartView() {
        val fragmentActivity = requireContext().getFragmentActivity()
        if (fragmentActivity == null) {
            Log.e("Biometric", "Activity를 찾을 수 없습니다.")
            return
        }
        biometricAuthHelper = BiometricAuthHelper(fragmentActivity, this)
    }

    override fun initDataBinding() {
    }

    override fun initAfterBinding() {
        setOnClick(binding.ivBack) {
            findNavController().navigateUp()
        }

        setOnClick(binding.btnSignUpPinNumberBiometricLater) {
            PreferenceUtil.putString(Constants.PREF_KEY_BASIC_LOGIN_TYPE, Constants.BASIC_LOGIN_TYPE_PIN_NUMBER)
            findNavController().navigate(R.id.action_SignUpPinNumberBiometricFragment_to_SignUpPinNumberStartFragment)
        }

        setOnClick(binding.btnSignUpPinNumberBiometricUseAgree) {
            biometricAuthHelper.showBiometricPrompt()

            //findNavController().navigate(R.id.action_SignUpPinNumberBiometricFragment_to_SignUpPinNumberStartFragment)
        }
    }

    override fun onAuthSuccess() {
        PreferenceUtil.putString(Constants.PREF_KEY_BASIC_LOGIN_TYPE, Constants.BASIC_LOGIN_TYPE_BIOMETRIC_AUTH)
        findNavController().navigate(R.id.action_SignUpPinNumberBiometricFragment_to_SignUpPinNumberStartFragment)
    }

    override fun onAuthFailed() {
        Log.e(TAG, "지문 일치 하지 않음")
    }

    override fun onAuthError(errorMsg: String) {
        Log.d(TAG, "지문 error = $errorMsg")
    }
}