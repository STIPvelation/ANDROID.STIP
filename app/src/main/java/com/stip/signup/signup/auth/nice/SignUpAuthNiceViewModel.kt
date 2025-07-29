package com.stip.stip.signup.signup.auth.nice

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.stip.stip.signup.api.repository.member.MemberRepository
import com.stip.stip.signup.base.BaseViewModel
import com.stip.stip.signup.model.MemberData
import com.stip.stip.signup.model.ResponseExistMember
import com.stip.stip.signup.utils.MutableSingleLiveData
import com.stip.stip.signup.utils.SingleLiveData
import com.skydoves.sandwich.onError
import com.skydoves.sandwich.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpAuthNiceViewModel @Inject constructor(
    private val memberRepository: MemberRepository
) : BaseViewModel() {

    private val _errorLiveData = MutableLiveData<Int>()
    val errorLiveData: LiveData<Int> get() = _errorLiveData

    private val _memberExistenceLiveData = MutableSingleLiveData<ResponseExistMember>()
    val memberExistenceLiveData: SingleLiveData<ResponseExistMember> get() = _memberExistenceLiveData

    // 본인인증 DI 값 저장
    private val _diValue = MutableLiveData<String>()
    val diValue: LiveData<String> get() = _diValue

    // 네비게이션 이벤트를 위한 SingleLiveEvent
    private val _navigateToBankEvent = MutableSingleLiveData<String>()
    val navigateToBankEvent: SingleLiveData<String> get() = _navigateToBankEvent

    // 회원 정보 조회 결과
    private val _memberInfoLiveData = MutableSingleLiveData<MemberData>()
    val memberInfoLiveData: SingleLiveData<MemberData> get() = _memberInfoLiveData

    // 로그인 상태 관리
    private val _loginStateLiveData = MutableLiveData<Boolean>(false)
    val loginStateLiveData: LiveData<Boolean> get() = _loginStateLiveData

    /** DI 값 설정 및 네비게이션 이벤트 발생 */
    fun setDiValue(di: String) {
        _diValue.value = di
        // 네비게이션 이벤트 발생
        _navigateToBankEvent.setValue(di)
    }

    /** 로그인 상태 설정 */
    fun setLoginState(isLoggedIn: Boolean) {
        _loginStateLiveData.value = isLoggedIn
    }

    /** 회원 존재 여부 확인 */
    fun requestGetNiceAuth(di: String) {
        showProgress()

        viewModelScope.launch {
            val response = memberRepository.getExistenceMember(di)
            android.util.Log.i("API CALL", "RESPONSE : " + response.toString())
            response.onSuccess {
                data.di = di
                // API 호출 성공
                _memberExistenceLiveData.setValue(data)
            }.onError {
                // API 호출 실패
                _errorLiveData.value = statusCode.code
            }
            hideProgress()
        }
    }

    /** 회원 정보 가져오기 */
    fun requestGetMemberInfo(di: String) {
        viewModelScope.launch {
            val response = memberRepository.getMembers()
            response.onSuccess {
                Log.d("SignUpAuthNice", "Member info fetched successfully" + data.toString())
                // 회원 정보 수신 성공
                _memberInfoLiveData.setValue(data)

                // 회원 정보 저장
                // TODO: 더미 유저 정보
                // Removed redundant null check
                // SharedPreference에 회원 정보 저장
//                val memberInfo = MemberInfo(
//                    name = data.name,
//                    // MemberInfo에는 email 필드가 필요하지만 MemberData에없으므로 빈 값 사용
//                    email = "user@example.com",
//                    phone = data.phoneNumber,
//                    memberId = data.memberNumber
//                )
//                // TODO: saveMemberInfo
//                PreferenceUtil.saveMemberInfo(memberInfo)

                // TODO: saveToken
                // JWT 토큰 저장 - API 응답에서 가져올 토큰이 없으니면 임시 토큰 생성
                val dummyToken =
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJtZW1iZXJJZCI6XCIke2RhdGEubWVtYmVyTnVtYmVyfVwiLCJpYXQiOjE2NTE4MzAyMDh9"
//                PreferenceUtil.saveToken(dummyToken)
//                val dummyToken =
//                // 토큰 저장 - API 응답에서 가져온 토큰 사용
//                PreferenceUtil.saveToken(data.token) // 실제 API 응답에 토큰이 포함되면 해당 값 사용
//                PreferenceUtil.saveToken(dummyToken)
            }.onError {
                Log.e("SignUpAuthNice", "Error fetching member info")
                // API 호출 실패
                _errorLiveData.value = statusCode.code
                // 오류 발생 시 로그인 상태 초기화
                setLoginState(false)
            }
        }
    }
}