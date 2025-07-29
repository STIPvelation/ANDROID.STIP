package com.stip.stip.signup.signup

import android.app.Activity
import android.content.Intent
import androidx.activity.viewModels
import com.stip.stip.R
import com.stip.stip.signup.base.BaseActivity
import com.stip.stip.databinding.ActivitySignUpBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpActivity: BaseActivity<ActivitySignUpBinding, SignUpViewModel>() {

    companion object {
        private const val LOGIN_TO_SIGN_UP_DATA = "LOGIN_TO_SIGN_UP_DATA"
        private const val PHONE_CHANGE_MODE = "PHONE_CHANGE_MODE"
        private const val PIN_CHANGE_MODE = "PIN_CHANGE_MODE" // PIN 변경 모드 추가
        private const val PIN_FORGOT_MODE = "PIN_FORGOT_MODE" // PIN 비밀번호를 잊었을 때 모드

        fun startSignUpActivity(
            activity: Activity,
        ) {
            val intent = Intent(activity, SignUpActivity::class.java).apply {
            }
            activity.startActivity(intent)
        }

        fun startSignUpActivityPinNumberChange(
            activity: Activity,
            isLoginActivity: Boolean,
            isPinForgot: Boolean = false // PIN 잊었을 때 플래그 추가
        ) {
            val intent = Intent(activity, SignUpActivity::class.java)
            intent.putExtra(LOGIN_TO_SIGN_UP_DATA, isLoginActivity)
            intent.putExtra(PIN_CHANGE_MODE, true) // PIN 변경 모드 플래그 설정
            intent.putExtra(PIN_FORGOT_MODE, isPinForgot) // PIN 잊었을 때 플래그 설정
            activity.startActivity(intent)
        }
        
        // 휴대폰 번호 변경을 위한 NICE 인증 시작
        fun startPhoneChangeActivity(
            activity: Activity
        ) {
            val intent = Intent(activity, SignUpActivity::class.java)
            intent.putExtra(PHONE_CHANGE_MODE, true)
            activity.startActivity(intent) 
        }
    }

    override val layoutResource: Int
        get() = R.layout.activity_sign_up

    override val viewModel: SignUpViewModel by viewModels()
    var isLoginActivity = false
    var isPhoneChangeMode = false
    var isPinChangeMode = false
    var isPinForgotMode = false

    override fun initStartView() {
        isLoginActivity = intent.getBooleanExtra(LOGIN_TO_SIGN_UP_DATA, false)
        isPhoneChangeMode = intent.getBooleanExtra(PHONE_CHANGE_MODE, false)
        isPinChangeMode = intent.getBooleanExtra(PIN_CHANGE_MODE, false)
        isPinForgotMode = intent.getBooleanExtra(PIN_FORGOT_MODE, false)
        
        // PIN 번호 변경 모드
        if (isPinChangeMode) {
            if (isPinForgotMode) {
                // PIN 번호를 잊었을 때, 바로 휴대폰 인증 화면으로 이동
                viewModel.setPhoneChangeMode(true) // 휴대폰 인증 모드 설정
                // ViewModel에서 처리되어 NICE 인증 화면으로 자동 이동
            } else {
                // 일반 PIN 변경 모드 - PIN 변경 화면으로 바로 이동
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fcv_section, com.stip.stip.signup.signup.pin.setting.SignUpPinNumberSettingFragment())
                    .commit()
            }
            viewModel.isLoginActivityCheck(isLoginActivity)
        }
        // 휴대폰 번호 변경 모드
        else if (isPhoneChangeMode) {
            // NICE 인증 화면으로 바로 이동하도록 설정
            viewModel.setPhoneChangeMode(true)
            // Navigation Controller에서 바로 NICE 인증 화면으로 이동할 수 있도록 하는 구현 필요
            // 현재는 ViewModel에서 처리
        } else {
            viewModel.isLoginActivityCheck(isLoginActivity)
        }
    }

    override fun initDataBinding() {
        with(viewModel) {
            isLoginLiveData.observe(this@SignUpActivity) {
                // 로그인 화면 진입
            }
        }
    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_sign_up)
//
//        if (intent.getBooleanExtra("SHOW_SIGN_UP_AUTH_NICE", false)) {
//            Log.d("SignUpActivity", "onCreate")
//            supportFragmentManager.beginTransaction()
//                .replace(R.id.fragment_container, SignUpAuthNiceFragment())
//                .commit()
//        }
//    }

    override fun initAfterBinding() {
    }
}