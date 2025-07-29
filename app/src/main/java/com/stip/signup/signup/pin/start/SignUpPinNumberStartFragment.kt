package com.stip.stip.signup.signup.pin.start

import android.text.Html
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.stip.stip.R
import com.stip.stip.databinding.FragmentSignUpPinNumberStartBinding
import com.stip.stip.signup.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpPinNumberStartFragment : BaseFragment<FragmentSignUpPinNumberStartBinding, SignUpPinNumberStartViewModel>() {
    override val layoutResource: Int
        get() = R.layout.fragment_sign_up_pin_number_start

    override val viewModel: SignUpPinNumberStartViewModel by viewModels()

    override fun initStartView() {
        val result = String.format(binding.root.context.getString(R.string.sign_up_pin_number_start_title))
        binding.tvSignUpPinNumberStartTitle.text = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(result, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(result)
        }
    }

    override fun initDataBinding() {
    }

    override fun initAfterBinding() {
        setOnClick(binding.ivBack) {
            findNavController().navigateUp()
        }

        setOnClick(binding.btnStart) {
            findNavController().navigate(R.id.action_SignUpPinNumberStartFragment_to_SignUpPinNumberFinishFragment)
        }
    }
}