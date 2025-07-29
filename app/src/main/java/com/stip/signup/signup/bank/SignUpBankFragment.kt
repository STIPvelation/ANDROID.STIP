package com.stip.stip.signup.signup.bank

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.stip.stip.signup.Constants
import com.stip.stip.R
import com.stip.stip.signup.base.BaseFragment
import com.stip.stip.signup.customview.CustomContentDialog
import com.stip.stip.databinding.FragmentSignUpBankBinding
import com.stip.stip.signup.keypad.KeypadAdapter
import com.stip.stip.signup.keypad.KeypadItem
import com.stip.stip.signup.keypad.KeypadType
import com.stip.stip.signup.model.BankData
import com.stip.stip.signup.model.RequestAccountNumber
import com.stip.stip.signup.signup.SignUpViewModel
import com.stip.stip.signup.utils.PreferenceUtil
import com.stip.stip.signup.utils.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignUpBankFragment: BaseFragment<FragmentSignUpBankBinding, SignUpBankViewModel>() {
    override val layoutResource: Int
        get() = R.layout.fragment_sign_up_bank

    override val viewModel: SignUpBankViewModel by viewModels()
    private val activityViewModel: SignUpViewModel by activityViewModels()

    private lateinit var keypadAdapter: KeypadAdapter

    override fun initStartView() {
        val di = arguments?.getString(Constants.BUNDLE_AUTH_DI_KEY) ?: ""
        if (di.isNotBlank()) {
            viewModel.requestGetNiceAuth(di)
        }

        val selectedBank = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(Constants.BUNDLE_SELECT_BANK_KEY, BankData::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable(Constants.BUNDLE_SELECT_BANK_KEY) as? BankData
        }
        selectedBank?.let {
            // 선택된 은행 정보를 UI에 반영
            binding.tvSignUpBankValue.text = it.name
        }

        setKyePad()
    }

    override fun initDataBinding() {
        with(viewModel) {
            isLoading.observe(viewLifecycleOwner) {
                if (it) {
                    showLoadingDialog()
                } else {
                    hideLoadingDialog()
                }
            }

            errorLiveData.observe(viewLifecycleOwner) {
                val errorMessage: String = when (it) {
                    Constants.NETWORK_DUPLICATE_ERROR_CODE -> getString(R.string.error_duplicate_409)
                    Constants.NETWORK_SERVER_ERROR_CODE -> getString(R.string.error_admin_500)
                    else -> getString(R.string.error_network_999)
                }

                CustomContentDialog(
                    binding.root.context
                ) {
                    activity?.finish()
                }.setText(
                    getString(R.string.dialog_bank_guide_title),
                    errorMessage,
                    "",
                    getString(R.string.common_confirm)
                )
            }

            signUpAuthNiceLiveData.observe(viewLifecycleOwner) {
                PreferenceUtil.putString(Constants.PREF_KEY_OCR_NAME_VALUE, it.name)
                PreferenceUtil.putString(Constants.PREF_KEY_OCR_BIRTHDAY_VALUE, it.birthdate)
                PreferenceUtil.putString(Constants.PREF_KEY_OCR_PHONE_NUMBER_VALUE, it.phoneNumber)

                // 인증 성공 처리
                activityViewModel.di.value = it.di
                activityViewModel.name.value = it.name
                activityViewModel.birthdate.value = it.birthdate
                activityViewModel.phoneNumber.value = it.phoneNumber
            }

            signUpBankAuthNumberCheckLiveData.observe(viewLifecycleOwner) {
                binding.btnSend.isEnabled = it
            }

            signUpBankAuthVerify.observe(viewLifecycleOwner) {
                activityViewModel.accountNumber.value = binding.tvSignUpBankNumberValue.text.toString()
                activityViewModel.bankCode.value = Utils.getBankCodeByName(binding.root.context, binding.tvSignUpBankValue.text.toString())

                // 성공
                val bundle = Bundle().apply {
                    putString(Constants.BUNDLE_ACCOUNT_NAME_KEY, binding.tvSignUpBankValue.text.toString())
                    putString(Constants.BUNDLE_ACCOUNT_NUMBER_KEY, binding.tvSignUpBankNumberValue.text.toString())
                    putString(Constants.BUNDLE_REQUEST_NO_KEY, it.requestNo)
                    putString(Constants.BUNDLE_REQUEST_RESPONSE_UNIQUE_ID_KEY, it.responseUniqueId)
                }
                findNavController().navigate(R.id.action_SignUpBankFragment_to_SignUpBankDepositFragment, bundle)
            }
        }
    }

    override fun initAfterBinding() {
        setOnClick(binding.tvSignUpBankTitle) {
            CoroutineScope(Dispatchers.Main).launch {
                viewModel.showProgress()
                delay(2000)
                viewModel.hideProgress()
            }
        }

        setOnClick(binding.btnSend) {
            android.util.Log.i("API CALL 2", activityViewModel.name.toString())
            viewModel.requestPostBankVerify(
                RequestAccountNumber(
                    name = activityViewModel.name.value ?: "",
                    accountNumber = binding.tvSignUpBankNumberValue.text.toString(),
                    bankCode = Utils.getBankCodeByName(binding.root.context, binding.tvSignUpBankValue.text.toString())
                )
            )
        }

        setOnClick(binding.tvSignUpBankValue) {
            findNavController().navigate(
                R.id.action_SignUpBankFragment_to_SignUpBankSelectFragment,
                null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.SignUpBankSelectFragment, true) // 기존 B 제거
                    .setLaunchSingleTop(true) // 현재 대상이 스택에 있으면 새로 추가하지 않음
                    .build()
            )
        }

        setOnClick(binding.tvSignUpBankNumberValue) {
            binding.rvNumber.visibility = View.VISIBLE
            binding.nsvBodySection.post {
                binding.nsvBodySection.smoothScrollTo(0, binding.nsvBodySection.bottom)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.rvNumber.visibility == View.VISIBLE) {
                    // 키패드가 보이면 숨기기
                    binding.rvNumber.visibility = View.GONE
                } else {
                    // 키패드가 없으면 원래 백버튼 동작 수행
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private fun setKyePad() {
        val pinInput = StringBuilder()
        // 1-9, 0 순서로 기본 정렬된 숫자 생성
        val numberItems = (1..9).map { KeypadItem(it.toString(), KeypadType.NUMBER) } +
                         listOf(KeypadItem("0", KeypadType.NUMBER))
        val fixedItems = listOf(
            KeypadItem("완료", KeypadType.DONE),
            KeypadItem("", KeypadType.DELETE, R.drawable.ic_del_white_31dp)
        )
        val keypadItemList = (numberItems + fixedItems).toMutableList()
        keypadAdapter = KeypadAdapter(
            keypadItemList
        ) { item ->
            when (item.type) {
                KeypadType.NUMBER -> {
                    pinInput.append(item.value)

                    binding.tvSignUpBankNumberValue.text = pinInput.toString()
                    viewModel.isBankAuthNumberCheck(
                        binding.tvSignUpBankValue.text.toString(),
                        pinInput.toString()
                    )
                }
                KeypadType.DONE -> {
                    // 완료 버튼 처리 - 키패드 숨김 처리
                    binding.rvNumber.visibility = View.GONE
                }
                KeypadType.DELETE -> {
                    if (pinInput.isNotEmpty()) {
                        pinInput.deleteCharAt(pinInput.length - 1)
                        binding.tvSignUpBankNumberValue.text = pinInput.toString()
                        viewModel.isBankAuthNumberCheck(
                            binding.tvSignUpBankValue.text.toString(),
                            pinInput.toString()
                        )
                    }
                }
                KeypadType.SHUFFLE -> {}
            }
        }
        binding.rvNumber.adapter = keypadAdapter
    }
}