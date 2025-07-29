package com.stip.stip.signup.signup.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stip.stip.signup.base.BaseViewModel

class SignUpAuthViewModel: BaseViewModel() {

    private val _signUpAuthAllAgreeCheckLiveData = MutableLiveData<Boolean>()
    val signUpAuthAllAgreeCheckLiveData: LiveData<Boolean> get() = _signUpAuthAllAgreeCheckLiveData
    /** 회원가입 약관 전체 동의 상태 */
    fun statusAllAgree(isCheck: Boolean) {
        _signUpAuthAllAgreeCheckLiveData.value = isCheck
    }

}