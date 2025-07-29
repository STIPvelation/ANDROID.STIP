package com.stip.api.repository

import com.stip.api.model.WalletHistoryRecord
import com.stip.api.service.WalletHistoryService
import com.stip.stip.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WalletHistoryRepository {
    private val walletHistoryService: WalletHistoryService by lazy {
        RetrofitClient.createTapiService(WalletHistoryService::class.java)
    }

    /**
     * 지갑 거래 내역 조회
     * @param authorization Bearer 토큰
     * @param marketPairId 마켓 페어 ID (옵션)
     * @param type 거래 종류 (옵션) - DEPOSIT(입금) / WITHDRAW(출금)
     * @param status 거래 상태 (옵션) - REQUEST(진행 중) / REJECTED(반환)
     * @return 지갑 거래 내역 목록
     */
    suspend fun getWalletHistory(
        authorization: String,
        marketPairId: String? = null,
        type: String? = null, 
        status: String? = null
    ): List<WalletHistoryRecord> = withContext(Dispatchers.IO) {
        try {
            walletHistoryService.getWalletHistory(
                authorization = authorization,
                marketPairId = marketPairId,
                type = type,
                status = status
            )
        } catch (e: Exception) {
            emptyList()
        }
    }
} 