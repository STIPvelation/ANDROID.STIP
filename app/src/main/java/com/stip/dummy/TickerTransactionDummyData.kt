package com.stip.dummy

import com.stip.ipasset.ticker.model.TickerDepositTransaction
import com.stip.ipasset.ticker.model.TickerWithdrawalTransaction
import java.util.*

/**
 * 티커 거래 내역 더미 데이터 제공 클래스
 */
//object TickerTransactionDummyData {
//
//    // 티커 입금 내역 더미 데이터 생성
//    fun getTickerDepositTransactions(tickerSymbol: String): List<TickerDepositTransaction> {
//        val currentTime = System.currentTimeMillis() / 1000
//        val oneDay = 86400L
//
//        return listOf(
//            TickerDepositTransaction(
//                id = 1,
//                tickerAmount = 500.0,
//                tickerSymbol = tickerSymbol,
//                usdAmount = 0.37,
//                timestamp = currentTime - oneDay,
//                status = "입금 완료"
//            ),
//            TickerDepositTransaction(
//                id = 2,
//                tickerAmount = 1000.0,
//                tickerSymbol = tickerSymbol,
//                usdAmount = 0.74,
//                timestamp = currentTime - oneDay * 2,
//                status = "입금 완료"
//            ),
//            TickerDepositTransaction(
//                id = 3,
//                tickerAmount = 780.0,
//                tickerSymbol = tickerSymbol,
//                usdAmount = 0.58,
//                timestamp = currentTime - oneDay * 7,
//                status = "입금 완료"
//            ),
//        )
//    }

    // 티커 출금 내역 더미 데이터 생성
//    fun getTickerWithdrawalTransactions(tickerSymbol: String): List<TickerWithdrawalTransaction> {
//        val currentTime = System.currentTimeMillis() / 1000
//        val oneDay = 86400L
//
//        return listOf(
//            TickerWithdrawalTransaction(
//                id = 4,
//                tickerAmount = 200.0,
//                tickerSymbol = tickerSymbol,
//                usdAmount = 0.15,
//                timestamp = currentTime - oneDay * 3,
//                status = "출금 완료",
//                recipientAddress = "0x8d4f7d84c65f289f5b9a2b69df8eb5fc116c2210ecabc229b79429a12f24",
//                fee = 0.01
//            ),
//            TickerWithdrawalTransaction(
//                id = 5,
//                tickerAmount = 350.0,
//                tickerSymbol = tickerSymbol,
//                usdAmount = 0.26,
//                timestamp = currentTime - oneDay * 5,
//                status = "출금 완료",
//                recipientAddress = "0x6e2f7d84c65f289f5b9a2b69df8eb5fc116c2210ecabc229b79429a12f24",
//                fee = 0.01
//            ),
//        )
//    }

    // 모든 티커 거래 내역 통합 (입금 + 출금)
//    fun getAllTransactions(tickerSymbol: String): List<Any> {
//        val deposits = getTickerDepositTransactions(tickerSymbol)
//        val withdrawals = getTickerWithdrawalTransactions(tickerSymbol)
//
//        val allTransactions = mutableListOf<Any>().apply {
//            addAll(deposits)
//            addAll(withdrawals)
//        }
//
//        // 타임스탬프 기준 내림차순 정렬 (최신 거래가 상단에 표시)
//        return allTransactions.sortedWith(compareByDescending {
//            when (it) {
//                is TickerDepositTransaction -> it.timestamp
//                is TickerWithdrawalTransaction -> it.timestamp
//                else -> 0L
//            }
//        })
//    }
//}
