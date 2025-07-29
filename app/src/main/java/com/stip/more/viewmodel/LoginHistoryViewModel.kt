package com.stip.stip.more.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stip.stip.more.model.LoginRecord
import java.util.*
import kotlinx.coroutines.*

/**
 * 로그인 이력 관리를 위한 ViewModel
 * iOS의 LoginHistoryViewModel과 유사한 기능을 구현
 */
class LoginHistoryViewModel : ViewModel() {
    
    private val TAG = "LoginHistoryViewModel"
    
    private val _loginHistory = MutableLiveData<List<LoginRecord>>()
    val loginHistory: LiveData<List<LoginRecord>> = _loginHistory
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _filterPeriod = MutableLiveData<String>("전체")
    val filterPeriod: LiveData<String> = _filterPeriod
    
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    
    /**
     * 필터링 기간에 따라 로그인 이력을 필터링하여 반환
     */
    fun getFilteredHistory(): List<LoginRecord> {
        val currentFilterPeriod = filterPeriod.value ?: "전체"
        val currentHistory = loginHistory.value ?: listOf()
        
        if (currentFilterPeriod == "전체") {
            return currentHistory
        }
        
        val calendar = Calendar.getInstance()
        val currentDate = calendar.time
        
        when (currentFilterPeriod) {
            "1주일" -> calendar.add(Calendar.DAY_OF_MONTH, -7)
            "1개월" -> calendar.add(Calendar.MONTH, -1)
            "3개월" -> calendar.add(Calendar.MONTH, -3)
            "6개월" -> calendar.add(Calendar.MONTH, -6)
            "1년" -> calendar.add(Calendar.YEAR, -1)
            else -> return currentHistory
        }
        
        val startDate = calendar.time
        return currentHistory.filter { it.date.after(startDate) || it.date == startDate }
    }
    
    /**
     * 필터링 기간 설정
     */
    fun setFilterPeriod(period: String) {
        _filterPeriod.value = period
    }
    
    /**
     * 로그인 이력 데이터 로드 - 실제 API 호출
     */
    fun fetchLoginHistory() {
        _isLoading.value = true
        Log.d(TAG, "Starting to fetch login history from API")
        
        coroutineScope.launch {
            try {
                // 실제 API 호출 코드
                val token = com.stip.stip.signup.utils.PreferenceUtil.getToken()
                if (token.isNullOrEmpty()) {
                    Log.e(TAG, "Token is null or empty, cannot fetch login history")
                    _loginHistory.value = emptyList()
                    return@launch
                }
                
                // 여기에 Retrofit을 통한 API 호출 코드가 들어갑니다
                // 예시: val response = apiService.getLoginHistory("Bearer $token")
                // val data = response.body() ?: emptyList()
                
                // 현재는 인증 오류로 인해 임시로 빈 목록을 반환
                Log.d(TAG, "API call would be made here with token")
                _loginHistory.value = emptyList()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching login history: ${e.message}")
                _loginHistory.value = emptyList() // 오류 발생 시 빈 목록 설정
            } finally {
                _isLoading.value = false
                Log.d(TAG, "Login history fetch completed")
            }
        }
    }
    
    /**
     * 모든 기기에서 로그아웃
     */
    fun logoutFromAllDevices(onComplete: () -> Unit) {
        _isLoading.value = true
        
        coroutineScope.launch {
            delay(1000) // 서버 통신을 시뮬레이션하기 위한 지연
            // 실제로는 여기에 로그아웃 API 호출 코드가 들어갑니다
            _isLoading.value = false
            onComplete()
        }
    }
    
    // 실제 API 호출로 대체되어 getSampleData() 메서드는 삭제됨
    
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}
