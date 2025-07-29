package com.stip.ipasset.usd.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stip.ipasset.manager.AssetSyncManager
import com.stip.ipasset.ticker.repository.IpAssetRepository
import com.stip.ipasset.usd.model.USDAssetData

/**
 * USD 자산 데이터 저장소 - 싱글톤 패턴 사용
 * AssetSyncManager와 연동하여 티커 자산과 동기화
 */
class USDAssetRepository private constructor() {

    // 라이브 데이터로 자산 데이터 관리
    private val _assetData = MutableLiveData<USDAssetData>(USDAssetData())
    val assetData: LiveData<USDAssetData> = _assetData
    
    // 자산 동기화 매니저
    private val assetSyncManager = AssetSyncManager.getInstance()

    // 현재 자산 데이터 가져오기
    fun getCurrentAssetData(): USDAssetData {
        return _assetData.value ?: USDAssetData()
    }

    // 자산 데이터 업데이트
    fun updateAssetData(newData: USDAssetData) {
        _assetData.postValue(newData)
    }
    
    /**
     * 입금 처리 - 티커 자산과 동기화
     * @param amount 입금 금액
     * @param context 티커 자산 업데이트를 위한 컨텍스트
     */
    fun processDeposit(amount: Double, context: Context): Boolean {
        val ipRepository = IpAssetRepository.getInstance(context)
        return assetSyncManager.processTransactionAndUpdateAssets(
            usdRepository = this,
            ipRepository = ipRepository,
            amount = amount  // 입금은 양수 금액
        )
    }

    /**
     * 출금 실행 - 잔액과 출금가능 금액 업데이트 및 티커 자산과 동기화
     * @param amount 출금 금액
     * @param context 티커 자산 업데이트를 위한 컨텍스트
     */
    fun processWithdrawal(amount: Double, context: Context): Boolean {
        if (amount <= 0) return false
        
        val currentData = getCurrentAssetData()
        
        // 출금가능 금액보다 많이 출금할 수 없음
        if (amount > currentData.withdrawable) {
            return false
        }
        
        val ipRepository = IpAssetRepository.getInstance(context)
        return assetSyncManager.processTransactionAndUpdateAssets(
            usdRepository = this,
            ipRepository = ipRepository,
            amount = -amount  // 출금은 음수 금액
        )
    }
    
    /**
     * 자산 동기화 - IP 내역의 티커 수량과 USD 자산 연동
     * @param context 티커 자산 업데이트를 위한 컨텍스트
     */
    fun syncWithIpAssets(context: Context) {
        val ipRepository = IpAssetRepository.getInstance(context)
        val currentUsdData = getCurrentAssetData()
        
        assetSyncManager.syncAssets(
            usdRepository = this,
            ipRepository = ipRepository,
            newUsdData = currentUsdData
        )
    }

    companion object {
        // 싱글톤 인스턴스
        @Volatile
        private var INSTANCE: USDAssetRepository? = null

        fun getInstance(): USDAssetRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = USDAssetRepository()
                INSTANCE = instance
                instance
            }
        }
    }
}
