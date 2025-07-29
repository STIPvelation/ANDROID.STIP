package com.stip.dummy

import com.stip.ipasset.usd.model.TransactionStatus
import com.stip.ipasset.usd.model.TransactionType
import com.stip.ipasset.usd.model.USDTransaction

/**
 * USD 트랜잭션 더미 데이터 제공 클래스
 */
object USDTransactionDummyData {

    /**
     * 모든 트랜잭션 데이터를 가져옵니다.
     */
    fun getAllTransactions(): List<USDTransaction> {
        return listOf(
            *getDepositTransactions().toTypedArray(),
            *getWithdrawalTransactions().toTypedArray(),
            *getReturnTransactions().toTypedArray(),
            *getInProgressTransactions().toTypedArray()
        ).sortedByDescending { it.date + it.time }
    }

    /**
     * 입금 트랜잭션만 가져옵니다.
     */
    fun getDepositTransactions(): List<USDTransaction> {
        return listOf(
            USDTransaction(
                id = "dep_001",
                date = "2025.06.29",
                time = "10:24",
                type = TransactionType.DEPOSIT,
                status = TransactionStatus.COMPLETED,
                amountUsd = 0.37,
                amountKrw = 500
            ),
            USDTransaction(
                id = "dep_002",
                date = "2025.06.27",
                time = "14:05",
                type = TransactionType.DEPOSIT,
                status = TransactionStatus.COMPLETED,
                amountUsd = 1.85,
                amountKrw = 2500
            ),
            USDTransaction(
                id = "dep_003",
                date = "2025.06.23",
                time = "09:33",
                type = TransactionType.DEPOSIT,
                status = TransactionStatus.COMPLETED,
                amountUsd = 3.70,
                amountKrw = 5000
            )
        )
    }

    /**
     * 출금 트랜잭션만 가져옵니다.
     */
    fun getWithdrawalTransactions(): List<USDTransaction> {
        return listOf(
            USDTransaction(
                id = "with_001",
                date = "2025.06.28",
                time = "16:03",
                type = TransactionType.WITHDRAWAL,
                status = TransactionStatus.COMPLETED,
                amountUsd = 0.15,
                amountKrw = 200
            ),
            USDTransaction(
                id = "with_002",
                date = "2025.06.25",
                time = "11:47",
                type = TransactionType.WITHDRAWAL,
                status = TransactionStatus.COMPLETED,
                amountUsd = 0.74,
                amountKrw = 1000
            )
        )
    }

    /**
     * 반환 트랜잭션만 가져옵니다.
     */
    fun getReturnTransactions(): List<USDTransaction> {
        return listOf(
            USDTransaction(
                id = "ret_001",
                date = "2025.06.26",
                time = "15:22",
                type = TransactionType.RETURN,
                status = TransactionStatus.RETURNED,
                amountUsd = 0.22,
                amountKrw = 300
            )
        )
    }

    /**
     * 진행중 트랜잭션만 가져옵니다.
     */
    fun getInProgressTransactions(): List<USDTransaction> {
        return listOf(
            USDTransaction(
                id = "prog_001",
                date = "2025.06.29",
                time = "08:55",
                type = TransactionType.DEPOSIT,
                status = TransactionStatus.IN_PROGRESS,
                amountUsd = 0.56,
                amountKrw = 750
            ),
            USDTransaction(
                id = "prog_002",
                date = "2025.06.28",
                time = "19:13",
                type = TransactionType.WITHDRAWAL,
                status = TransactionStatus.IN_PROGRESS,
                amountUsd = 0.37,
                amountKrw = 500
            )
        )
    }
}