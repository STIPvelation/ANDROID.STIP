package com.stip.stip.signup.signup

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.suspendOnError
import com.skydoves.sandwich.suspendOnException
import com.skydoves.sandwich.suspendOnSuccess
import com.stip.stip.signup.Constants
import com.stip.stip.signup.api.repository.member.MemberRepository
import com.stip.stip.signup.base.BaseViewModel
import com.stip.stip.signup.model.RequestSignUpMember
import com.stip.stip.signup.model.ResponseSignUpMember
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val memberRepository: MemberRepository
): BaseViewModel() {

    private val _isLoginLiveData = MutableLiveData<Boolean>()
    val isLoginLiveData: LiveData<Boolean> get() = _isLoginLiveData
    /** 로그인 화면에서 진입 체크 */
    fun isLoginActivityCheck(isLogin: Boolean) {
        _isLoginLiveData.value = isLogin
    }
    
    // 휴대폰 번호 변경 모드 관련 변수
    private val _isPhoneChangeMode = MutableLiveData<Boolean>(false)
    val isPhoneChangeMode: LiveData<Boolean> get() = _isPhoneChangeMode
    
    /** 휴대폰 번호 변경 모드 설정 */
    fun setPhoneChangeMode(isPhoneChange: Boolean) {
        _isPhoneChangeMode.value = isPhoneChange
    }


    val birthdate = MutableLiveData<String>()
    val phoneNumber = MutableLiveData<String>()

    val di = MutableLiveData<String>()
    val name = MutableLiveData<String>()
    val englishFirstName = MutableLiveData<String>()
    val englishLastName = MutableLiveData<String>()
    val email = MutableLiveData<String>()
    val pin = MutableLiveData<String>()
    val address = MutableLiveData<String>()
    val addressDetail = MutableLiveData<String>()
    val postalCode = MutableLiveData<String>()
    val bankCode = MutableLiveData<String>()
    val accountNumber = MutableLiveData<String>()
    val isDirectAccount = MutableLiveData<Boolean>()
    val usagePurpose = MutableLiveData<String>()
    val sourceOfFunds = MutableLiveData<String>()
    val job = MutableLiveData<String>()

    /**
     * 회원 가입
     */
    private val _errorLiveData = MutableLiveData<Int>()
    val errorLiveData: LiveData<Int> get() = _errorLiveData

    private val _signUpLiveData = MutableLiveData<ResponseSignUpMember>()
    val signUpLiveData: LiveData<ResponseSignUpMember> get() = _signUpLiveData
    /** 회원 가입 요청 API */
    fun requestPostSignUpMembers() {
        val requestSignUpMember = RequestSignUpMember(
            accountNumber = accountNumber.value ?: "",
            address = address.value ?: "",
            addressDetail = addressDetail.value ?: "",
            bankCode = bankCode.value ?: "",
            di = di.value ?: "",
            email = email.value ?: "",
            englishFirstName = englishFirstName.value ?: "",
            englishLastName = englishLastName.value ?: "",
            isDirectAccount = isDirectAccount.value ?: false,
            job = job.value ?: "",
            name = name.value ?: "",
            pin = pin.value ?: "",
            postalCode = postalCode.value ?: "",
            sourceOfFunds = sourceOfFunds.value ?: "",
            usagePurpose = usagePurpose.value ?: ""
        )
        showProgress()

        viewModelScope.launch {
            val response = memberRepository.postSignUpMembers(requestSignUpMember)
            response.suspendOnSuccess {
                this.response.body()?.let {
                    _signUpLiveData.value = it
                }
            }.suspendOnError {
                Log.e("Error",this.errorBody?.string() ?: "error")
                val errorMessage = when (this.response.code()) {
                    409 -> Constants.NETWORK_DUPLICATE_ERROR_CODE
                    else -> Constants.NETWORK_SERVER_ERROR_CODE
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
