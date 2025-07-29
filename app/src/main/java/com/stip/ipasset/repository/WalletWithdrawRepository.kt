package com.stip.ipasset.repository

import com.stip.ipasset.model.WithdrawRequest
import com.stip.ipasset.model.WithdrawResponse

/**
 * 출금 Repository 인터페이스
 */
interface WalletWithdrawRepository {
    /**
     * 암호화폐 출금 요청
     * @param withdrawRequest 출금 요청 데이터
     * @return Result<WithdrawResponse>
     */
    suspend fun withdrawCrypto(withdrawRequest: WithdrawRequest): Result<WithdrawResponse>
} 