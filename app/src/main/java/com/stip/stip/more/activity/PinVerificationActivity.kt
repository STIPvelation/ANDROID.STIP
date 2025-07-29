package com.stip.stip.more.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.stip.stip.R
import com.stip.stip.databinding.FragmentSignUpPinNumberFinishBinding
import com.stip.stip.more.fragment.PinIncorrectDialogFragment
import com.stip.stip.more.fragment.ForgetPinNumberDialogFragment
import com.stip.stip.signup.Constants
import com.stip.stip.signup.utils.PreferenceUtil
import com.stip.stip.signup.model.RequestPinNumber
import com.stip.stip.signup.api.repository.member.MemberRepository
import com.stip.stip.signup.customview.LoadingDialog
import com.stip.stip.signup.keypad.KeypadAdapter
import com.stip.stip.signup.keypad.KeypadItem
import com.stip.stip.signup.keypad.KeypadType
import com.stip.stip.signup.pin.PinAdapter
import com.stip.stip.signup.customview.CustomContentDialog
import com.skydoves.sandwich.suspendOnError
import com.skydoves.sandwich.suspendOnException
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class PinVerificationActivity : AppCompatActivity() {

    @Inject
    lateinit var memberRepository: MemberRepository

    private lateinit var binding: FragmentSignUpPinNumberFinishBinding
    private lateinit var pinAdapter: PinAdapter
    private lateinit var keypadAdapter: KeypadAdapter

    private var currentPin = ""
    private val validPin: String
        get() = PreferenceUtil.getString(Constants.PREF_KEY_PIN_VALUE, "") // 저장된 PIN 가져오기
    private var attemptCount = 0
    private val maxAttempts = 5
    private var isPinChange = false // PIN 변경 목적인지 표시
    private var isSettingNewPin = false // 새로운 PIN 설정 중인지 표시
    private var isBiometricSetup = false // 생체인증 설정 목적인지 표시 플래그

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentSignUpPinNumberFinishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 플래그 값 가져오기
        isPinChange = intent.getBooleanExtra("is_pin_change", false)
        isSettingNewPin = intent.getBooleanExtra("is_setting_new_pin", false)
        isBiometricSetup = intent.getBooleanExtra("is_biometric_setup", false)

        // 헤더 설정
        binding.ivBack.setOnClickListener {
            finish()
        }

        // 목적에 따른 제목 및 안내 메시지 설정
        if (isSettingNewPin) {
            binding.tvSignUpPinNumberFinishTitle.text = "새 PIN 번호 입력"
            binding.tvPinNumberGuide.text = "새롭게 사용할 6자리 PIN 번호를 입력해 주세요."
        } else if (isPinChange) {
            binding.tvSignUpPinNumberFinishTitle.text = "PIN 번호 변경"
            binding.tvPinNumberGuide.text = "현재 사용중인 PIN 번호를 입력해 주세요."
            binding.tvPinNumberGuide.visibility = View.VISIBLE
            binding.tvPinNumberGuide.text = "PIN 번호가 기억나지 않으신가요?"
        } else if (isBiometricSetup) {
            binding.tvSignUpPinNumberFinishTitle.text = "생체인증 설정"
            binding.tvPinNumberGuide.text = "PIN 번호를 입력하고 생체인증 등록을 진행해 주세요."
        } else {
            binding.tvSignUpPinNumberFinishTitle.text = "PIN 번호 입력"
            binding.tvPinNumberGuide.text = "6자리 PIN 번호를 입력해 주세요."
            // 스킵 버튼은 이 레이아웃에 존재하지 않으므로 제거
        }

        // PIN 어댑터 설정
        pinAdapter = PinAdapter(mutableListOf(true, true, true, true, true, true))
        binding.rvSignUpPinNumberPassword.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvSignUpPinNumberPassword.adapter = pinAdapter

        // 키패드 어댑터 설정
        setupKeypad()

        // PIN 번호 잊음 다이얼로그 설정 (PIN 변경 화면에서만 표시)
        if (isPinChange) {
            binding.tvPinNumberGuide.setOnClickListener {
                val dialog = ForgetPinNumberDialogFragment(
                    onConfirm = { 
                        // Handle confirmation - e.g., navigate to authentication screen
                        val intent = Intent(this@PinVerificationActivity, PinVerificationActivity::class.java)
                        intent.putExtra("is_setting_new_pin", true)
                        startActivity(intent)
                    },
                    onCancel = {
                        // Handle cancellation (do nothing or add specific behavior)
                    }
                )
                dialog.show(supportFragmentManager, "ForgetPinNumberDialog")
            }
        }
    }

    private fun setupKeypad() {
        val keypadItems = mutableListOf<KeypadItem>()
        // 1~9 숫자 키패드 아이템 추가
        for (i in 1..9) {
            keypadItems.add(KeypadItem(i.toString(), KeypadType.NUMBER))
        }

        // 지문, 0, 삭제 아이템 추가
        keypadItems.add(KeypadItem("", KeypadType.NUMBER)) // 왼쪽 하단 빈칸 (지문인식 사용하지 않음)
        keypadItems.add(KeypadItem("0", KeypadType.NUMBER))
        keypadItems.add(KeypadItem("", KeypadType.DELETE))

        // 키패드 어댑터 설정
        keypadAdapter = KeypadAdapter(keypadItems.toMutableList()) { keypadItem ->
            when (keypadItem.type) {
                KeypadType.NUMBER -> {
                    if (currentPin.length < 6) {
                        // 현재 PIN에 숫자 추가
                        currentPin += keypadItem.value
                        // 입력된 PIN 개수에 따라 UI 업데이트
                        pinAdapter.updatePinCount(currentPin.length)

                        // PIN 6자리 모두 입력 완료 시 검증 시작
                        if (currentPin.length == 6) {
                            verifyPin()
                        }
                    }
                }
                KeypadType.DELETE -> {
                    if (currentPin.isNotEmpty()) {
                        // 마지막 숫자 제거
                        currentPin = currentPin.substring(0, currentPin.length - 1)
                        // UI 업데이트
                        pinAdapter.updatePinCount(currentPin.length)
                    }
                }
                else -> {
                    // 지문인식 등은 현재 사용하지 않음
                }
            }
        }
        // KeypadAdapter now uses lambda passed in the constructor for click events

        // 키패드 표시
        binding.rvSignUpPinNumberKeypad.layoutManager = GridLayoutManager(this, 3)
        binding.rvSignUpPinNumberKeypad.adapter = keypadAdapter
    }

    private fun verifyPin() {
        if (isPinChange) {
            // PIN 변경 목적일 경우 새 PIN 입력 화면으로 이동
            val intent = Intent(this@PinVerificationActivity, PinVerificationActivity::class.java)
            intent.putExtra("is_setting_new_pin", true)
            startActivity(intent)
            finish()
            return
        } else if (isBiometricSetup) {
            PreferenceUtil.putString(Constants.PREF_KEY_PIN_VALUE, currentPin)
            val sharedPrefBio = getSharedPreferences("security_pref", android.content.Context.MODE_PRIVATE)
            sharedPrefBio.edit().putBoolean("biometric_enabled", true).apply()
            Toast.makeText(this@PinVerificationActivity, "생체인증이 활성화되었습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // di 값 가져오기 - 실제로는 로그인한 회원의 di 값을 사용해야 함
        val memberDi = PreferenceUtil.getString(Constants.PREF_KEY_DI_VALUE, "")
        if (memberDi.isEmpty()) {
            Toast.makeText(this@PinVerificationActivity, "로그인 정보가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // PIN 번호 암호화 및 요청 객체 생성
        val requestPinNumber = RequestPinNumber(
            pin = currentPin // 실제로는 암호화된 PIN 사용 필요
        )

        // 로딩 다이얼로그 표시
        val loadingDialog = LoadingDialog(this@PinVerificationActivity)
        loadingDialog.setCancelable(false)
        loadingDialog.show()

        lifecycleScope.launch {

            val response = memberRepository.verifyMemberPin(memberDi, requestPinNumber)
            response.suspendOnSuccess {
                // PIN 유효성 검사 성공 - PIN 일치
                runOnUiThread {
                    // 성공 처리
                    android.util.Log.d("PinVerification", "PIN 일치 확인됨")
                    // 시도 횟수 초기화
                    attemptCount = 0

                    // 새 PIN 설정 목적일 경우 PIN 저장
                    if (isSettingNewPin) {
                        PreferenceUtil.putString(Constants.PREF_KEY_PIN_VALUE, currentPin)
                        android.util.Log.d("PinVerification", "새 PIN 저장 완료")

                        // 로딩 다이얼로그 닫기
                        loadingDialog.dismiss()

                        // 성공 메시지 표시 후 종료
                        CustomContentDialog(this@PinVerificationActivity) { _ ->
                            finish()
                        }.setText("성공", "PIN 번호가 변경되었습니다.", "", "확인", false)
                        finish()
                    } else if (isBiometricSetup) {
                        // 생체인증 설정 목적일 경우 로딩 다이얼로그 표시
                        val biometricLoadingDialog = LoadingDialog(this@PinVerificationActivity)
                        biometricLoadingDialog.setCancelable(false)
                        biometricLoadingDialog.show()

                        // 2초 후에 생체인증 활성화 처리
                        Handler(Looper.getMainLooper()).postDelayed({
                            // 생체인증 설정 활성화
                            val sharedPrefBio = getSharedPreferences("security_pref", android.content.Context.MODE_PRIVATE)
                            sharedPrefBio.edit().putBoolean("biometric_enabled", true).apply()

                            // 로딩 다이얼로그 닫기
                            biometricLoadingDialog.dismiss()

                            // 완료 메시지 표시
                            Toast.makeText(this@PinVerificationActivity, "생체인증 정보 사용이 활성화되었습니다.", Toast.LENGTH_SHORT).show()

                            // 원래 화면으로 돌아가기 위해 활동 종료
                            finish()
                        }, 2000) // 2초 후 실행
                    } else {
                        // 일반 접근 목적일 경우 회원 정보 화면으로 이동
                        // 로딩 다이얼로그 닫기
                        loadingDialog.dismiss()

                        // 인증 성공 결과 전달
                        setResult(RESULT_OK)
                        finish()
                    }
                }
            }.suspendOnError {
                // PIN 유효성 검사 실패 - PIN 불일치
                android.util.Log.e("LoginPinNumber", "PIN 번호 불일치 발생!!!파파")
                runOnUiThread {
                    android.util.Log.e("LoginPinNumber", "PIN 번호 불일치 발생!!!포포")
                    // 로딩 다이얼로그 닫기
                    loadingDialog.dismiss()
                    attemptCount++
                    currentPin = ""
                    pinAdapter.updatePinCount(0)

                    // 오류 메시지 표시
                    val warningText = "다시 입력 해주세요.(" + attemptCount + "/" + maxAttempts + ")"
                    binding.tvSignUpPinNumberWarning.text = warningText
                    binding.tvSignUpPinNumberWarning.setTextColor(ContextCompat.getColor(this@PinVerificationActivity, R.color.red_DB3949_100))
                    binding.tvSignUpPinNumberWarning.visibility = View.VISIBLE

                    // PIN 변경 시에만 안내 메시지 표시 중이었다면 숨김
                    if (isPinChange) {
                        binding.tvPinNumberGuide.visibility = View.GONE
                    }

                    // 5번 이상 틀렸을 경우 경고 다이얼로그 표시
                    if (attemptCount >= maxAttempts) {
                        val dialog = PinIncorrectDialogFragment()
                        dialog.show(supportFragmentManager, "PinIncorrectDialog")
                    }
                }
            }.suspendOnException {
                // 예외 발생 - 네트워크 오류 등
                runOnUiThread {
                    // 로딩 다이얼로그 닫기
                    loadingDialog.dismiss()
                    Toast.makeText(this@PinVerificationActivity, "서버 연결 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    currentPin = ""
                    pinAdapter.updatePinCount(0)
                }
            }
        }
    }

    /**
     * PIN 번호가 연속된 숫자(123456 등)이거나 동일한 숫자의 반복(111111 등)인지 확인
     * @param pin 6자리 PIN 번호
     * @return 연속되거나 반복되는 경우 true, 아닌 경우 false
     */
    private fun isSequentialOrRepeated(pin: String): Boolean {
        // 입력된 PIN이 6자리가 아니면 유효하지 않음
        if (pin.length != 6) return false
        android.util.Log.e("PinVerification", "PIN 번호 불일치 발생!!!쓰쓰")
        // 동일한 숫자의 반복 검사 (모든 숫자가 같은 경우)
        val distinctDigits = pin.toCharArray().distinct()
        if (distinctDigits.size == 1) return true

        // 연속된 숫자 검사
        var isSequential = true
        for (i in 1 until pin.length) {
            val prev = pin[i - 1].digitToInt()
            val curr = pin[i].digitToInt()

            // 이전 숫자 + 1이 현재 숫자가 아니면 연속되지 않은 것
            if (prev + 1 != curr) {
                isSequential = false
                break
            }
        }

        // 연속된 숫자가 아니면 역순으로 연속된 숫자인지 확인 (654321 등)
        if (!isSequential) {
            isSequential = true
            for (i in 1 until pin.length) {
                val prev = pin[i - 1].digitToInt()
                val curr = pin[i].digitToInt()

                // 이전 숫자 - 1이 현재 숫자가 아니면 연속되지 않은 것
                if (prev - 1 != curr) {
                    isSequential = false
                    break
                }
            }
        }

        return isSequential
    }
}
