package com.stip.stip.signup.intro

import android.util.Log
import androidx.activity.viewModels
import com.skydoves.sandwich.onSuccess
import com.stip.stip.signup.Constants
import com.stip.stip.signup.Constants.TAG
import com.stip.stip.R
import com.stip.stip.signup.base.BaseActivity
import com.stip.stip.databinding.ActivityIntroBinding
import com.stip.stip.signup.login.LoginActivity
import com.stip.stip.signup.permission.PermissionActivity
import com.stip.stip.signup.utils.PreferenceUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class IntroActivity: BaseActivity<ActivityIntroBinding, IntroViewModel>() {

    override val layoutResource: Int
        get() = R.layout.activity_intro

    override val viewModel: IntroViewModel by viewModels()

    override fun initStartView() {
        Log.e(TAG,"token = ${PreferenceUtil.getString(Constants.PREF_KEY_AUTH_TOKEN_VALUE)}")
        Log.e(TAG,"di = ${PreferenceUtil.getString(Constants.PREF_KEY_DI_VALUE)}")
        Log.e(TAG,"pin = ${PreferenceUtil.getString(Constants.PREF_KEY_PIN_VALUE)}")

        val di = PreferenceUtil.getString(Constants.PREF_KEY_DI_VALUE)
        val pin = PreferenceUtil.getString(Constants.PREF_KEY_PIN_VALUE)

        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)

            if (di.isNotBlank()) {
                // 회원 정보 조회
                val memberInfoResponse = viewModel.getMemberInfo()
                var isMember = false
                memberInfoResponse.onSuccess {
                    isMember = true
                }
                if (isMember) {
                    // 회원가입이 완전히 끝난 회원 → PIN 로그인 화면 이동
                    com.stip.stip.signup.login.LoginPinNumberActivity.startLoginPinNumberActivity(this@IntroActivity)
                    finish()
                } else {
                    // 회원가입 미완료 → 로그인 화면으로 이동
                    LoginActivity.startLoginActivity(this@IntroActivity)
                    finish()
                }
            } else {
                // Check if permission screen has been shown before
                val permissionShown = PreferenceUtil.getBoolean("PREF_KEY_PERMISSION_SHOWN", false)

                if (permissionShown) {
                    // Skip permission screen if already shown
                    LoginActivity.startLoginActivity(this@IntroActivity)
                } else {
                    // Show permission screen for first time
                    PermissionActivity.startPermissionActivity(this@IntroActivity)
                }
            }
        }
    }


    override fun initDataBinding() {
    }

    // 이 부분 추가!
    override fun initAfterBinding() {
    }
}