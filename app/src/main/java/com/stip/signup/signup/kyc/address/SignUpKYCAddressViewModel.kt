package com.stip.stip.signup.signup.kyc.address

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.suspendOnError
import com.skydoves.sandwich.suspendOnException
import com.skydoves.sandwich.suspendOnSuccess
import com.stip.stip.signup.api.repository.kakao.KakaoLocationRepository
import com.stip.stip.signup.base.BaseViewModel
import com.stip.stip.signup.model.KakaoZipCodeData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpKYCAddressViewModel @Inject constructor(
    private val kakaoLocationRepository: KakaoLocationRepository
): BaseViewModel() {

    private val _signUpKYCZipCodeKakaoLiveData = MutableLiveData<KakaoZipCodeData>()
    val signUpKYCZipCodeKakaoLiveData: LiveData<KakaoZipCodeData> get() = _signUpKYCZipCodeKakaoLiveData
    /** 카카오 주소 검색 API */
    fun requestGetZipCodeKakao(key: String, query: String) {
        showProgress()

        viewModelScope.launch {
            val response = kakaoLocationRepository.getKakaoZipCode(key, query)

            response.suspendOnSuccess {
                this.response.body()?.let {
                    _signUpKYCZipCodeKakaoLiveData.value = it
                }
            }.suspendOnError {
                Log.e("Error",this.errorBody?.string().toString())
            }.suspendOnException {
                Log.e("Exception",this.message.toString())
            }

            hideProgress()
        }
    }

}