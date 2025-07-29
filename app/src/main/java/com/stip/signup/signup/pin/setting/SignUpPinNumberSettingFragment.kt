package com.stip.stip.signup.signup.pin.setting

import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.stip.stip.MainActivity
import com.stip.stip.R
import com.stip.stip.databinding.FragmentSignUpPinNumberSettingBinding
import com.stip.stip.signup.Constants
import com.stip.stip.signup.Constants.BUNDLE_AUTH_DI_KEY
import com.stip.stip.signup.base.BaseFragment
import com.stip.stip.signup.customview.CustomContentDialog
import com.stip.stip.signup.keypad.KeypadAdapter
import com.stip.stip.signup.keypad.KeypadItem
import com.stip.stip.signup.keypad.KeypadType
import com.stip.stip.signup.keypad.GridSpacingItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import com.stip.stip.signup.model.RequestAuthLogin
import com.stip.stip.signup.model.RequestPinNumber
import com.stip.stip.signup.pin.PinAdapter
import com.stip.stip.signup.signup.SignUpViewModel
import com.stip.stip.signup.utils.PreferenceUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpPinNumberSettingFragment : BaseFragment<FragmentSignUpPinNumberSettingBinding, SignUpPinNumberSettingViewModel>() {
    override val layoutResource: Int
        get() = R.layout.fragment_sign_up_pin_number_setting

    override val viewModel: SignUpPinNumberSettingViewModel by viewModels()
    private val activityViewModel: SignUpViewModel by activityViewModels()

    private lateinit var pinAdapter: PinAdapter
    private lateinit var keypadAdapter: KeypadAdapter
    private val pinInput = StringBuilder()

    private var firstPinInput: String? = null // 첫 번째 입력 저장
    private var isConfirming = false // 확인 단계 여부
    private var failCount = 0 // 비밀번호 불일치 횟수

    private var isLogin = false
    private var di: String? = null


    override fun initStartView() {
        di = arguments?.getString(BUNDLE_AUTH_DI_KEY) ?: ""
        resetPinInput()
        setKeyPad()
    }

    override fun initDataBinding() {
        with(activityViewModel) {
            isLoginLiveData.observe(viewLifecycleOwner) {
                isLogin = it
            }

            signUpLiveData.observe(viewLifecycleOwner) {
                if (it.memberId.isNotBlank()) {
                    PreferenceUtil.putString(Constants.PREF_KEY_MEMBER_ID_VALUE, it.memberId)
                    // PIN 번호는 이미 PIN 설정 시점에 저장되었으므로 여기서는 저장하지 않음

                    onPinSetSuccess()
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
        }

        with(viewModel) {
            isLoading.observe(viewLifecycleOwner) {
                if (it) {
                    showLoadingDialog()
                } else {
                    hideLoadingDialog()
                }
            }
            pinNumberChangeLiveData.observe(viewLifecycleOwner) {
                if (it) {
                    PreferenceUtil.putString(Constants.PREF_KEY_PIN_VALUE, pinInput.toString())
                    di?.let {
                        viewModel.requestPostAuthLogin(
                            RequestAuthLogin(
                                di = it,
                                pin = pinInput.toString()
                            )
                        )
                    }
                }
            }

            authLoginLiveData.observe(viewLifecycleOwner) {
                PreferenceUtil.putString(Constants.PREF_KEY_AUTH_TOKEN_VALUE, it.accessToken)
                MainActivity.startMainActivity(requireActivity())
                activity?.finish()
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
        }
    }

    override fun initAfterBinding() {
        // 타이틀 및 안내 메시지 설정
        binding.tvSignUpPinNumberSettingTitle.text = getString(R.string.sign_up_pin_number_setting_title)
        binding.tvSignUpPinNumberSettingSameConditionWarning.text = getString(R.string.sign_up_pin_number_setting_same_condition_warning)
        binding.tvSignUpPinNumberSettingSameConditionWarning.visibility = View.VISIBLE
        
        setOnClick(binding.ivBack) {
            findNavController().navigateUp()
        }
    }

    private fun setKeyPad() {
        // 1-9, 0 순서로 기본 정렬된 숫자 생성
        val numberItems = (1..9).map { KeypadItem(it.toString(), KeypadType.NUMBER) } +
                         listOf(KeypadItem("0", KeypadType.NUMBER))
        val fixedItems = listOf(
            KeypadItem("완료", KeypadType.DONE),
            KeypadItem("", KeypadType.DELETE, R.drawable.ic_del_white_31dp)
        )
        val keypadItemList = (numberItems + fixedItems).toMutableList()

        keypadAdapter = KeypadAdapter(keypadItemList) { item ->
            when (item.type) {
                KeypadType.NUMBER -> handleNumberInput(item.value)
                KeypadType.DELETE -> handleDelete()
                KeypadType.DONE -> {
                    // 완료 버튼 처리 - 필요한 경우 수정
                    binding.rvSignUpPinNumberKeypad.visibility = View.GONE 
                }
                KeypadType.SHUFFLE -> {} // 사용하지 않음
            }
        }
        // Add negative spacing decoration to bring keypad buttons closer horizontally
        binding.rvSignUpPinNumberKeypad.addItemDecoration(
            GridSpacingItemDecoration(
                spanCount = 3,
                horizontalSpacing = -50, // Negative spacing to bring items closer horizontally
                verticalSpacing = 0,
                includeEdge = false
            )
        )
        binding.rvSignUpPinNumberKeypad.adapter = keypadAdapter
    }

    /** 숫자 입력 처리 */
    private fun handleNumberInput(value: String) {
        if (pinInput.length < 6) {
            pinInput.append(value)
            pinAdapter.updatePinCount(pinInput.length)

            // 6자리 입력 완료 시 처리
            if (pinInput.length == 6) {
                val pin = pinInput.toString()

                if (!isConfirming) {
                    // 1. 동일하거나 연속된 숫자 검사
                    if (isSequentialOrRepeated(pin)) {
                        CustomContentDialog(
                            binding.root.context
                        ) {
                            resetPinSetting()
                        }.setText(
                            getString(R.string.dialog_pin_number_title),
                            getString(R.string.sign_up_pin_number_setting_same_condition_warning),
                            "",
                            getString(R.string.common_confirm),
                            false
                        )
                        resetPinInput()
                        return
                    }

                    // 2. 개인정보 포함 여부 검사
                    if (containsPersonalInfo(pin)) {
                        CustomContentDialog(
                            binding.root.context
                        ) {
                            resetPinSetting()
                        }.setText(
                            getString(R.string.dialog_pin_number_title),
                            getString(R.string.dialog_pin_number_personal_information_warning),
                            "",
                            getString(R.string.common_confirm),
                            false
                        )
                        resetPinInput()
                        return
                    }

                    // 위 검사를 통과하면 비밀번호 확인 단계로 진행
                    // 타이틀을 PIN 비밀번호 확인으로 변경
                    binding.tvSignUpPinNumberSettingTitle.text = getString(R.string.sign_up_pin_number_setting_confirm_title)

                    binding.tvSignUpPinNumberSettingSameConditionWarning.text = getString(R.string.sign_up_pin_number_setting_confirm_warning)

                    // 첫 번째 입력 저장 후 초기화
                    firstPinInput = pin
                    isConfirming = true
                    resetPinInput()
                } else {
                    // 두 번째 입력과 비교
                    if (firstPinInput == pin) {
                        if (isLogin) {
                            // 로그인 PIN 번호 변경
                            di?.let {
                                viewModel.requestPatchPinNumber(it, RequestPinNumber(pin))
                            }
                        } else {
                            // 정상 회원가입 동작
                            activityViewModel.pin.value = pin
                            // PIN 번호를 PreferenceUtil에 저장
                            PreferenceUtil.putString(Constants.PREF_KEY_PIN_VALUE, pin)
                            android.util.Log.d("SignUpPin", "PIN 번호 저장 완료: $pin")
                            activityViewModel.requestPostSignUpMembers()
                        }
                    } else {
                        onPinSetFailure()
                    }
                }
            }
        }
    }

    /** 삭제 버튼 처리 */
    private fun handleDelete() {
        if (pinInput.isNotEmpty()) {
            pinInput.deleteCharAt(pinInput.length - 1)
            pinAdapter.updatePinCount(pinInput.length)
        }
    }

    /** PIN 입력 초기화 */
    private fun resetPinInput() {
        pinInput.clear()
        pinAdapter = PinAdapter(MutableList(6) { false })
        binding.rvSignUpPinNumberPassword.adapter = pinAdapter
    }

    /** 비밀번호 설정 성공 */
    private fun onPinSetSuccess() {
        findNavController().navigate(R.id.action_SignUpPinNumberSettingFragment_to_SignUpPinNumberBiometricFragment)
    }

    /** 비밀번호 설정 실패 */
    private fun onPinSetFailure() {
        binding.tvSignUpPinNumberWarning.visibility = View.VISIBLE
        failCount++

        if (failCount >= 5) {
            // 5회 실패 시 비밀번호 재설정 메시지 표시 후 초기화
            binding.tvSignUpPinNumberWarning.text = getString(R.string.sign_up_pin_number_setting_confirm_not_correct, failCount)

            CustomContentDialog(
                binding.root.context
            ) {
                resetPinSetting()
            }.setText(
                getString(R.string.dialog_pin_number_title),
                getString(R.string.dialog_pin_number_content),
                "",
                getString(R.string.common_confirm),
                false
            )
        } else {
            // 1~4회 실패 메시지 출력
            binding.tvSignUpPinNumberWarning.text = getString(R.string.sign_up_pin_number_setting_confirm_not_correct, failCount)
            resetPinInput()
        }
    }

    /** 비밀번호 설정 전체 초기화 */
    private fun resetPinSetting() {
        binding.tvSignUpPinNumberWarning.visibility = View.GONE
        binding.tvSignUpPinNumberSettingSameConditionWarning.text = getString(R.string.sign_up_pin_number_setting_same_condition_warning)
        firstPinInput = null
        isConfirming = false
        failCount = 0
        resetPinInput()
    }

    /** 동일한 숫자 또는 연속된 숫자(오름차순/내림차순) 체크 */
    private fun isSequentialOrRepeated(pin: String): Boolean {

        if (pin.length != 6) return false // 6자리 고정

        // 1. 동일한 숫자로만 이루어진 경우 (예: 111111)
        if (pin.all { it == pin[0] }) {
            return true
        }

        // 2. 연속된 숫자인 경우 (예: 123456, 987654)
        val ascending = "0123456789"
        val descending = "9876543210"
        if (ascending.contains(pin) || descending.contains(pin)) {
            return true
        }

        return false // 위 조건에 해당하지 않으면 유효한 PIN
    }

    /** 개인정보 포함 여부 체크 */
    private fun containsPersonalInfo(pin: String): Boolean {

        val userPersonalInfo = listOf(
            activityViewModel.birthdate.value,
            activityViewModel.phoneNumber.value
        )
        for (info in userPersonalInfo) {
            if (!info.isNullOrEmpty()) {
                // 최소 4자리 이상의 연속된 부분 문자열이 PIN에 포함되는지 검사
                for (i in 0..info.length - 4) { // 최소 4자리 이상 부분 문자열 추출
                    val substring = info.substring(i, i + 4) // 4자리 부분 문자열
                    if (pin.contains(substring)) {
                        return true // PIN이 개인정보 일부를 포함하면 제한
                    }
                }
            }
        }
        return false // 개인정보가 포함되지 않으면 유효한 PIN
    }
}