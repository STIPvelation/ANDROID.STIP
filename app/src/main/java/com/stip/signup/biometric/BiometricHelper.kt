package com.stip.stip.signup.biometric

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.stip.stip.signup.getFragmentActivity
import java.util.concurrent.Executor

class BiometricAuthHelper(private val context: Context, private val callback: AuthCallback) {

    interface AuthCallback {
        fun onAuthSuccess() // 인증 성공
        fun onAuthFailed() // 인증 실패
        fun onAuthError(errorMsg: String) // 인증 오류 발생
    }

    // 생체 인증 가능 여부 확인 (지문 + 얼굴)
    private fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(context, "기기가 생체 인증을 지원하지 않습니다.", Toast.LENGTH_SHORT).show()
                false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(context, "현재 생체 인증을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
                false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(context, "등록된 지문 또는 얼굴 정보가 없습니다.", Toast.LENGTH_LONG).show()
                false
            }
            else -> false
        }
    }

    // 생체 인증 실행 (지문 & 얼굴 자동 적용)
    fun showBiometricPrompt() {
        if (!isBiometricAvailable()) return

        val fragmentActivity = context.getFragmentActivity()
        if (fragmentActivity == null) {
            Toast.makeText(context, "생체 인증을 실행할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val executor: Executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            fragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    callback.onAuthSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    callback.onAuthError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    callback.onAuthFailed()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("생체 인증\n")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .setNegativeButtonText("취소")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}