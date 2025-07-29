package com.stip.stip.signup.signup.bank.deposit

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.suspendOnError
import com.skydoves.sandwich.suspendOnException
import com.skydoves.sandwich.suspendOnSuccess
import com.stip.stip.signup.Constants.PREF_KEY_BANK_DEPOSIT_SAVE_TIME
import com.stip.stip.signup.Constants.PREF_KEY_BANK_DEPOSIT_TIME_VALUE
import com.stip.stip.signup.api.repository.auth.AuthRepository
import com.stip.stip.signup.base.BaseViewModel
import com.stip.stip.signup.model.RequestAccountOTP
import com.stip.stip.signup.utils.MutableSingleLiveData
import com.stip.stip.signup.utils.PreferenceUtil
import com.stip.stip.signup.utils.SingleLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpBankDepositViewModel @Inject constructor(
    private val authRepository: AuthRepository
): BaseViewModel() {

    private val _signUpBankDepositNumberCheckLiveData = MutableSingleLiveData<Boolean>()
    val signUpBankDepositNumberCheckLiveData: SingleLiveData<Boolean> get() = _signUpBankDepositNumberCheckLiveData
    /** 계좌 인증번호 전송 enable */
    fun isBankDepositNumberCheck(number: String) {
        _signUpBankDepositNumberCheckLiveData.setValue(number.count() == 6)
    }

    private val _signUpOTPNumberLiveData = MutableSingleLiveData<Boolean>()
    val signUpOTPNumberLiveData: SingleLiveData<Boolean> get() = _signUpOTPNumberLiveData
    /** 계좌 점유 인증 OTP 번호 확인 */
    fun requestOTPNumber(requestAccountOTP: RequestAccountOTP) {
        showProgress()

        viewModelScope.launch {
            val response = authRepository.postNiceAccountOwnerShipOTPVerify(requestAccountOTP)
            response.suspendOnSuccess {
                _signUpOTPNumberLiveData.setValue(true)
            }.suspendOnError {
                Log.e("Error",this.errorBody?.string() ?: "error")
                _signUpOTPNumberLiveData.setValue(false)
            }.suspendOnException {
                Log.e("Exception",this.message.toString())
                _signUpOTPNumberLiveData.setValue(false)
            }
            hideProgress()
        }
    }

    /** 타이머 관리 */
    private lateinit var job : Job

    private val _timerCountLiveData = MutableLiveData(1000 * 60 * 5)
    private val _timeData = MutableLiveData<String>()
    val timeData: LiveData<String> get() = _timeData
    /** 인증번호 입력 제한 시간 타이머 */
    fun timerStart(){
        if(::job.isInitialized) job.cancel()

        val lastSavedTime = PreferenceUtil.getLong(PREF_KEY_BANK_DEPOSIT_SAVE_TIME)
        val lastSavedTimerValue = PreferenceUtil.getInt(PREF_KEY_BANK_DEPOSIT_TIME_VALUE, 1000 * 60 * 5)

        val currentTime = System.currentTimeMillis()
        val timeElapsed = (currentTime - lastSavedTime) // 경과 시간 계산

        _timerCountLiveData.value = if(lastSavedTime == 0L) {
            1000 * 60 * 5
        } else if (timeElapsed < lastSavedTimerValue) {
            (lastSavedTimerValue - timeElapsed).toInt()
        } else {
            0 // 만료 처리
        }

        if(_timerCountLiveData.value == 0){
            _timeData.value = "00:00"
        }

        job = viewModelScope.launch {
            while(_timerCountLiveData.value!! > 0) {
                _timerCountLiveData.value = _timerCountLiveData.value!!.minus(1000)
                delay(1000L)

                val minute = _timerCountLiveData.value!!.div(1000).mod(3600).div(60)
                val second = _timerCountLiveData.value!!.div(1000).mod(60)

                val displayMinute = if (minute < 10) "0$minute" else "$minute"
                val displaySecond = if (second < 10) "0$second" else "$second"

                _timeData.value = "$displayMinute:$displaySecond"
            }
        }
    }

    fun saveTimerState(){
        PreferenceUtil.putLong(PREF_KEY_BANK_DEPOSIT_SAVE_TIME, System.currentTimeMillis())
        PreferenceUtil.putInt(PREF_KEY_BANK_DEPOSIT_TIME_VALUE, _timerCountLiveData.value ?: 0)
    }

    fun saveTimerReset(){
        PreferenceUtil.putLong(PREF_KEY_BANK_DEPOSIT_SAVE_TIME, 0L)
        PreferenceUtil.putInt(PREF_KEY_BANK_DEPOSIT_TIME_VALUE, 1000 * 60 * 5)
    }

}