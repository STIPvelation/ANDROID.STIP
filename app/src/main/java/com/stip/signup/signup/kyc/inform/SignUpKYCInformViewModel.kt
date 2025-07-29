package com.stip.stip.signup.signup.kyc.inform

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stip.stip.signup.base.BaseViewModel

class SignUpKYCInformViewModel: BaseViewModel() {

    var lastName: String? = null
    var firstName: String? = null

    private val _signUpKYCInformCheckLiveData = MutableLiveData<Boolean>()
    val signUpKYCInformCheckLiveData: LiveData<Boolean> get() = _signUpKYCInformCheckLiveData
    /** KYC Inform Validation */
    fun isKYCInformCheck(lastName: String, firstNamed: String, currentAddress: String) {
        _signUpKYCInformCheckLiveData.value = lastName.isNotBlank() && firstNamed.isNotBlank() && currentAddress.isNotBlank()
    }

}