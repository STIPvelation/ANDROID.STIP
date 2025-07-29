package com.stip.stip.order.data

data class OrderParams(
    val limitPriceStr: String?,
    val quantityOrTotalStr: String?,
    val triggerPriceStr: String?,
    val isMarketOrder: Boolean,
    val isReservedOrder: Boolean,
    val isInputModeTotalAmount: Boolean
) 