package com.stip.dummy

import android.util.Log

/**
 * 확장된 더미 트랜잭션 데이터 관리 객체
 * 기존 Transaction 모델과 새로운 USD/Ticker 트랜잭션 모델 모두 관리
 */
object TransactionDummyDataExtended {
    // USD 더미 트랜잭션 데이터
    private val usdTransactions = mutableListOf<Any>()
    
    // 티커 더미 트랜잭션 데이터
    private val tickerTransactions = mutableMapOf<String, MutableList<Any>>()
    
    init {
        // 초기 더미 데이터 생성
        createUSDDummyData()
        createTickerDummyData("BTC")
        createTickerDummyData("ETH")
    }
    
    /**
     * USD 더미 데이터 생성
     */
    private fun createUSDDummyData() {
        // 더미 데이터 목록 초기화
        usdTransactions.clear()
        
        // 입금 완료 더미 데이터
        usdTransactions.add(USDDepositTransaction(
            id = 1001,
            amount = 0.37,
            amountKrw = 500,
            timestamp = 1719014640, // 2025.06.11 10:24
            status = "입금 완료",
            txHash = "0x123abc456def789",
            exchangeRate = 1350.0
        ))
        
        usdTransactions.add(USDDepositTransaction(
            id = 1003,
            amount = 0.22,
            amountKrw = 300,
            timestamp = 1718755920, // 2025.06.07 14:32
            status = "입금 완료",
            txHash = "0x789def456abc123",
            exchangeRate = 1365.0
        ))
        
        // 출금 완료 더미 데이터
        usdTransactions.add(USDWithdrawalTransaction(
            id = 1002,
            amount = 0.15,
            amountKrw = 200,
            timestamp = 1718928180, // 2025.06.10 16:03
            status = "출금 완료",
            txHash = "0xabc123def456789",
            exchangeRate = 1333.0,
            recipientAddress = "0xUserWalletAddress123456789",
            fee = 0.001
        ))
    }
    
    /**
     * 티커 더미 데이터 생성
     */
    private fun createTickerDummyData(tickerSymbol: String) {
        // 해당 티커에 대한 더미 데이터 목록 초기화
        val transactions = mutableListOf<Any>()
        
        // 입금 완료 더미 데이터
        transactions.add(TickerDepositTransaction(
            id = 1001,
            tickerAmount = 10.37,
            tickerSymbol = tickerSymbol,
            usdAmount = 5000.0,
            timestamp = 1719014640, // 2025.06.11 10:24
            status = "입금 완료",
            txHash = "0x123abc456def789"
        ))
        
        transactions.add(TickerDepositTransaction(
            id = 1003,
            tickerAmount = 20.22,
            tickerSymbol = tickerSymbol,
            usdAmount = 3000.0,
            timestamp = 1718755920, // 2025.06.07 14:32
            status = "입금 완료",
            txHash = "0x789def456abc123"
        ))
        
        // 출금 완료 더미 데이터
        transactions.add(TickerWithdrawalTransaction(
            id = 1002,
            tickerAmount = 15.15,
            tickerSymbol = tickerSymbol,
            usdAmount = 2000.0,
            timestamp = 1718928180, // 2025.06.10 16:03
            status = "출금 완료",
            txHash = "0xabc123def456789",
            recipientAddress = "0xUserWalletAddress123456789",
            fee = 0.001
        ))
        
        // 맵에 저장
        tickerTransactions[tickerSymbol] = transactions
    }
    
    /**
     * USD 트랜잭션 데이터 가져오기
     */
    fun getUSDTransactions(): List<Any> {
        return usdTransactions.toList()
    }
    
    /**
     * USD 입금 트랜잭션만 가져오기
     */
    fun getUSDDepositTransactions(): List<USDDepositTransaction> {
        return usdTransactions.filterIsInstance<USDDepositTransaction>()
    }
    
    /**
     * USD 출금 트랜잭션만 가져오기
     */
    fun getUSDWithdrawalTransactions(): List<USDWithdrawalTransaction> {
        return usdTransactions.filterIsInstance<USDWithdrawalTransaction>()
    }
    
    /**
     * 특정 티커의 모든 트랜잭션 가져오기
     */
    fun getTickerTransactions(tickerSymbol: String): List<Any> {
        return tickerTransactions[tickerSymbol]?.toList() ?: emptyList()
    }
    
    /**
     * 특정 티커의 입금 트랜잭션만 가져오기
     */
    fun getTickerDepositTransactions(tickerSymbol: String): List<TickerDepositTransaction> {
        return tickerTransactions[tickerSymbol]?.filterIsInstance<TickerDepositTransaction>() ?: emptyList()
    }
    
    /**
     * 특정 티커의 출금 트랜잭션만 가져오기
     */
    fun getTickerWithdrawalTransactions(tickerSymbol: String): List<TickerWithdrawalTransaction> {
        return tickerTransactions[tickerSymbol]?.filterIsInstance<TickerWithdrawalTransaction>() ?: emptyList()
    }
    
    /**
     * USD 트랜잭션 추가 (새 트랜잭션 기록)
     */
    fun addUSDTransaction(transaction: Any) {
        when (transaction) {
            is USDDepositTransaction, is USDWithdrawalTransaction -> {
                usdTransactions.add(transaction)
                Log.d("TransactionDummyData", "USD 트랜잭션 추가됨")
            }
        }
    }
    
    /**
     * 티커 트랜잭션 추가 (새 트랜잭션 기록)
     */
    fun addTickerTransaction(transaction: Any) {
        when (transaction) {
            is TickerDepositTransaction -> {
                val symbol = transaction.tickerSymbol
                if (!tickerTransactions.containsKey(symbol)) {
                    tickerTransactions[symbol] = mutableListOf()
                }
                tickerTransactions[symbol]?.add(transaction)
                Log.d("TransactionDummyData", "${symbol} 입금 트랜잭션 추가됨")
            }
            is TickerWithdrawalTransaction -> {
                val symbol = transaction.tickerSymbol
                if (!tickerTransactions.containsKey(symbol)) {
                    tickerTransactions[symbol] = mutableListOf()
                }
                tickerTransactions[symbol]?.add(transaction)
                Log.d("TransactionDummyData", "${symbol} 출금 트랜잭션 추가됨")
            }
        }
    }
}
