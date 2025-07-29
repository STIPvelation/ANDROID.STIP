package com.stip.ipasset.api

import com.stip.ipasset.model.WithdrawRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 출금 API 서비스 인터페이스
 */
interface WalletWithdrawService {
    /**
     * 암호화폐 출금 요청
     * @param withdrawRequest 출금 요청 데이터
     * @return 빈 응답 (성공 시 200 OK, 실패 시 에러)
     */
    @POST("api/wallet/withdraw")
    suspend fun withdrawCrypto(
        @Body withdrawRequest: WithdrawRequest
    ): Response<Unit>
} 