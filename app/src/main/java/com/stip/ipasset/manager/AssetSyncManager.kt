package com.stip.ipasset.manager

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stip.ipasset.model.IpAsset
import com.stip.ipasset.ticker.repository.IpAssetRepository
import com.stip.ipasset.usd.model.USDAssetData
import com.stip.ipasset.usd.repository.USDAssetRepository

/**
 * 자산 동기화 리스너 인터페이스 - 자산 변경시 UI 업데이트를 위해 사용
 */
interface AssetSyncListener {
    fun onAssetUpdated(usdBalance: Double)
}

/**
 * AssetSyncManager - 티커 수량, 입출금 내역, USD 자산을 동기화하는 매니저 클래스
 * 싱글턴 패턴으로 구현
 */
class AssetSyncManager private constructor() {

    // 자산 변경 이벤트를 알리기 위한 LiveData
    private val _assetSyncEvent = MutableLiveData<AssetSyncEvent>()
    val assetSyncEvent: LiveData<AssetSyncEvent> = _assetSyncEvent
    
    // 자산 동기화 리스너 목록
    private val syncListeners = mutableListOf<AssetSyncListener>()
    
    /**
     * 자산 동기화 리스너 등록
     */
    fun registerSyncListener(listener: AssetSyncListener) {
        if (!syncListeners.contains(listener)) {
            syncListeners.add(listener)
        }
    }
    
    /**
     * 자산 동기화 리스너 제거
     */
    fun unregisterSyncListener(listener: AssetSyncListener) {
        syncListeners.remove(listener)
    }
    
    /**
     * 모든 리스너에게 자산 업데이트 알림
     */
    private fun notifyListeners(usdBalance: Double) {
        syncListeners.forEach { it.onAssetUpdated(usdBalance) }
    }

    /**
     * USD 자산 업데이트와 함께 관련 티커 자산도 함께 업데이트
     */
    fun syncAssets(usdRepository: USDAssetRepository, ipRepository: IpAssetRepository, newUsdData: USDAssetData) {
        // USD 자산 저장소 업데이트
        usdRepository.updateAssetData(newUsdData)
        
        // 티커 자산 저장소의 USD 티커 업데이트
        val usdTicker = ipRepository.getAsset("USD")
        if (usdTicker != null) {
            ipRepository.updateAsset("USD", newUsdData.balance)
        } else {
            // USD 티커가 없으면 새로 생성 (실제 앱에서는 ID 생성 로직이 필요할 수 있음)
            val newUsdTicker = IpAsset(
                id = "USD",
                name = "US Dollar",
                ticker = "USD",
                balance = newUsdData.balance,
                value = newUsdData.balance,
                currencyCode = "USD",
                amount = newUsdData.balance
            )
            ipRepository.addAsset(newUsdTicker)
        }

        // 자산 동기화 이벤트 발생
        _assetSyncEvent.postValue(AssetSyncEvent(newUsdData.balance))
        
        // 리스너에게 알림
        notifyListeners(newUsdData.balance)
    }

    /**
     * 입출금 처리 후 자산 업데이트
     * @param usdRepository USD 자산 저장소
     * @param ipRepository IP 자산 저장소
     * @param amount 처리된 금액 (입금은 양수, 출금은 음수)
     */
    fun processTransactionAndUpdateAssets(
        usdRepository: USDAssetRepository, 
        ipRepository: IpAssetRepository,
        amount: Double
    ): Boolean {
        val currentUsdData = usdRepository.getCurrentAssetData()
        
        // 출금인 경우 잔액 확인
        if (amount < 0 && Math.abs(amount) > currentUsdData.withdrawable) {
            return false
        }
        
        // 새 잔액 계산
        val newBalance = currentUsdData.balance + amount
        val newWithdrawable = if (amount < 0) {
            currentUsdData.withdrawable + amount
        } else {
            currentUsdData.withdrawable + amount
        }
        
        // USD 자산 데이터 업데이트
        val updatedUsdData = currentUsdData.copy(
            balance = newBalance,
            withdrawable = newWithdrawable
        )
        
        // 모든 자산 동기화
        syncAssets(usdRepository, ipRepository, updatedUsdData)
        
        return true
    }
    
    /**
     * 자산 동기화 이벤트 데이터 클래스
     */
    data class AssetSyncEvent(val usdBalance: Double)

    companion object {
        @Volatile
        private var INSTANCE: AssetSyncManager? = null

        fun getInstance(): AssetSyncManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AssetSyncManager()
                INSTANCE = instance
                instance
            }
        }
    }
}
