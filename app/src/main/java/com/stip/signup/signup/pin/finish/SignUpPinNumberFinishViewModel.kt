package com.stip.stip.signup.signup.pin.finish

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
class SignUpPinNumberFinishViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository
): BaseViewModel() {

    private val _errorLiveData = MutableLiveData<Int>()
    val errorLiveData: LiveData<Int> get() = _errorLiveData

    private val _authLoginLiveData = MutableLiveData<ResponseAuthLogin>()
    val authLoginLiveData: LiveData<ResponseAuthLogin> get() = _authLoginLiveData

    private suspend fun fetchMemberInfo(): Boolean {
        var retryCount = 0
        while (retryCount < 3) {
            Log.d("SignUpPinNumberFinishViewModel", "회원 정보 조회 시도 ${retryCount + 1}")
            val memberResponse = memberRepository.getMembers()
            
            var success = false
            memberResponse.onSuccess {
                Log.d("SignUpPinNumberFinishViewModel", "회원 정보 조회 성공: $data")
                PreferenceUtil.saveUserId(data.id)
                Log.d("SignUpPinNumberFinishViewModel", "userId 저장 완료: ${data.id}")
                
                // MemberData를 MemberInfo로 변환하여 저장 (사용 가능한 필드만)
                try {
                    val memberInfo = com.stip.stip.model.MemberInfo(
                        id = data.id,
                        name = data.name,
                        englishFirstName = "",
                        englishLastName = "",
                        telecomProvider = data.telecomProvider,
                        phoneNumber = data.phoneNumber,
                        birthdate = "",
                        email = "",
                        bankCode = "",
                        accountNumber = "",
                        address = "",
                        addressDetail = "",
                        postalCode = "",
                        isDirectAccount = false,
                        usagePurpose = "",
                        sourceOfFunds = "",
                        job = ""
                    )
                    PreferenceUtil.saveMemberInfo(memberInfo)
                    Log.d("SignUpPinNumberFinishViewModel", "기본 회원정보 저장 완료: ${memberInfo.name}")
                } catch (e: Exception) {
                    Log.e("SignUpPinNumberFinishViewModel", "기본 회원정보 저장 실패: ${e.message}")
                }
                
                success = true
            }.onError {
                Log.e("SignUpPinNumberFinishViewModel", "회원 정보 조회 실패 (시도 ${retryCount + 1}): ${statusCode.code}")
            }
            
            if (success) return true
            
            if (retryCount < 2) {
                delay(500)
            }
            retryCount++
        }
        return false
    }

    /** 로그인 처리 */
    fun requestPostAuthLogin(requestAuthLogin: RequestAuthLogin) {
        showProgress()

        viewModelScope.launch {
            val response: com.skydoves.sandwich.ApiResponse<ResponseAuthLogin> = authRepository.postAuthLogin(requestAuthLogin)
            response.suspendOnSuccess {
                this.response.body()?.let { loginResponse ->
                    Log.d("SignUpPinNumberFinishViewModel", "로그인 성공, 토큰 저장: ${loginResponse.accessToken}")
                    PreferenceUtil.saveToken(loginResponse.accessToken)
                    // MainViewModel의 isAuthenticated가 작동하도록 Constants 키에도 저장
                    PreferenceUtil.putString(com.stip.stip.signup.Constants.PREF_KEY_AUTH_TOKEN_VALUE, loginResponse.accessToken)
                    
                    // 토큰에서 userId 추출 시도
                    val extractedUserId = PreferenceUtil.extractUserIdFromToken(loginResponse.accessToken)
                    if (extractedUserId != null) {
                        Log.d("SignUpPinNumberFinishViewModel", "토큰에서 userId 추출 성공: $extractedUserId")
                    } else {
                        Log.w("SignUpPinNumberFinishViewModel", "토큰에서 userId 추출 실패, API에서 조회 시도")
                        
                        // 토큰 저장이 완료된 후 약간의 딜레이를 준 후 회원정보 조회
                        delay(500)
                        
                        val success = fetchMemberInfo()
                        if (!success) {
                            // 모든 시도 실패 후 기존 정보 확인
                            try {
                                val existingMemberInfo = PreferenceUtil.getMemberInfo()
                                if (existingMemberInfo != null) {
                                    Log.d("SignUpPinNumberFinishViewModel", "기존 저장된 회원정보 사용: ${existingMemberInfo.name}")
                                    PreferenceUtil.saveUserId(existingMemberInfo.id)
                                    Log.d("SignUpPinNumberFinishViewModel", "기존 userId 활용: ${existingMemberInfo.id}")
                                } else {
                                    Log.w("SignUpPinNumberFinishViewModel", "기존 저장된 회원정보도 없음")
                                }
                            } catch (e: Exception) {
                                Log.e("SignUpPinNumberFinishViewModel", "기존 회원정보 조회 중 오류: ${e.message}")
                            }
                        }
                    }
                    
                    _authLoginLiveData.value = loginResponse
                }
            }.suspendOnError {
                Log.e("SignUpPinNumberFinishViewModel", "로그인 실패: ${this.response.code()}")
                val errorMessage = when (this.response.code()) {
                    409 -> Constants.NETWORK_DUPLICATE_ERROR_CODE
                    else -> Constants.NETWORK_SERVER_ERROR_CODE
                }
                _errorLiveData.value = errorMessage
            }.suspendOnException {
                Log.e("SignUpPinNumberFinishViewModel", "로그인 처리 중 예외 발생: ${this.message}")
                _errorLiveData.value = Constants.NETWORK_ERROR_CODE
            }
            
            hideProgress()
        }
    }


    private val _memberDeleteLiveData = MutableLiveData<Boolean>()
    val memberDeleteLiveData: LiveData<Boolean> get() = _memberDeleteLiveData
    /** 회원 탈퇴 */
    fun requestDeleteMembers(di: String, pin: String) {
        showProgress()

        viewModelScope.launch {
            try {
                val token = PreferenceUtil.getString(Constants.PREF_KEY_AUTH_TOKEN_VALUE)
                if (token.isNullOrBlank()) {
                    _errorLiveData.value = Constants.NETWORK_ERROR_CODE
                    hideProgress()
                    return@launch
                }
                
                val response = memberRepository.deleteMembers(token)
                if (response.isSuccessful) {
                    val deleteResponse = response.body()
                    if (deleteResponse?.success == true) {
                        _memberDeleteLiveData.value = true
                    } else {
                        _errorLiveData.value = Constants.NETWORK_SERVER_ERROR_CODE
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        409 -> Constants.NETWORK_DUPLICATE_ERROR_CODE
                        else -> Constants.NETWORK_SERVER_ERROR_CODE
                    }
                    _errorLiveData.value = errorMessage
                }
            } catch (e: Exception) {
                Log.e("SignUpPinNumberFinishViewModel", "회원 탈퇴 처리 중 예외 발생", e)
                _errorLiveData.value = Constants.NETWORK_ERROR_CODE
            } finally {
                hideProgress()
            }
        }
    }

}