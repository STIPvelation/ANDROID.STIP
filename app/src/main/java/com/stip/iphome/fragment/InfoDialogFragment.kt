package com.stip.stip.iphome.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.stip.stip.R
import com.stip.stip.databinding.DialogBuyErrorBinding

class InfoDialogFragment : DialogFragment() {

    private var _binding: DialogBuyErrorBinding? = null
    private val binding get() = _binding!!

    private var confirmClickListener: (() -> Unit)? = null // 추가: 확인 버튼 클릭 리스너

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogBuyErrorBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            val titleResId = it.getInt(ARG_TITLE_RES_ID, R.string.buy_order_error_title)
            val message = it.getString(ARG_MESSAGE) ?: "오류가 발생했습니다."
            val prefixColorResId = it.getInt(ARG_TITLE_COLOR_RES_ID, R.color.dialog_title_buy_error_red)

            val fullTitle = getString(titleResId)
            binding.textErrorMessage.text = message

            try {
                val prefixColor = ContextCompat.getColor(requireContext(), prefixColorResId)
                val defaultColor = ContextCompat.getColor(requireContext(), R.color.dialog_message_text)

                val spannableTitle = SpannableString(fullTitle)

                if (fullTitle.length >= 2) {
                    spannableTitle.setSpan(
                        ForegroundColorSpan(prefixColor),
                        0,
                        2,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    if (fullTitle.length > 2) {
                        spannableTitle.setSpan(
                            ForegroundColorSpan(defaultColor),
                            2,
                            fullTitle.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                } else {
                    spannableTitle.setSpan(
                        ForegroundColorSpan(defaultColor),
                        0,
                        fullTitle.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                binding.textErrorTitle.text = spannableTitle

            } catch (e: Exception) {
                Log.e(TAG, "Error applying spannable color to title", e)
                binding.textErrorTitle.text = fullTitle
            }
        }

        binding.buttonConfirm.setOnClickListener {
            confirmClickListener?.invoke() // 추가: 리스너 호출
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window ?: return
        val dpWidth = 360
        val dpHeight = 200
        val density = resources.displayMetrics.density
        val widthPx = (dpWidth * density).toInt()
        val heightPx = (dpHeight * density).toInt()
        window.setLayout(widthPx, heightPx)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setConfirmClickListener(listener: () -> Unit): InfoDialogFragment {
        this.confirmClickListener = listener
        return this
    }

    companion object {
        const val TAG = "InfoDialog"

        private const val ARG_TITLE_RES_ID = "arg_title_res_id"
        private const val ARG_MESSAGE = "arg_message"
        private const val ARG_TITLE_COLOR_RES_ID = "arg_title_color_res_id"

        fun newInstance(
            @StringRes titleResId: Int,
            message: String,
            @ColorRes titleColorResId: Int = R.color.dialog_title_buy_error_red
        ): InfoDialogFragment {
            val fragment = InfoDialogFragment()
            val args = Bundle().apply {
                putInt(ARG_TITLE_RES_ID, titleResId)
                putString(ARG_MESSAGE, message)
                putInt(ARG_TITLE_COLOR_RES_ID, titleColorResId)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
