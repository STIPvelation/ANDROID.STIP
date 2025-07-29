package com.stip.stip.api.model

/**
 * API 응답의 표준 래퍼 클래스
 * 서버로부터 받은 응답을 처리하기 위한 공통 구조
 */
data class ApiResponse<T>(
    val name: String,
    val success: Boolean,
    val data: T?,
    val message: String?,
    val errorCode: String?
) {

    fun isSuccess(): Boolean = success && data != null
    
    inline fun onSuccess(action: (T) -> Unit): ApiResponse<T> {
        if (isSuccess() && data != null) {
            action(data)
        }
        return this
    }
    
    inline fun onError(action: (String?) -> Unit): ApiResponse<T> {
        if (!isSuccess()) {
            action(message)
        }
        return this
    }
}
