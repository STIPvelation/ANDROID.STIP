package com.stip.api.service

import com.stip.api.model.PortfolioResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 포트폴리오 관련 API 서비스 인터페이스
 * 서버에서 사용자의 포트폴리오 데이터를 조회하기 위한 API 정의
 */
interface PortfolioService {
    /**
     * 사용자의 포트폴리오 데이터 조회
     * @param userId 회원 ID
     * @return 포트폴리오 데이터 응답
     */
    @GET("api/wallet/asset/portfolio")
    suspend fun getPortfolio(
        @Query("userId") userId: String
    ): PortfolioResponse
} 