package com.stip.stip.iphome.util // 실제 패키지 경로 확인

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import com.google.android.material.tabs.TabLayout
import com.stip.stip.iphome.fragment.InfoDialogFragment // InfoDialogFragment 경로 확인 및 import
import java.text.DecimalFormat
import java.util.Locale

object OrderUtils {

    val fixedTwoDecimalFormatter = DecimalFormat("#,##0.00").apply {
        decimalFormatSymbols =
            decimalFormatSymbols.apply { groupingSeparator = ','; decimalSeparator = '.' }
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        roundingMode = java.math.RoundingMode.DOWN
    }
    val numberParseFormat = DecimalFormat.getNumberInstance(Locale.US) as DecimalFormat


    fun formatNumberWithCommas(numberString: String?): String {
        return try {
            if (numberString.isNullOrBlank()) "0.00" else fixedTwoDecimalFormatter.format(
                numberString.replace(Regex("[^\\d.-]"), "").toDouble()
            )
        } catch (e: Exception) {
            "0.00"
        }
    }

    fun hideKeyboard(context: Context?, view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun showToast(context: Context?, message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_LONG).show()
        }
    }

    fun showErrorDialog(
        fragmentManager: FragmentManager,
        @StringRes titleResId: Int,
        message: String,
        @ColorRes titleColorResId: Int
    ) {
        if (fragmentManager.isStateSaved) {
            Log.w("OrderUtils", "Cannot show error dialog: FragmentManager state already saved.")
            return
        }
        try {
            InfoDialogFragment.newInstance(titleResId, message, titleColorResId)
                .show(fragmentManager, InfoDialogFragment.TAG)
        } catch (e: IllegalStateException) {
            Log.e("OrderUtils", "Error showing dialog: ${e.message}")
        } catch (e: Exception) {
            Log.e("OrderUtils", "Unexpected error showing dialog", e)
        }
    }

    fun setTabTextColor(context: Context?, tab: TabLayout.Tab?, color: Int) {
        if (tab == null || context == null) return
        val tv = tab.view as? ViewGroup
        tv?.let { vg ->
            for (i in 0 until vg.childCount) {
                (vg.getChildAt(i) as? TextView)?.setTextColor(color)
            }
        }
    }
}