package com.stip.stip.api.service

import com.stip.stip.api.model.ApiResponse
import com.stip.stip.api.model.IpListingResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * IP 리스팅 관련 API 서비스 인터페이스
 * 백엔드 서버에서 IP 리스팅 데이터를 조회하기 위한 API 정의
 */
interface IpListingService {
    
    /**
     * IP 리스팅 데이터 조회
     * @return IP 리스팅 데이터 응답
     */
    @GET("api/ip/listing")
    suspend fun getIpListing(): ApiResponse<IpListingResponse>
    
    /**
     * 특정 카테고리의 IP 리스팅 데이터 조회
     * @param category 조회할 카테고리
     * @return 카테고리별 IP 리스팅 데이터 응답
     */
    @GET("api/ip/listing/category")
    suspend fun getIpListingByCategory(@Query("category") category: String): ApiResponse<IpListingResponse>
    
    /**
     * IP 상세 정보 조회
     * @param ticker IP 티커명
     * @return IP 상세 정보 응답
     */
    @GET("api/ip/detail")
    suspend fun getIpDetail(@Query("ticker") ticker: String): ApiResponse<IpListingResponse>
}
