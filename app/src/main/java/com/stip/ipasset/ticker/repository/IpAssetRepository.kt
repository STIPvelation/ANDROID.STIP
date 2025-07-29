package com.stip.ipasset.ticker.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stip.ipasset.model.IpAsset

class IpAssetRepository private constructor(private val context: Context) {

    // LiveData를 통해 자산 데이터를 관리하도록 변경
    private val _assetsData = MutableLiveData<List<IpAsset>>(emptyList())
    val assetsData: LiveData<List<IpAsset>> = _assetsData

    companion object {
        private var instance: IpAssetRepository? = null
        
        fun getInstance(context: Context): IpAssetRepository {
            if (instance == null) {
                instance = IpAssetRepository(context)
            }
            return instance!!
        }
        
        fun setTestData(context: Context, data: List<IpAsset>) {
            getInstance(context)._assetsData.postValue(data)
        }
    }
    
    fun getIpAssets(): List<IpAsset> {
        return _assetsData.value ?: emptyList()
    }
    
    fun getAsset(id: String): IpAsset? {
        return getIpAssets().find { it.id == id }
    }
    
    // 새 자산 추가 메서드
    fun addAsset(asset: IpAsset): Boolean {
        val currentAssets = getIpAssets().toMutableList()
        if (currentAssets.any { it.id == asset.id }) {
            return false // 이미 존재하는 자산은 추가하지 않음
        }
        currentAssets.add(asset)
        _assetsData.postValue(currentAssets)
        return true
    }
    
    fun updateAsset(id: String, newBalance: Double): IpAsset? {
        val currentAssets = getIpAssets().toMutableList()
        val existingIndex = currentAssets.indexOfFirst { it.id == id }
        if (existingIndex != -1) {
            val existing = currentAssets[existingIndex]
            val updated = existing.copy(balance = newBalance, amount = newBalance)
            currentAssets[existingIndex] = updated
            _assetsData.postValue(currentAssets)
            return updated
        }
        return null
    }
}
