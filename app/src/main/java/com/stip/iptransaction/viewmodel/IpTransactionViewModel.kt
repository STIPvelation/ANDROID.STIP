package com.stip.stip.iptransaction.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stip.stip.iptransaction.repository.IpTransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IpTransactionViewModel @Inject constructor(
    private val repository: IpTransactionRepository
) : ViewModel() {

    private val _cancelOrderResult = MutableLiveData<Result<Unit>>()
    val cancelOrderResult: LiveData<Result<Unit>> = _cancelOrderResult
    
    // 잔액 새로고침 콜백
    private var onBalanceRefreshCallback: (() -> Unit)? = null
    
    fun setOnBalanceRefreshCallback(callback: () -> Unit) {
        onBalanceRefreshCallback = callback
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            try {
                val result = repository.cancelOrder(orderId)
                _cancelOrderResult.value = result
                
                // 주문 취소 시도 후 항상 잔액 새로고침 (성공/실패 관계없이)
                onBalanceRefreshCallback?.invoke()
                
                // 전역 OrderDataCoordinator를 통해 잔액 새로고침
                val globalCoordinator = com.stip.stip.iphome.fragment.OrderContentViewFragment.getGlobalOrderDataCoordinator()
                globalCoordinator?.refreshBalance()
                
                // 주문 취소 성공 시 추가 처리
                if (result.isSuccess) {
                    Log.d("IpTransactionViewModel", "주문 취소 성공 - 잔액 새로고침 완료")
                } else {
                    Log.w("IpTransactionViewModel", "주문 취소 실패 - 잔액 새로고침은 수행됨")
                }
            } catch (e: Exception) {
                _cancelOrderResult.value = Result.failure(e)
            }
        }
    }
    
    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            try {
                val result = repository.cancelOrder(orderId)
                _cancelOrderResult.value = result
            } catch (e: Exception) {
                _cancelOrderResult.value = Result.failure(e)
            }
        }
    }
} 