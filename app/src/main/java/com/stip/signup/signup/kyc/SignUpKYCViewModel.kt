package com.stip.stip.signup.signup.kyc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.stip.stip.signup.base.BaseViewModel

class SignUpKYCViewModel: BaseViewModel() {

    val email = MutableLiveData<String>()
    val name = MutableLiveData<String>()
    val address = MutableLiveData<String>()
    val isAccount = MutableLiveData(true)
    val purpose = MutableLiveData<String>()
    val source = MutableLiveData<String>()
    val job = MutableLiveData<String>()

    private val _isButtonEnabled = MediatorLiveData<Boolean>().apply {
        addSource(email) { checkFields() }
        addSource(name) { checkFields() }
        addSource(address) { checkFields() }
        addSource(isAccount) { checkFields() }
        addSource(purpose) { checkFields() }
        addSource(source) { checkFields() }
        addSource(job) { checkFields() }
    }
    val isButtonEnabled: LiveData<Boolean> get() = _isButtonEnabled

    private fun checkFields() {
        _isButtonEnabled.value = !email.value.isNullOrBlank() &&
                !name.value.isNullOrBlank() &&
                !address.value.isNullOrBlank() &&
                isAccount.value == true &&
                !purpose.value.isNullOrBlank() &&
                !source.value.isNullOrBlank() &&
                !job.value.isNullOrBlank()
    }


}