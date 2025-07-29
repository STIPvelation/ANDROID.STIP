package com.stip.ipasset.ticker.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stip.ipasset.model.TransactionHistory

class TransactionDetailViewModel : ViewModel() {
    
    private val _transaction = MutableLiveData<TransactionHistory?>()
    val transaction: LiveData<TransactionHistory?> = _transaction
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error
    
    fun fetchTransactionDetailsById(transactionId: String) {
        _isLoading.value = true
        
        // Simulating network request to fetch transaction details
        // In a real app, this would be done via a repository
        val dummyTransaction = TransactionHistory(
            id = transactionId,
            timestamp = System.currentTimeMillis() / 1000, // Convert to seconds
            type = "WITHDRAWAL",
            amount = 100_000_000, // Amount in smallest units
            ticker = "BTC",
            currencyCode = "BTC",
            status = TransactionHistory.Status.WITHDRAWAL_COMPLETED,
            txId = "0xabcdef123456789",
            toAddress = "0x123456789abcdef",
            executionPrice = 35000.0 // 1 BTC = 35,000 USD
        )
        
        // Simulate delay - Deprecated Handler() 생성자 수정
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            _isLoading.value = false
            _transaction.value = dummyTransaction
        }, 500)
    }
}
