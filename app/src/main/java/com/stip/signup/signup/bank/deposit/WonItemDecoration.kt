package com.stip.stip.signup.signup.bank.deposit

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R

class WonItemDecoration(
    val context: Context
): RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val position = parent.getChildAdapterPosition(view)
        val count = state.itemCount
        val offSet = context.resources.getDimension(R.dimen.margin_6dp).toInt()

        when (position) {
            count - 1 -> {

            }
            else -> {
                outRect.right = offSet
            }
        }
    }

}