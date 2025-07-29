package com.stip.stip.signup.keypad

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Custom item decoration for grid layouts with precise control over spacing
 */
class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val horizontalSpacing: Int,
    private val verticalSpacing: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view) // item position
        val column = position % spanCount // column index

        if (includeEdge) {
            // Set spacing for the left and right edges
            outRect.left = horizontalSpacing - column * horizontalSpacing / spanCount
            outRect.right = (column + 1) * horizontalSpacing / spanCount

            // Set spacing for the top edge
            if (position < spanCount) {
                outRect.top = verticalSpacing
            }
            outRect.bottom = verticalSpacing
        } else {
            // Set spacing between columns
            outRect.left = column * horizontalSpacing / spanCount
            outRect.right = horizontalSpacing - (column + 1) * horizontalSpacing / spanCount
            
            // Set bottom spacing
            if (position >= spanCount) {
                outRect.top = verticalSpacing
            }
        }
    }
}
