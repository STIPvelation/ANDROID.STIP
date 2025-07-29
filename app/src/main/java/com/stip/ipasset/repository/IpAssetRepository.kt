package com.stip.ipasset.repository

import android.content.Context
import com.stip.ipasset.model.IpAsset

class IpAssetRepository private constructor(private val context: Context) {

    companion object {
        private var instance: IpAssetRepository? = null
        private var testData: List<IpAsset> = emptyList()
        
        fun getInstance(context: Context): IpAssetRepository {
            if (instance == null) {
                instance = IpAssetRepository(context)
            }
            return instance!!
        }
        
        fun setTestData(data: List<IpAsset>) {
            testData = data
        }
    }
    
    fun getIpAssets(): List<IpAsset> {
        return testData
    }
}
