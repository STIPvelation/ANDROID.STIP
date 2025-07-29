package com.stip.stip.signup.signup.pin.number

import android.app.Activity
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.stip.stip.R
import com.stip.stip.databinding.FragmentSignUpPinNumberBinding
import com.stip.stip.signup.base.BaseFragment
import com.stip.stip.signup.signup.SignUpActivity
import com.stip.stip.signup.signup.SignUpOCRWebViewActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpPinNumberFragment : BaseFragment<FragmentSignUpPinNumberBinding, SignUpPinNumberViewModel>() {
    override val layoutResource: Int
        get() = R.layout.fragment_sign_up_pin_number

    override val viewModel: SignUpPinNumberViewModel by viewModels()

    private val ocrResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // OCR 성공 시 SignUpPinNumberSettingFragment로 이동
            findNavController().navigate(R.id.action_SignUpPinNumberFragment_to_SignUpPinNumberSettingFragment)
        }
    }

    override fun initStartView() {
    }

    override fun initDataBinding() {
    }

    override fun initAfterBinding() {
        setOnClick(binding.ivBack) {
            findNavController().navigateUp()
        }

        setOnClick(binding.btnTake) {
            val intent = Intent(activity as SignUpActivity, SignUpOCRWebViewActivity::class.java)
            ocrResultLauncher.launch(intent)
        }
    }
}