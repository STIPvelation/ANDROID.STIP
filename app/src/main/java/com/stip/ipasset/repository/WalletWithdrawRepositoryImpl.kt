package com.stip.ipasset.repository

import android.util.Log
import com.stip.ipasset.api.WalletWithdrawService
import com.stip.ipasset.model.WithdrawRequest
import com.stip.ipasset.model.WithdrawResponse
import retrofit2.HttpException
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 출금 Repository 구현
 */
@Singleton
class WalletWithdrawRepositoryImpl @Inject constructor(
    private val walletWithdrawService: WalletWithdrawService
) : WalletWithdrawRepository {

    companion object {
        private const val TAG = "WalletWithdrawRepo"
    }

    override suspend fun withdrawCrypto(withdrawRequest: WithdrawRequest): Result<WithdrawResponse> {
        return try {
            
            val response = walletWithdrawService.withdrawCrypto(withdrawRequest)
            
            Log.d(TAG, "응답 수신: ${response.code()}")
            
            // HTTP 상태 코드 확인
            if (response.isSuccessful) {
                // 성공 응답 (빈 응답이어도 성공)
                Log.d(TAG, "출금 요청 성공 (빈 응답)")
                val successResponse = WithdrawResponse(
                    success = true,
                    message = "출금이 성공적으로 처리되었습니다."
                )
                Result.success(successResponse)
            } else {
                // HTTP 에러
                val errorCode = response.code()
                Log.e(TAG, "❌ HTTP 에러 발생: $errorCode")

                val errorMessage = when (errorCode) {
                    400 -> "잘못된 요청입니다."
                    401 -> "인증이 필요합니다."
                    403 -> "권한이 없습니다."
                    404 -> "Not Found"
                    500 -> "서버 에러입니다."
                    else -> "알 수 없는 서버 오류가 발생했습니다. (코드: $errorCode)"
                }

                Result.failure(Exception(errorMessage))
            }
        } catch (e: HttpException) {
            val errorCode = e.code()
            Log.e(TAG, "HTTP 에러 발생: $errorCode")
            
            val errorMessage = when (errorCode) {
                400 -> "잘못된 요청입니다. 입력 정보를 확인해주세요."
                401 -> "인증이 필요합니다. 다시 로그인해주세요."
                403 -> "권한이 없습니다."
                404 -> "출금 서비스를 찾을 수 없습니다."
                500 -> "서버 내부 오류가 발생했습니다."
                503 -> "서비스를 일시적으로 사용할 수 없습니다."
                else -> "알 수 없는 서버 오류가 발생했습니다. (코드: $errorCode)"
            }
            
            Result.failure(Exception(errorMessage))
        } catch (e: ConnectException) {
            Log.e(TAG, "네트워크 연결 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다. 네트워크 연결을 확인해주세요."))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "연결 시간 초과", e)
            Result.failure(Exception("연결 시간이 초과되었습니다. 잠시 후 다시 시도해주세요."))
        } catch (e: UnknownHostException) {
            Log.e(TAG, "호스트를 찾을 수 없음", e)
            Result.failure(Exception("서버 주소를 찾을 수 없습니다. 네트워크 연결을 확인해주세요."))
        } catch (e: Exception) {
            Log.e(TAG, "알 수 없는 오류 발생: ${e.message}", e)
            Result.failure(Exception("출금 요청 중 오류가 발생했습니다: ${e.message}"))
        }
    }
} 