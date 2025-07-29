package com.stip.stip.signup.signup.bank.deposit

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.stip.stip.signup.Constants
import com.stip.stip.R
import com.stip.stip.signup.base.BaseFragment
import com.stip.stip.signup.customview.CustomContentDialog
import com.stip.stip.databinding.FragmentSignUpBankDepositBinding
import com.stip.stip.signup.keypad.KeypadAdapter
import com.stip.stip.signup.keypad.KeypadItem
import com.stip.stip.signup.keypad.KeypadType
import com.stip.stip.signup.model.RequestAccountOTP
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpBankDepositFragment: BaseFragment<FragmentSignUpBankDepositBinding, SignUpBankDepositViewModel>() {

    override val layoutResource: Int
        get() = R.layout.fragment_sign_up_bank_deposit

    override val viewModel: SignUpBankDepositViewModel by viewModels()

    private lateinit var wonAdapter: WonAdapter
    private lateinit var keypadAdapter: KeypadAdapter
    private var number = ""
    private var requestNo: String? = null
    private var responseUniqueId: String? = null
    private var failCount = 0

    override fun initStartView() {
        val accountName = arguments?.getString(Constants.BUNDLE_ACCOUNT_NAME_KEY)
        val accountNumber = arguments?.getString(Constants.BUNDLE_ACCOUNT_NUMBER_KEY)
        accountName?.let {
            accountNumber?.let {
                // 선택된 은행 정보를 UI에 반영
                binding.tvSignUpBankNumber.text = "$accountName $accountNumber"
            }
        }
        requestNo = arguments?.getString(Constants.BUNDLE_REQUEST_NO_KEY)
        responseUniqueId = arguments?.getString(Constants.BUNDLE_REQUEST_RESPONSE_UNIQUE_ID_KEY)

        val result = String.format(binding.root.context.getString(R.string.sign_up_bank_1_won_title))
        binding.tvSignUpBank1WonTitle.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(result, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(result)
        }

        wonAdapter = WonAdapter() {
            binding.rvNumber.visibility = View.VISIBLE
            binding.nsvBodySection.post {
                binding.nsvBodySection.smoothScrollTo(0, binding.nsvBodySection.bottom)
            }
        }
        binding.rvSignUpVerificationCode.adapter = wonAdapter
        binding.rvSignUpVerificationCode.addItemDecoration(WonItemDecoration(binding.root.context))
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

            signUpBankDepositNumberCheckLiveData.observe(viewLifecycleOwner) {
                if (it) {
                    binding.btnAuth.isEnabled = true
                } else {
                    binding.btnAuth.isEnabled = false
                }
            }

            timeData.observe(viewLifecycleOwner) {
                binding.btnAuth.text = getString(R.string.sign_up_bank_1_won_auth, it)

                /*
                if(it == "00:00"){
                    CustomContentDialog(
                        binding.root.context
                    ) {
                        findNavController().navigateUp()
                    }.setText(
                        getString(R.string.dialog_bank_guide_title),
                        getString(R.string.dialog_bank_deposit_fail_time_out),
                        "",
                        getString(R.string.common_confirm),
                        true
                    )
                    viewModel.saveTimerReset()
                }
                 */
            }

            signUpOTPNumberLiveData.observe(viewLifecycleOwner) {
                if (it) {
                    try {
                        // 안전하게 네비게이션 처리
                        if (isAdded && !isRemoving && !isDetached && view != null) {
                            val navController = findNavController()
                            // 현재 대상이 올바른지 확인
                            if (navController.currentDestination?.id == R.id.SignUpBankDepositFragment) {
                                // 지연을 주어 UI 갱신이 완료된 후 네비게이션 수행
                                binding.root.post {
                                    navController.navigate(R.id.action_SignUpBankDepositFragment_to_SignUpKYCFragment)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    failCount += 1

                    if (failCount == 3) {
                        // 실패 횟수 3회
                        CustomContentDialog(
                            binding.root.context
                        ) {
                            activity?.finish()
                        }.setText(
                            getString(R.string.dialog_bank_deposit_fail_title),
                            getString(R.string.dialog_bank_deposit_three_fail_content),
                            "",
                            getString(R.string.common_confirm),
                            false
                        )
                    } else {
                        // 실패 횟수 3회 미만
                        val result = binding.root.context.getString(R.string.dialog_bank_deposit_fail_content, failCount)
                        CustomContentDialog(
                            binding.root.context
                        ) {
                        }.setText(
                            getString(R.string.dialog_bank_deposit_fail_title),
                            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                Html.fromHtml(result, Html.FROM_HTML_MODE_LEGACY)
                            } else {
                                @Suppress("DEPRECATION")
                                Html.fromHtml(result)
                            }).toString(),
                            "",
                            getString(R.string.common_confirm),
                            false
                        )
                    }
                }
            }
        }
    }

    override fun initAfterBinding() {
        setOnClick(binding.tvSignUpBank1WonHelp) {
            CustomContentDialog(
                binding.root.context
            ) {
            }.setText(
                getString(R.string.dialog_bank_guide_title),
                getString(R.string.dialog_bank_1_help_content),
                "",
                getString(R.string.common_confirm),
                true
            )
        }

        setOnClick(binding.clSignUpBankNumberSection) {
            findNavController().navigate(
                R.id.action_SignUpBankDepositFragment_to_SignUpBankSelectFragment,
                null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.SignUpBankSelectFragment, true) // 기존 B 제거
                    .setLaunchSingleTop(true) // 현재 대상이 스택에 있으면 새로 추가하지 않음
                    .build()
            )
        }

        setOnClick(binding.btnAuth) {
            requestNo?.let { requestNo ->
                responseUniqueId?.let { responseUniqueId ->
                    viewModel.requestOTPNumber(
                        RequestAccountOTP(
                            number,
                            requestNo,
                            responseUniqueId
                        )
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.timerStart()  // 포그라운드로 전환 시 타이머 재개
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveTimerState() // 백그라운드로 전환 시 타이머 상태 저장
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().window.setDecorFitsSystemWindows(true)
        } else {
            @Suppress("DEPRECATION")
            requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        }
    }

    override fun onDestroy() {
        viewModel.saveTimerReset()
        super.onDestroy()
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
                    if (number.length < 6) {
                        number += item.value
                    }
                }
                KeypadType.DONE -> {
                    // 완료 버튼 처리 - 키패드 숨김 처리
                    binding.rvNumber.visibility = View.GONE
                }
                KeypadType.DELETE -> {
                    if (number.isNotEmpty()) {
                        number = number.dropLast(1)
                    }
                }
                KeypadType.SHUFFLE -> {}
            }
            wonAdapter.updateNumber(number)
            viewModel.isBankDepositNumberCheck(number)
        }
        binding.rvNumber.adapter = keypadAdapter
    }
}