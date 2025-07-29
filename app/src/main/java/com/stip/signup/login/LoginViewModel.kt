package com.stip.stip.signup.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.onError
import com.skydoves.sandwich.onSuccess
import com.skydoves.sandwich.suspendOnError
import com.skydoves.sandwich.suspendOnException
import com.skydoves.sandwich.suspendOnSuccess
import com.stip.stip.signup.Constants
import com.stip.stip.signup.api.repository.auth.AuthRepository
import com.stip.stip.signup.api.repository.member.MemberRepository
import com.stip.stip.signup.base.BaseViewModel
import com.stip.stip.signup.model.RequestAuthLogin
import com.stip.stip.signup.model.ResponseAuthLogin
import com.stip.stip.signup.utils.PreferenceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository
): BaseViewModel() {

    private val _pinNumberBasicSettingLiveData = MutableLiveData<Boolean>()
    val pinNumberBasicSettingLiveData: LiveData<Boolean> get() = _pinNumberBasicSettingLiveData
    /** PIN 번호로 기본 로그인 설정 */
    fun setPinNumberBasicSetting(isCheck: Boolean) {
        _pinNumberBasicSettingLiveData.value = isCheck
    }

    private val _biometricAuthBasicSettingLiveData = MutableLiveData<Boolean>()
    val biometricAuthBasicSettingLiveData: LiveData<Boolean> get() = _biometricAuthBasicSettingLiveData
    /** 생체인증으로 기본 로그인 설정 */
    fun setBiometricAuthBasicSetting(isCheck: Boolean) {
        _biometricAuthBasicSettingLiveData.value = isCheck
    }

    private val _errorLiveData = MutableLiveData<Int>()
    val errorLiveData: LiveData<Int> get() = _errorLiveData

    private val _authLoginLiveData = MutableLiveData<ResponseAuthLogin>()
    val authLoginLiveData: LiveData<ResponseAuthLogin> get() = _authLoginLiveData

    private suspend fun fetchMemberInfo(): Boolean {
        var retryCount = 0
        while (retryCount < 3) {
            Log.d("LoginViewModel", "회원 정보 조회 시도 ${retryCount + 1}")
            val memberResponse = memberRepository.getMembers()
            
            var success = false
            memberResponse.onSuccess {
                Log.d("LoginViewModel", "회원 정보 조회 성공: $data")
                PreferenceUtil.saveUserId(data.id)
                Log.d("LoginViewModel", "userId 저장 완료: ${data.id}")
                success = true
            }.onError {
                Log.e("LoginViewModel", "회원 정보 조회 실패 (시도 ${retryCount + 1}): ${statusCode.code}")
            }
            
            if (success) return true
            
            if (retryCount < 2) {
                delay(500)
            }
            retryCount++
        }
        return false
    }

    suspend fun getMemberInfo() = memberRepository.getMembers()

    /** 로그인 처리 */
    fun requestPostAuthLogin(requestAuthLogin: RequestAuthLogin) {
        showProgress()

        viewModelScope.launch {
            val response = authRepository.postAuthLogin(requestAuthLogin)
            response.suspendOnSuccess {
                this.response.body()?.let { loginResponse ->
                    Log.d("LoginViewModel", "로그인 성공, 토큰 저장: ${loginResponse.accessToken}")
                    PreferenceUtil.saveToken(loginResponse.accessToken)
                    
                    // 토큰에서 userId 추출 시도
                    val extractedUserId = PreferenceUtil.extractUserIdFromToken(loginResponse.accessToken)
                    if (extractedUserId != null) {
                        Log.d("LoginViewModel", "토큰에서 userId 추출 성공: $extractedUserId")
                    } else {
                        Log.w("LoginViewModel", "토큰에서 userId 추출 실패, API에서 조회 시도")
                        
                        // 토큰 저장이 완료된 후 약간의 딜레이를 준 후 회원정보 조회
                        delay(500)
                        
                        val success = fetchMemberInfo()
                        if (!success) {
                            // 모든 시도 실패 후 기존 정보 확인
                            try {
                                val existingMemberInfo = PreferenceUtil.getMemberInfo()
                                if (existingMemberInfo != null) {
                                    Log.d("LoginViewModel", "기존 저장된 회원정보 사용: ${existingMemberInfo.name}")
                                    PreferenceUtil.saveUserId(existingMemberInfo.id)
                                    Log.d("LoginViewModel", "기존 userId 활용: ${existingMemberInfo.id}")
                                } else {
                                    Log.w("LoginViewModel", "기존 저장된 회원정보도 없음")
                                }
                            } catch (e: Exception) {
                                Log.e("LoginViewModel", "기존 회원정보 조회 중 오류: ${e.message}")
                            }
                        }
                    }
                    
                    _authLoginLiveData.value = loginResponse
                }
            }.suspendOnError {
                Log.e("Error",this.errorBody?.string() ?: "error")
                val errorMessage = when (this.response.code()) {
                    409 -> Constants.NETWORK_DUPLICATE_ERROR_CODE
                    else -> Constants.NETWORK_LOGIN_FAIL_CODE
                }
                _errorLiveData.value = errorMessage
            }.suspendOnException {
                Log.e("Exception",this.message.toString())
                _errorLiveData.value = Constants.NETWORK_ERROR_CODE
            }

            hideProgress()
        }
    }
}