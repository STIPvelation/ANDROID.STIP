package com.stip.stip.signup.customview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stip.stip.R

class TermsBottomSheet(
    private val termsTitle: String,
    private val detailContent: String
) : BottomSheetDialogFragment() {

    // ✅ 바텀시트에 흰 배경 테마 적용
    override fun getTheme(): Int = R.style.BottomSheetWhiteTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.bottom_dialog_terms, container, false)
        val closeImage = view.findViewById<AppCompatImageView>(R.id.iv_close)
        val title = view.findViewById<AppCompatTextView>(R.id.tv_terms_title)
        val content = view.findViewById<AppCompatTextView>(R.id.tv_terms_content)

        title.text = termsTitle
        content.text = detailContent

        closeImage.setOnClickListener {
            dismiss()
        }

        return view
    }
}
