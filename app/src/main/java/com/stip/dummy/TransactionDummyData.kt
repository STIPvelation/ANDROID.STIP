package com.stip.dummy

import java.util.Date

/**
 * 입출금 거래 내역 더미 데이터 클래스
 */
data class Transaction(
    val id: Long,
    val currencyCode: String,
    val amount: Double,
    val fee: Double,
    val type: TransactionType,
    val status: TransactionStatus,
    val address: String?,
    val date: Date,
    val confirmations: Int = 0
)

enum class TransactionType {
    DEPOSIT, WITHDRAWAL
}

enum class TransactionStatus {
    PENDING, COMPLETED, FAILED
}

/**
 * 입출금 거래 내역 더미 데이터 관리 객체
 */
object TransactionDummyData {
    
    // 거래 내역 더미 데이터
    private val transactions = mutableListOf<Transaction>()
    
    init {
        // 초기 더미 데이터 생성
        val currentTime = System.currentTimeMillis()
        
        // 입금 더미 데이터
        transactions.add(
            Transaction(
                id = 1,
                currencyCode = "JWV",
                amount = 100.0,
                fee = 0.0,
                type = TransactionType.DEPOSIT,
                status = TransactionStatus.COMPLETED,
                address = "0x1234567890abcdef",
                date = Date(currentTime - 86400000), // 하루 전
                confirmations = 6
            )
        )
        
        // 출금 더미 데이터
        transactions.add(
            Transaction(
                id = 2,
                currencyCode = "MDM",
                amount = 50.0,
                fee = 1.0,
                type = TransactionType.WITHDRAWAL,
                status = TransactionStatus.COMPLETED,
                address = "0xabcdef1234567890",
                date = Date(currentTime - 43200000), // 12시간 전
                confirmations = 6
            )
        )
    }
    
    /**
     * 모든 거래 내역 반환
     */
    fun getAllTransactions(): List<Transaction> {
        return transactions.toList()
    }
    
    /**
     * 특정 통화의 거래 내역만 반환
     */
    fun getTransactionsByCurrency(currencyCode: String): List<Transaction> {
        return transactions.filter { it.currencyCode == currencyCode }
    }
    
    /**
     * 새 거래 내역 추가
     */
    fun addTransaction(transaction: Transaction) {
        transactions.add(transaction)
    }
}
