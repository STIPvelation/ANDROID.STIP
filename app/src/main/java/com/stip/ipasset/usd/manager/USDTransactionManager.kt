package com.stip.ipasset.usd.manager

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stip.ipasset.manager.AssetSyncManager
import com.stip.ipasset.manager.AssetSyncListener
import com.stip.ipasset.ticker.repository.IpAssetRepository
import com.stip.ipasset.usd.model.*
import com.stip.ipasset.usd.repository.USDAssetRepository
import java.text.SimpleDateFormat
import java.util.*

/**
 * USD 트랜잭션 관리자 - 모든 USD 관련 트랜잭션을 처리하고 자산을 동기화하는 클래스
 * 티커 수량, 입출금 내역, 및 USD 총 보유자산을 연동하여 관리
 */
class USDTransactionManager private constructor() : AssetSyncListener {

    // 트랜잭션 목록
    private val _depositTransactions = MutableLiveData<List<USDDepositTransaction>>(emptyList())
    val depositTransactions: LiveData<List<USDDepositTransaction>> = _depositTransactions
    
    private val _withdrawalTransactions = MutableLiveData<List<USDWithdrawalTransaction>>(emptyList())
    val withdrawalTransactions: LiveData<List<USDWithdrawalTransaction>> = _withdrawalTransactions
    
    private val _returnTransactions = MutableLiveData<List<USDReturnTransaction>>(emptyList())
    val returnTransactions: LiveData<List<USDReturnTransaction>> = _returnTransactions
    
    private val _processTransactions = MutableLiveData<List<USDProcessTransaction>>(emptyList())
    val processTransactions: LiveData<List<USDProcessTransaction>> = _processTransactions
    
    // 모든 트랜잭션 통합 뷰
    private val _allTransactions = MutableLiveData<List<USDTransaction>>(emptyList())
    val allTransactions: LiveData<List<USDTransaction>> = _allTransactions
    
    // 자산 동기화 매니저
    private val assetSyncManager = AssetSyncManager.getInstance()
    
    init {
        // 자산 동기화 리스너 등록
        assetSyncManager.registerSyncListener(this)
    }
    
    /**
     * AssetSyncListener 구현
     * 자산이 업데이트될 때 호출됨
     */
    override fun onAssetUpdated(usdBalance: Double) {
        // USD 자산이 변경되었을 때 UI 업데이트 등의 추가 작업 가능
    }
    
    /**
     * 입금 트랜잭션 추가
     */
    fun addDepositTransaction(context: Context, usdAmount: Double, krwAmount: Int): USDDepositTransaction {
        val usdRepository = USDAssetRepository.getInstance()
        val transaction = createDepositTransaction(usdAmount, krwAmount)
        
        // 트랜잭션 저장
        val currentTransactions = _depositTransactions.value?.toMutableList() ?: mutableListOf()
        currentTransactions.add(0, transaction) // 최신 트랜잭션을 맨 앞에 추가
        _depositTransactions.postValue(currentTransactions)
        
        // 통합 트랜잭션 뷰 업데이트
        updateAllTransactions()
        
        // 자산 업데이트
        usdRepository.processDeposit(usdAmount, context)
        
        return transaction
    }
    
    /**
     * 출금 트랜잭션 추가
     */
    fun addWithdrawalTransaction(context: Context, usdAmount: Double, krwAmount: Int): USDWithdrawalTransaction? {
        val usdRepository = USDAssetRepository.getInstance()
        
        // 출금 가능 여부 확인
        if (!usdRepository.processWithdrawal(usdAmount, context)) {
            return null
        }
        
        val transaction = createWithdrawalTransaction(usdAmount, krwAmount)
        
        // 트랜잭션 저장
        val currentTransactions = _withdrawalTransactions.value?.toMutableList() ?: mutableListOf()
        currentTransactions.add(0, transaction) // 최신 트랜잭션을 맨 앞에 추가
        _withdrawalTransactions.postValue(currentTransactions)
        
        // 통합 트랜잭션 뷰 업데이트
        updateAllTransactions()
        
        return transaction
    }
    
    /**
     * 모든 트랜잭션 데이터를 통합하여 업데이트
     */
    private fun updateAllTransactions() {
        val allTxs = mutableListOf<USDTransaction>()
        
        // 입금 트랜잭션 추가
        _depositTransactions.value?.forEach { deposit ->
            allTxs.add(USDTransaction(
                id = deposit.id,
                date = deposit.date,
                time = deposit.time,
                type = TransactionType.DEPOSIT,
                status = TransactionStatus.COMPLETED,
                amountUsd = deposit.usdAmount,
                amountKrw = deposit.krwAmount
            ))
        }
        
        // 출금 트랜잭션 추가
        _withdrawalTransactions.value?.forEach { withdrawal ->
            allTxs.add(USDTransaction(
                id = withdrawal.id,
                date = withdrawal.date,
                time = withdrawal.time,
                type = TransactionType.WITHDRAWAL,
                status = TransactionStatus.COMPLETED,
                amountUsd = withdrawal.usdAmount,
                amountKrw = withdrawal.krwAmount
            ))
        }
        
        // 날짜 기준 내림차순 정렬
        allTxs.sortByDescending { tx ->
            val dateStr = "${tx.date} ${tx.time}"
            val format = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
            format.parse(dateStr)
        }
        
        _allTransactions.postValue(allTxs)
    }
    
    /**
     * 동기화 - IP 내역의 티커 수량, 입출금 내역, USD 자산 연동
     */
    fun syncAllAssets(context: Context) {
        val usdRepository = USDAssetRepository.getInstance()
        usdRepository.syncWithIpAssets(context)
    }
    
    /**
     * 새 입금 트랜잭션 생성
     */
    private fun createDepositTransaction(usdAmount: Double, krwAmount: Int): USDDepositTransaction {
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        return USDDepositTransaction(
            id = UUID.randomUUID().toString(),
            date = dateFormat.format(today.time),
            time = timeFormat.format(today.time),
            status = "입금 완료",
            usdAmount = usdAmount,
            krwAmount = krwAmount,
            txHash = "tx_${System.currentTimeMillis()}"
        )
    }
    
    /**
     * 새 출금 트랜잭션 생성
     */
    private fun createWithdrawalTransaction(usdAmount: Double, krwAmount: Int): USDWithdrawalTransaction {
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        return USDWithdrawalTransaction(
            id = UUID.randomUUID().toString(),
            date = dateFormat.format(today.time),
            time = timeFormat.format(today.time),
            status = "출금 완료",
            usdAmount = usdAmount,
            krwAmount = krwAmount,
            txHash = "tx_${System.currentTimeMillis()}"
        )
    }
    
    companion object {
        @Volatile
        private var INSTANCE: USDTransactionManager? = null

        fun getInstance(): USDTransactionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = USDTransactionManager()
                INSTANCE = instance
                instance
            }
        }
    }
}
