package com.stip.stip.more.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stip.stip.api.repository.MemberRepository
import com.stip.stip.model.MemberInfo
import com.stip.stip.api.service.NiceAuthResponse
import kotlinx.coroutines.launch
import android.util.Log
import com.stip.stip.model.MemberUpdateInfo

/**
 * 회원정보 수정 화면을 위한 ViewModel
 * API 통신 및 데이터 상태 관리 담당
 */
class MemberInfoEditViewModel : ViewModel() {
    private val memberRepository = MemberRepository()
    private val TAG = "MemberInfoEditViewModel"

    // 회원 정보 LiveData
    private val _memberInfo = MutableLiveData<MemberInfo?>()
    val memberInfo: LiveData<MemberInfo?> = _memberInfo

    // NICE 인증 정보 LiveData
    private val _niceAuthInfo = MutableLiveData<NiceAuthResponse?>()
    val niceAuthInfo: LiveData<NiceAuthResponse?> = _niceAuthInfo


    // 로딩 상태 LiveData
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // 에러 메시지 LiveData
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // 성공 메시지 LiveData
    private val _successMessage = MutableLiveData<String>()
    val successMessage: LiveData<String> = _successMessage

    // 회원 DI 값 LiveData
    private val _memberDi = MutableLiveData<String?>()
    val memberDi: LiveData<String?> = _memberDi


    // 회원 정보 불러오기
    fun loadMemberInfo() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val memberInfo = memberRepository.getMemberInfo()
                _memberInfo.postValue(memberInfo)
            } catch (e: Exception) {
                _errorMessage.postValue("회원 정보를 불러오는 중 오류가 발생했습니다: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // 회원 정보 업데이트
    fun updateMemberInfo(memberInfo: MemberInfo) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val success = memberRepository.updateMemberInfo(
                    MemberUpdateInfo(
                        englishFirstName = memberInfo.englishFirstName,
                        englishLastName = memberInfo.englishLastName,
                        address = memberInfo.address,
                        addressDetail = memberInfo.addressDetail,
                        postalCode = memberInfo.postalCode,
                        job = memberInfo.job
                    )
                )
                if (success) {
                    _memberInfo.postValue(memberInfo)
                    _successMessage.postValue("회원 정보가 성공적으로 업데이트되었습니다.")
                } else {
                    _errorMessage.postValue("회원 정보 업데이트에 실패했습니다.")
                }
            } catch (e: Exception) {
                _errorMessage.postValue("회원 정보 업데이트 중 오류가 발생했습니다: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * 이메일 업데이트
     */
    fun updateEmail(newEmail: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val success = memberRepository.updateEmail(newEmail)
                if (success) {
                    _memberInfo.value = _memberInfo.value?.copy(email = newEmail)
                    _successMessage.value = "이메일이 성공적으로 업데이트되었습니다."
                    Log.d(TAG, "이메일 업데이트 성공: $newEmail")
                } else {
                    _errorMessage.value = "이메일 업데이트에 실패했습니다."
                    Log.e(TAG, "이메일 업데이트 실패")
                }
            } catch (e: Exception) {
                _errorMessage.value = "이메일 업데이트 중 오류가 발생했습니다: ${e.message}"
                Log.e(TAG, "이메일 업데이트 예외 발생", e)
            } finally {
                _isLoading.value = false
            }
        }
    }


    /**
     * 전화번호 업데이트
     */
    fun updatePhone(newPhone: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val success = memberRepository.updatePhoneNumber(newPhone)
                if (success) {
                    // 성공 시 로컬 데이터 업데이트
                    _memberInfo.value = _memberInfo.value?.copy(phoneNumber = newPhone)
                    _successMessage.value = "전화번호가 성공적으로 업데이트되었습니다."
                    Log.d(TAG, "전화번호 업데이트 성공: $newPhone")
                } else {
                    _errorMessage.value = "전화번호 업데이트에 실패했습니다."
                    Log.e(TAG, "전화번호 업데이트 실패")
                }
            } catch (e: Exception) {
                _errorMessage.value = "전화번호 업데이트 중 오류가 발생했습니다: ${e.message}"
                Log.e(TAG, "전화번호 업데이트 예외 발생", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * NICE 본인인증 DI로 인증 정보 조회
     * @param di NICE 인증 DI 값
     */
    fun getNiceAuthInfo(di: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val authInfo = memberRepository.getNiceAuthInfo(di)
                if (authInfo != null) {
                    // NICE 인증 정보 저장
                    _niceAuthInfo.value = authInfo
                    Log.d(TAG, "NICE 인증 정보 조회 성공: 전화번호=${authInfo.phoneNumber}")

                    // 인증된 전화번호로 자동 업데이트
                    updatePhone(authInfo.phoneNumber)
                } else {
                    _errorMessage.value = "본인인증 정보를 가져오지 못했습니다."
                    Log.e(TAG, "NICE 인증 정보 조회 실패")
                }
            } catch (e: Exception) {
                _errorMessage.value = "본인인증 정보를 가져오는 중 오류가 발생했습니다: ${e.message}"
                Log.e(TAG, "NICE 인증 정보 조회 예외 발생", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 필수 정보 업데이트 (직업, 회사명, 주소 등)
    fun updateRequiredInfo(
        englishName: String,
        address: String,
        addressDetail: String,
        postalCode: String,
        job: String
    ) {
        Log.d(TAG, "updateRequiredInfo: $englishName, $address, $addressDetail, $postalCode, $job")
        val currentInfo = _memberInfo.value ?: return
        val updatedInfo = currentInfo.copy(
            englishFirstName = englishName.split(" ")[0],
            englishLastName = englishName.split(" ")[1],
            job = job,
            postalCode = postalCode,
            address = address,
            addressDetail = addressDetail
        )
        updateMemberInfo(updatedInfo)
    }

    /**
     * 현재 로그인된 회원의 DI 값 조회
     */
    fun getMemberDi() {
        val di = memberRepository.getMemberDi()
        _memberDi.value = di
        Log.d(TAG, "회원 DI 값 조회: $di")
    }
}

