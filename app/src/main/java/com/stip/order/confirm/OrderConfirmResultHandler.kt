package com.stip.stip.order

import android.util.Log // For logging
import androidx.fragment.app.Fragment
import com.stip.stip.databinding.FragmentOrderContentBinding // Keep binding for now, might remove later
// Import ConfirmOrderDialogFragment to access constants
import com.stip.stip.iphome.fragment.ConfirmOrderDialogFragment
// import java.text.DecimalFormat // Removed, assuming data comes parsed from bundle

class OrderConfirmResultHandler(
    private val fragment: Fragment,
    private val binding: FragmentOrderContentBinding, // May become redundant
    // numberParseFormat removed
    // Updated callback signature to include price (nullable Double)
    private val onConfirmed: (isBuy: Boolean, quantity: Double, price: Double?) -> Unit
) {

    companion object {
        private const val TAG = "OrderConfirmResult" // Log tag
    }

    fun registerResultListener() {
        // Use constants from ConfirmOrderDialogFragment
        fragment.parentFragmentManager.setFragmentResultListener(
            ConfirmOrderDialogFragment.REQUEST_KEY, // Use Constant
            fragment.viewLifecycleOwner
        ) { requestKey, bundle ->
            Log.d(TAG, "Received result for request key: $requestKey")
            val confirmed = bundle.getBoolean(ConfirmOrderDialogFragment.RESULT_KEY_CONFIRMED) // Use Constant

            if (confirmed) {
                Log.d(TAG, "Order confirmed by user.")
                // Extract data directly from the bundle passed back by the dialog
                val isBuy = bundle.getBoolean(ConfirmOrderDialogFragment.RESULT_KEY_IS_BUY, false) // Provide default
                // Get quantity as Double, default to 0.0 if missing
                val quantity = bundle.getDouble(ConfirmOrderDialogFragment.RESULT_KEY_QUANTITY, 0.0)
                // Get price as Double? (use containsKey to check for null vs. missing)
                val price = if (bundle.containsKey(ConfirmOrderDialogFragment.RESULT_KEY_PRICE)) {
                    bundle.getDouble(ConfirmOrderDialogFragment.RESULT_KEY_PRICE)
                } else {
                    null // Handle cases where price might not be applicable or sent (e.g., some market orders)
                }

                Log.d(TAG, "Extracted from bundle: isBuy=$isBuy, quantity=$quantity, price=$price")

                if (quantity > 0) { // Basic validation on extracted quantity
                    // Call the callback with data from the bundle
                    onConfirmed(isBuy, quantity, price)
                } else {
                    Log.e(TAG, "Invalid quantity ($quantity) received from confirmation dialog.")
                    // Handle error case - maybe show a toast?
                }
            } else {
                Log.d(TAG, "Order cancelled by user.")
                // Handle cancellation if needed (e.g., unlock UI elements)
            }
        }
    }
}