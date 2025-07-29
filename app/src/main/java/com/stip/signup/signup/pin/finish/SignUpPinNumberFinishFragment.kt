package com.stip.stip.signup.signup.pin.finish

import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.stip.stip.MainActivity
import com.stip.stip.R
import com.stip.stip.databinding.FragmentSignUpPinNumberFinishBinding
import com.stip.stip.signup.Constants
import com.stip.stip.signup.base.BaseFragment
import com.stip.stip.signup.customview.CustomContentDialog
import com.stip.stip.signup.keypad.KeypadAdapter
import com.stip.stip.signup.keypad.KeypadItem
import com.stip.stip.signup.keypad.KeypadItemDecoration
import com.stip.stip.signup.keypad.KeypadType
import com.stip.stip.signup.model.RequestAuthLogin
import com.stip.stip.signup.pin.PinAdapter
import com.stip.stip.signup.utils.PreferenceUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpPinNumberFinishFragment : BaseFragment<FragmentSignUpPinNumberFinishBinding, SignUpPinNumberFinishViewModel>() {
    override val layoutResource: Int
        get() = R.layout.fragment_sign_up_pin_number_finish

    override val viewModel: SignUpPinNumberFinishViewModel by viewModels()

    private lateinit var pinAdapter: PinAdapter
    private lateinit var keypadAdapter: KeypadAdapter
    private val pinInput = StringBuilder()

    private var failedAttempts = 0 // 실패 횟수 저장

    override fun initStartView() {
        resetPinInput()
        setupKeyPad()
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
                }.setText(
                    getString(R.string.dialog_bank_guide_title),
                    errorMessage,
                    "",
                    getString(R.string.common_confirm)
                )
            }

            authLoginLiveData.observe(viewLifecycleOwner) {
                val safeContext = activity ?: return@observe
                MainActivity.startMainActivity(safeContext)
                activity?.finish()
            }

            memberDeleteLiveData.observe(viewLifecycleOwner) {
                if (it) {
                    // 데이터 초기화 후 다시 가입 시작
                    PreferenceUtil.clear()
                    activity?.finish()
                }
            }
        }
    }

    override fun initAfterBinding() {
        setOnClick(binding.ivBack) {
            findNavController().navigateUp()
        }

        setOnClick(binding.tvLoginPinNumberForgetPassword) {
            CustomContentDialog(
                binding.root.context
            ) {
                // 삭제 API 호출
                viewModel.requestDeleteMembers(
                    PreferenceUtil.getString(Constants.PREF_KEY_DI_VALUE),
                    pinInput.toString()
                )
            }.setText(
                getString(R.string.dialog_pin_number_title),
                getString(R.string.dialog_pin_number_finish_reset),
                getString(R.string.common_cancel),
                getString(R.string.common_confirm),
                false
            )
        }
    }

    /** 키패드 설정 */
    private fun setupKeyPad() {
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
                    // 완료 버튼 처리 - 키패드 숨김
                    binding.rvSignUpPinNumberKeypad.visibility = View.GONE
                }
                KeypadType.SHUFFLE -> {} // 사용하지 않음
            }
        }
        binding.rvSignUpPinNumberKeypad.adapter = keypadAdapter
        binding.rvSignUpPinNumberKeypad.addItemDecoration(KeypadItemDecoration())
    }

    /** 숫자 입력 처리 */
    private fun handleNumberInput(value: String) {
        if (pinInput.length < 6) {
            pinInput.append(value)
            pinAdapter.updatePinCount(pinInput.length)

            // 6자리 입력 완료 시 처리
            if (pinInput.length == 6) {
                validatePin()
            }
        }
    }

    /** 입력한 PIN 검증 */
    private fun validatePin() {
        if (pinInput.toString() == PreferenceUtil.getString(Constants.PREF_KEY_PIN_VALUE)) {
            onPinCheckSuccess()
        } else {
            onPinCheckFailure()
        }
    }

    /** PIN 확인 성공 */
    private fun onPinCheckSuccess() {
        // 기본 로그인 타입을 PIN 번호로 설정 (생체인증을 선택하지 않은 경우)
        val basicLoginType = PreferenceUtil.getString(Constants.PREF_KEY_BASIC_LOGIN_TYPE, "")
        if (basicLoginType.isBlank()) {
            PreferenceUtil.putString(Constants.PREF_KEY_BASIC_LOGIN_TYPE, Constants.BASIC_LOGIN_TYPE_PIN_NUMBER)
            Log.d("SignUpPinFinish", "기본 로그인 타입을 PIN 번호로 설정")
        }
        
        viewModel.requestPostAuthLogin(
            RequestAuthLogin(
                di = PreferenceUtil.getString(Constants.PREF_KEY_DI_VALUE),
                pin = pinInput.toString()
            )
        )
    }

    /** PIN 확인 실패 */
    private fun onPinCheckFailure() {
        failedAttempts++

        binding.tvSignUpPinNumberWarning.visibility = View.VISIBLE
        if (failedAttempts >= 5) {
            CustomContentDialog(
                binding.root.context
            ) {
                // 데이터 초기화 후 다시 가입 시작
                PreferenceUtil.clear()
                activity?.finish()
            }.setText(
                getString(R.string.dialog_pin_number_title),
                getString(R.string.dialog_pin_number_finish_reset),
                getString(R.string.common_cancel),
                getString(R.string.common_confirm),
                false
            )
        } else {
            binding.tvSignUpPinNumberWarning.text = getString(R.string.sign_up_pin_number_setting_confirm_not_correct, failedAttempts)
        }

        resetPinInput()
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
}