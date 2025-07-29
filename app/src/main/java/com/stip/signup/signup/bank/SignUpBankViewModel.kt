package com.stip.stip.signup.signup.bank

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.suspendOnError
import com.skydoves.sandwich.suspendOnException
import com.skydoves.sandwich.suspendOnSuccess
import com.stip.stip.signup.Constants
import com.stip.stip.signup.api.repository.auth.AuthRepository
import com.stip.stip.signup.base.BaseViewModel
import com.stip.stip.signup.model.RequestAccountNumber
import com.stip.stip.signup.model.ResponseAccountNumber
import com.stip.stip.signup.model.ResponseAuthPass
import com.stip.stip.signup.utils.MutableSingleLiveData
import com.stip.stip.signup.utils.SingleLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpBankViewModel @Inject constructor(
    private val authRepository: AuthRepository
): BaseViewModel() {

    /**
     * 회원 가입 Bank
     */
    private val _signUpBankAuthNumberCheckLiveData = MutableSingleLiveData<Boolean>()
    val signUpBankAuthNumberCheckLiveData: SingleLiveData<Boolean> get() = _signUpBankAuthNumberCheckLiveData
    /** 계좌 인증번호 전송 enable */
    fun isBankAuthNumberCheck(bank: String, accountNumber: String) {
        _signUpBankAuthNumberCheckLiveData.setValue(bank.isNotBlank() && accountNumber.isNotBlank())
    }

    private val _signUpBankAuthVerify = MutableSingleLiveData<ResponseAccountNumber>()
    val signUpBankAuthVerify: SingleLiveData<ResponseAccountNumber> get() = _signUpBankAuthVerify
    /** 계좌 번호 확인 */
    fun requestPostBankVerify(requestBankVerify: RequestAccountNumber) {
        showProgress()

        viewModelScope.launch {
            val response = authRepository.postNiceAccountOwnerShipVerify(requestBankVerify)
            response.suspendOnSuccess {
                this.response.body()?.let {
                    _signUpBankAuthVerify.setValue(it)
                }
            }.suspendOnError {
                Log.e("Error",this.errorBody?.string() ?: "error")
                _errorLiveData.value = Constants.NETWORK_SERVER_ERROR_CODE
            }.suspendOnException {
                Log.e("Exception",this.message.toString())
                _errorLiveData.value = Constants.NETWORK_ERROR_CODE
            }

            hideProgress()
        }
    }


    private val _errorLiveData = MutableLiveData<Int>()
    val errorLiveData: LiveData<Int> get() = _errorLiveData

    private val _signUpAuthNiceLiveData = MutableSingleLiveData<ResponseAuthPass>()
    val signUpAuthNiceLiveData: SingleLiveData<ResponseAuthPass> get() = _signUpAuthNiceLiveData
    /** NICE PASS 본인 인증 정보 조회*/
    fun requestGetNiceAuth(di: String) {
        showProgress()

        viewModelScope.launch {
            val response = authRepository.getNiceAuthPass(di)
            response.suspendOnSuccess {
                this.response.body()?.let {
                    _signUpAuthNiceLiveData.setValue(it)
                }
            }.suspendOnError {
                Log.e("Error",this.errorBody?.string() ?: "error")
                _errorLiveData.value = Constants.NETWORK_SERVER_ERROR_CODE
            }.suspendOnException {
                Log.e("Exception",this.message.toString())
                _errorLiveData.value = Constants.NETWORK_ERROR_CODE
            }
            hideProgress()
        }
    }

}