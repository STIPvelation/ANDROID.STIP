package com.stip.stip.signup.keypad

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * A custom GridLayoutManager that overrides the spacing calculation to create a more compact grid
 */
class CompactGridLayoutManager : GridLayoutManager {
    
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : 
        super(context, attrs, defStyleAttr, defStyleRes)
    
    constructor(context: Context, spanCount: Int) : super(context, spanCount)
    
    constructor(context: Context, spanCount: Int, orientation: Int, reverseLayout: Boolean) : 
        super(context, spanCount, orientation, reverseLayout)
    
    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        // Make sure to call the super implementation first
        super.onLayoutChildren(recycler, state)
    }
    
    // Override to change spacing behavior
    override fun supportsPredictiveItemAnimations(): Boolean {
        return false // Disable predictive animations to avoid spacing issues
    }
}
