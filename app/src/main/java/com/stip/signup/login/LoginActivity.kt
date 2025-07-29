package com.stip.stip.signup.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import com.skydoves.sandwich.onSuccess
import com.stip.stip.R
import com.stip.stip.MainActivity
import com.stip.stip.databinding.ActivityLoginBinding
import com.stip.stip.signup.base.BaseActivity
import com.stip.stip.signup.Constants
import com.stip.stip.signup.utils.PreferenceUtil
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.stip.stip.signup.signup.SignUpActivity
import com.stip.stip.signup.login.LoginPinNumberActivity

@AndroidEntryPoint
class LoginActivity: BaseActivity<ActivityLoginBinding, LoginViewModel>() {

    companion object {
        private const val PREFS_NAME = "MembershipRestrictionPrefs"
        private const val PREF_KEY_DEVICE_ID = "device_unique_id"
        private const val PREF_KEY_WITHDRAWAL_COUNT = "withdrawal_count_yearly"
        private const val PREF_KEY_LAST_WITHDRAWAL_TIME = "last_withdrawal_time"
        private const val PREF_KEY_PHONE_NUMBERS = "withdrawn_phone_numbers"
        private const val MAX_WITHDRAWALS_PER_YEAR = 3
        private const val REACTIVATION_DELAY_HOURS = 24
        
        fun startLoginActivity(
            activity: Activity,
        ) {
            val intent = Intent(activity, LoginActivity::class.java).apply {
            }
            activity.startActivity(intent)
            activity.finish()
        }
    }

    override val layoutResource: Int
        get() = R.layout.activity_login

    override val viewModel: LoginViewModel by viewModels()

    override fun initStartView() {
        val language = resources.configuration.locales[0].language
        val titleResId = if (language == "ko") {
            R.drawable.ic_login_title
        } else {
            R.drawable.ic_login_english
        }

        binding.tvTitle.setImageResource(titleResId)
        Log.e("start","세상의")

        // 기본 로그인 타입에 따른 자동 이동 기능
        val basicLoginType = PreferenceUtil.getString(Constants.PREF_KEY_BASIC_LOGIN_TYPE, "")
        val storedDi = PreferenceUtil.getString(Constants.PREF_KEY_DI_VALUE, "")
        val storedPin = PreferenceUtil.getString(Constants.PREF_KEY_PIN_VALUE, "")
        
        // DI와 PIN이 모두 있고, 기본 로그인 타입이 설정된 경우 자동 이동
        if (storedDi.isNotBlank() && basicLoginType.isNotBlank()) {
            when (basicLoginType) {
                Constants.BASIC_LOGIN_TYPE_PIN_NUMBER -> {
                    Log.d("LoginActivity", "기본 로그인 타입: PIN 번호 - PIN 로그인 화면으로 이동")
                    LoginPinNumberActivity.startLoginPinNumberActivity(this)
                    finish()
                }
                Constants.BASIC_LOGIN_TYPE_BIOMETRIC_AUTH -> {
                    // 생체인증의 경우 PIN이 있어야 함
                    if (storedPin.isNotBlank()) {
                        Log.d("LoginActivity", "기본 로그인 타입: 생체인증 - PIN 있음 - 생체인증 로그인 화면으로 이동")
                        LoginBiometricAuthActivity.startLoginBiometricAuthActivity(this)
                        finish()
                    } else {
                        Log.d("LoginActivity", "기본 로그인 타입: 생체인증 - PIN 없음 - PIN 로그인 화면으로 이동")
                        LoginPinNumberActivity.startLoginPinNumberActivity(this)
                        finish()
                    }
                }
            }
        }
    }

    override fun initDataBinding() {
    }
    
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 회원탈퇴 후 메시지 표시
        if (intent.getBooleanExtra("show_withdrawal_message", false)) {
            val withdrawalTime = intent.getLongExtra("withdrawal_time", 0L)
            if (withdrawalTime > 0) {
                showWithdrawalMessage(withdrawalTime)
            }
        }
    }

    private fun showWithdrawalMessage(withdrawalTime: Long) {
        // 추가 다이얼로그 표시 없이 전체 기능 유지
    }
    
    /**
     * 회원탈퇴 제한 조건을 확인하는 함수
     * - 24시간 내 재가입 불가
     * - 1년에 3회 이상 탈퇴 불가
     */
    private fun checkWithdrawalRestrictions(): Boolean {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentTime = System.currentTimeMillis()
        
        // 기기 ID 확인
        var deviceId = sharedPrefs.getString(PREF_KEY_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            sharedPrefs.edit().putString(PREF_KEY_DEVICE_ID, deviceId).apply()
        }
        
        // 1. 마지막 탈퇴 시간 확인 (24시간 제한)
        val lastWithdrawalTime = sharedPrefs.getLong(PREF_KEY_LAST_WITHDRAWAL_TIME, 0)
        if (lastWithdrawalTime > 0) {
            val elapsedHours = TimeUnit.MILLISECONDS.toHours(currentTime - lastWithdrawalTime)
            if (elapsedHours < REACTIVATION_DELAY_HOURS) {
                // 24시간 제한 적용 (메시지 없이)
                return false
            }
        }
        
        // 2. 연간 탈퇴 횟수 확인
        val yearlyWithdrawalCount = sharedPrefs.getInt(PREF_KEY_WITHDRAWAL_COUNT, 0)
        if (yearlyWithdrawalCount >= MAX_WITHDRAWALS_PER_YEAR) {
            // 재가입 시도 차단 (메시지 없이)
            return false
        }
        
        return true
    }

    override fun initAfterBinding() {
        // 둘러보기 클릭 리스너
        setOnClick(binding.tvLookAround) {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // STIP 시작하기 통합 인증 버튼 클릭 리스너 (나이스 본인인증 기반)
        setOnClick(binding.btnLogin) {
            Log.d("LoginActivity", "STIP 시작하기 버튼 클릭")
            // 저장된 DI 값 확인
            val storedDi = PreferenceUtil.getString(Constants.PREF_KEY_DI_VALUE, "")
            if (storedDi.isNotBlank()) {
                // DI가 있으면 회원 정보 조회
                CoroutineScope(Dispatchers.Main).launch {
                    val memberInfoResponse = viewModel.getMemberInfo()
                    var isMember = false
                    memberInfoResponse.onSuccess {
                        isMember = true
                    }
                    if (isMember) {
                        // 회원가입이 완전히 끝난 회원 → PIN 로그인 화면 이동
                        val intent = Intent(this@LoginActivity, LoginPinNumberActivity::class.java).apply {
                            putExtra("di_value", storedDi)
                        }
                        startActivity(intent)
                    } else {
                        // 회원가입 미완료 → 회원가입 화면 이동
                        val intent = Intent(this@LoginActivity, SignUpActivity::class.java)
                        startActivity(intent)
                    }
                }
            } else {
                // DI가 없으면 본인인증 진행
                Log.d("LoginActivity", "DI 없음: 나이스 본인인증 시작")
                // 탈퇴 제한 기간/횟수 확인
                if (checkWithdrawalRestrictions()) {
                    // 나이스 본인인증 요청
                    requestNiceIdentityVerification()
                } else {
                    Log.d("LoginActivity", "회원가입 제한 조건 걸림")
                    // 제한 조건에 걸리면 Toast 메시지를 표시
                    showWithdrawalRestrictionMessage()
                }
            }
        }


    }
    
    /**
     * 나이스 본인인증을 요청하는 함수
     * 본인인증 성공 시 handleNiceAuthResult()에서 결과 처리
     */
    private fun requestNiceIdentityVerification() {
        android.util.Log.d("LoginActivity", "나이스 본인인증 요청")
        val intent = Intent(this, com.stip.stip.signup.signup.SignUpActivity::class.java)
        startActivity(intent)
    }
    
    /**
     * 나이스 본인인증 결과 처리 함수
     * 본인인증 성공 시 DI 값으로 회원 여부를 판단하여 로그인 또는 회원가입 진행
     */
    private fun handleNiceAuthResult(success: Boolean, di: String?, ci: String?) {
        if (!success || di.isNullOrEmpty()) {
            android.util.Log.e("LoginActivity", "본인인증 실패 또는 DI 값이 없음")
            Toast.makeText(this, "본인인증에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_LONG).show()
            return
        }
        
        android.util.Log.d("LoginActivity", "본인인증 성공: DI=$di")
        
        // TODO: DI 값으로 서버에 회원 여부 확인 API 호출 필요
        checkUserExistenceByDi(di, ci)
    }
    
    /**
     * DI 값으로 서버에 회원 여부 확인 요청
     * 서버에서 DI 값을 기준으로 회원 DB 조회 결과를 받아 처리
     */
    private fun checkUserExistenceByDi(di: String, ci: String?) {
        android.util.Log.d("LoginActivity", "DI 값으로 회원 여부 확인 요청")
        
        // 저장된 DI 값과 비교하여 기존 회원인지 확인
        val storedDi = PreferenceUtil.getString(Constants.PREF_KEY_DI_VALUE, "")
        val storedPin = PreferenceUtil.getString(Constants.PREF_KEY_PIN_VALUE, "")
        
        // 저장된 DI가 있고, 현재 입력한 DI와 일치하면서 PIN이 설정된 경우에만 기존 회원으로 판단
        // 회원가입이 완전히 완료된 사용자만 로그인 플로우로 진입
        val userExists = storedDi.isNotBlank() && di == storedDi && storedPin.isNotBlank()
        
        if (userExists) {
            // DI 값이 DB에 존재 (기존 회원) → 로그인 프로세스
            // 새로운 DI 값을 저장 (갱신)
            if (di != storedDi && di.isNotBlank()) {
                PreferenceUtil.putString(Constants.PREF_KEY_DI_VALUE, di)
            }
            routeToExistingUserFlow(di)
        } else {
            // DI 값이 DB에 없음 (신규 회원) → 회원가입 프로세스
            if (di.isNotBlank()) {
                PreferenceUtil.putString(Constants.PREF_KEY_DI_VALUE, di)
            }
            routeToNewUserFlow(di, ci)
        }
    }
    
    /**
     * 기존 회원 처리 - 로그인 프로세스로 라우팅
     */
    private fun routeToExistingUserFlow(di: String) {
        android.util.Log.d("LoginActivity", "기존 회원 확인됨: 로그인 화면(PIN 입력)으로 이동")
        
        // DI 값을 함께 전달하여 PIN 번호 입력 화면으로 이동
        val intent = Intent(this, LoginPinNumberActivity::class.java).apply {
            putExtra("di_value", di) // PIN 인증 후 서버 로그인에 필요할 수 있음
        }
        startActivity(intent)
    }
    
    /**
     * 신규 회원 처리 - 회원가입 프로세스로 라우팅
     */
    private fun routeToNewUserFlow(di: String, ci: String?) {
        android.util.Log.d("LoginActivity", "신규 회원 확인됨: 회원가입 프로세스 시작")
        Toast.makeText(this, "STIP 신규 회원가입을 시작합니다.", Toast.LENGTH_SHORT).show()
        
        // DI/CI 값을 함께 전달하여 회원가입 화면으로 이동
        val intent = Intent(this, com.stip.stip.signup.signup.SignUpActivity::class.java).apply {
            putExtra("di_value", di)
            putExtra("ci_value", ci)
        }
        startActivity(intent)
    }
    
    /**
     * 회원 탈퇴 제한 메시지 표시 함수
     */
    private fun showWithdrawalRestrictionMessage() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastWithdrawalTime = sharedPrefs.getLong(PREF_KEY_LAST_WITHDRAWAL_TIME, 0)
        val yearlyWithdrawalCount = sharedPrefs.getInt(PREF_KEY_WITHDRAWAL_COUNT, 0)
        
        if (lastWithdrawalTime > 0) {
            val currentTime = System.currentTimeMillis()
            val elapsedHours = TimeUnit.MILLISECONDS.toHours(currentTime - lastWithdrawalTime)
            if (elapsedHours < REACTIVATION_DELAY_HOURS) {
                Toast.makeText(this, "탈퇴 후 ${REACTIVATION_DELAY_HOURS}시간 동안 회원가입이 불가합니다.", Toast.LENGTH_LONG).show()
                return
            }
        }
        
        if (yearlyWithdrawalCount >= MAX_WITHDRAWALS_PER_YEAR) {
            Toast.makeText(this, "연간 탈퇴 횟수(${MAX_WITHDRAWALS_PER_YEAR}회)를 초과하여 회원가입이 불가합니다.", Toast.LENGTH_LONG).show()
            return
        }
        
        // 나머지 경우에는 일반적인 오류 메시지 표시
        Toast.makeText(this, "회원가입 처리 중 오류가 발생했습니다. 다시 시도해주세요.", Toast.LENGTH_LONG).show()
    }
}
