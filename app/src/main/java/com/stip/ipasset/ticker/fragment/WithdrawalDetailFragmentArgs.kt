package com.stip.ipasset.ticker.fragment

import android.os.Bundle
import androidx.navigation.NavArgs
import kotlin.String
import kotlin.jvm.JvmStatic

data class WithdrawalDetailFragmentArgs(val transactionId: String) : NavArgs {
    companion object {
        @JvmStatic
        fun fromBundle(bundle: Bundle): WithdrawalDetailFragmentArgs {
            bundle.classLoader = WithdrawalDetailFragmentArgs::class.java.classLoader
            val __transactionId = bundle.getString("transactionId") ?: ""
            return WithdrawalDetailFragmentArgs(__transactionId)
        }
    }
}
