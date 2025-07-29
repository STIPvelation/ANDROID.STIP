package com.stip.stip.signup.keypad

import com.stip.stip.R

data class KeypadItem(
    val value: String,
    val type: KeypadType,
    val iconRes: Int? = null
) {
    companion object {
        fun default(): MutableList<KeypadItem> {
            val numberItems = (0..9).map { KeypadItem(it.toString(), KeypadType.NUMBER) }.shuffled()
            val fixedItems = listOf(
                KeypadItem("재배열", KeypadType.SHUFFLE),
                numberItems.first(),
                KeypadItem("", KeypadType.DELETE, R.drawable.ic_del_white_31dp)
            )
            return (numberItems.drop(1) + fixedItems).toMutableList()
        }
    }
}
