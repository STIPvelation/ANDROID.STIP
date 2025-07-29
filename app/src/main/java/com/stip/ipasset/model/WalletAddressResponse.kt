package com.stip.ipasset.model

import com.google.gson.annotations.SerializedName

data class WalletAddressResponse(
    @SerializedName("wallet_address")
    val walletAddress: String
) 