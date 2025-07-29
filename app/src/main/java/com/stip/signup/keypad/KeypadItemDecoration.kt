package com.stip.stip.signup.keypad

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Custom item decoration for keypad to reduce horizontal spacing between buttons
 */
class KeypadItemDecoration : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        
        // Apply negative horizontal spacing to bring buttons closer
        outRect.left = -15
        outRect.right = -15
    }
}
