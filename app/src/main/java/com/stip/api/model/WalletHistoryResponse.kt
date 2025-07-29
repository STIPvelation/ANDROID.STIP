package com.stip.api.model

import com.google.gson.annotations.SerializedName

// 입출금 내역(거래 내역) 단일 레코드
data class WalletHistoryRecord(
    @SerializedName("id") val id: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("type") val type: String, // DEPOSIT(입금), WITHDRAW(출금)
    @SerializedName("requestAmount") val requestAmount: Double,
    @SerializedName("amount") val amount: Double,
    @SerializedName("realAmount") val realAmount: Double,
    @SerializedName("fee") val fee: Double,
    @SerializedName("feeValue") val feeValue: Double,
    @SerializedName("status") val status: String, // REQUEST(진행 중), REJECTED(반환)
    @SerializedName("toAddress") val toAddress: String?,
    @SerializedName("fromAddress") val fromAddress: String?,
    @SerializedName("transactionId") val transactionId: String?,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("walletIpId") val walletIpId: String,
    @SerializedName("marketPairId") val marketPairId: String,
    @SerializedName("marketPairSymbol") val marketPairSymbol: String,
    @SerializedName("marketPairBaseAsset") val marketPairBaseAsset: String,
    @SerializedName("marketPairName") val marketPairName: String,
    @SerializedName("usdAmount") val usdAmount: Double,
    @SerializedName("member") val member: MemberInfo?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("deletedAt") val deletedAt: String?
)

// 회원 정보
data class MemberInfo(
    @SerializedName("id") val id: String,
    @SerializedName("di") val di: String,
    @SerializedName("email") val email: String,
    @SerializedName("name") val name: String,
    @SerializedName("englishFirstName") val englishFirstName: String,
    @SerializedName("englishLastName") val englishLastName: String,
    @SerializedName("pin") val pin: String,
    @SerializedName("birthdate") val birthdate: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("telecomProvider") val telecomProvider: String,
    @SerializedName("phoneNumber") val phoneNumber: String,
    @SerializedName("isPhoneNumberVerified") val isPhoneNumberVerified: Boolean,
    @SerializedName("address") val address: String,
    @SerializedName("addressDetail") val addressDetail: String,
    @SerializedName("postalCode") val postalCode: String,
    @SerializedName("bankCode") val bankCode: String,
    @SerializedName("accountNumber") val accountNumber: String,
    @SerializedName("isDirectAccount") val isDirectAccount: Boolean,
    @SerializedName("usagePurpose") val usagePurpose: String,
    @SerializedName("sourceOfFunds") val sourceOfFunds: String,
    @SerializedName("job") val job: String,
    @SerializedName("memberRole") val memberRole: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("modifiedAt") val modifiedAt: String
)

data class WalletHistoryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: WalletHistoryData
)

data class WalletHistoryData(
    @SerializedName("record") val record: List<WalletHistoryRecord>,
    @SerializedName("pagination") val pagination: Pagination
)

data class Pagination(
    @SerializedName("currentPage") val currentPage: Int,
    @SerializedName("limit") val limit: Int,
    @SerializedName("totalRecords") val totalRecords: Int,
    @SerializedName("totalPages") val totalPages: Int
) 