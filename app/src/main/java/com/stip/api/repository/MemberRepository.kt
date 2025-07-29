package com.stip.stip.api.repository

import android.util.Log
import com.stip.stip.api.RetrofitClient
import com.stip.stip.api.service.MemberService
import com.stip.stip.api.service.EmailUpdateRequest
import com.stip.stip.api.service.NiceAuthResponse
import com.stip.stip.api.service.PhoneNumberUpdateRequest
import com.stip.stip.model.MemberInfo
import com.stip.stip.model.MemberUpdateInfo
import com.stip.stip.signup.utils.PreferenceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 회원정보 관련 리포지토리 클래스
 * API를 통해 회원정보를 가져오고 업데이트하는 기능 제공
 */
class MemberRepository {
    private val TAG = "MemberRepository"

    private val memberService: MemberService by lazy {
        RetrofitClient.createAuthService(MemberService::class.java)
    }

    /**
     * 회원 정보 조회
     * @return 성공 시 회원 정보, 실패 시 null
     */
    suspend fun getMemberInfo(): MemberInfo? = withContext(Dispatchers.IO) {
        try {
            memberService.getMemberInfo()
        } catch (e: Exception) {
            Log.e(TAG, "회원 정보 조회 예외 발생: ${e.message}")
            PreferenceUtil.getMemberInfo()
        }
    }

    /**
     * 회원 정보 업데이트
     * @param memberInfo 업데이트할 회원 정보
     * @return 성공 시 true, 실패 시 false
     */
    suspend fun updateMemberInfo(memberInfo: MemberUpdateInfo): Boolean = withContext(Dispatchers.IO) {
        try {
            memberService.updateMemberInfo(memberInfo)
            true
        } catch (e: Exception) {
            // 예외 발생 시 실패 반환
            Log.e(TAG, "회원 정보 업데이트 예외 발생: ${e.message}")
            false
        }
    }

    /**
     * 회원 이메일 수정
     * @param email 새로운 이메일 주소
     * @return 성공 시 true, 실패 시 false
     */
    suspend fun updateEmail(email: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "회원 이메일 업데이트 시도: $email")
            val response = memberService.updateEmail(EmailUpdateRequest(email))
            
            val isSuccess = response.isSuccessful
            if (isSuccess) {
                Log.d(TAG, "회원 이메일 업데이트 성공 (${response.code()})")
            } else {
                Log.e(TAG, "회원 이메일 업데이트 실패 (${response.code()}): ${response.message()}")
            }
            
            isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "회원 이메일 업데이트 예외 발생: ${e.message}", e)
            false
        }
    }
    
    /**
     * 회원 전화번호 수정
     * @param phoneNumber 새로운 전화번호
     * @return 성공 시 true, 실패 시 false
     */
    suspend fun updatePhoneNumber(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "회원 전화번호 업데이트 시도: $phoneNumber")
            val response = memberService.updatePhoneNumber(PhoneNumberUpdateRequest(phoneNumber))
            
            val isSuccess = response.isSuccessful
            if (isSuccess) {
                Log.d(TAG, "회원 전화번호 업데이트 성공 (${response.code()})")
            } else {
                Log.e(TAG, "회원 전화번호 업데이트 실패 (${response.code()}): ${response.message()}")
            }
            
            isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "회원 전화번호 업데이트 예외 발생: ${e.message}", e)
            false
        }
    }
    
    /**
     * NICE 본인인증 DI로 인증 정보 조회
     * @param di NICE 인증 DI 값
     * @return 성공 시 인증 정보, 실패 시 null
     */
    suspend fun getNiceAuthInfo(di: String): NiceAuthResponse? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "NICE 본인인증 정보 조회 시도: DI=$di")
            val response = memberService.getNiceAuthInfo(di)
            
            if (response.isSuccessful) {
                val authInfo = response.body()
                Log.d(TAG, "NICE 본인인증 정보 조회 성공: 전화번호=${authInfo?.phoneNumber}")
                
                // DI 값 저장 (첫 인증 성공 시)
                authInfo?.di?.let { diValue ->
                    if (PreferenceUtil.getMemberDi() == null) {
                        PreferenceUtil.saveMemberDi(diValue)
                        Log.d(TAG, "회원 DI 값 저장: $diValue")
                    }
                }
                
                return@withContext authInfo
            } else {
                Log.e(TAG, "NICE 본인인증 정보 조회 실패 (${response.code()}): ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "NICE 본인인증 정보 조회 예외 발생: ${e.message}", e)
            null
        }
    }
    
    /**
     * 로컬에 저장된 회원의 DI 값 조회
     * @return 저장된 DI 값, 없으면 null
     */
    fun getMemberDi(): String? {
        val di = PreferenceUtil.getMemberDi()
        Log.d(TAG, "로컬에서 회원 DI 값 조회: $di")
        return di
    }
}
