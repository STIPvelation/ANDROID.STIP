package com.stip.ipasset.ticker.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stip.ipasset.model.IpAsset

class WithdrawalInputViewModel : ViewModel() {
    
    private val _amount = MutableLiveData<String>()
    val amount: LiveData<String> = _amount
    
    private val _address = MutableLiveData<String>()
    val address: LiveData<String> = _address
    
    private val _selectedAsset = MutableLiveData<IpAsset?>()
    val selectedAsset: LiveData<IpAsset?> = _selectedAsset
    
    fun setAmount(value: String) {
        _amount.value = value
    }
    
    fun setAddress(value: String) {
        _address.value = value
    }
    
    fun setSelectedAsset(asset: IpAsset) {
        _selectedAsset.value = asset
    }
}
