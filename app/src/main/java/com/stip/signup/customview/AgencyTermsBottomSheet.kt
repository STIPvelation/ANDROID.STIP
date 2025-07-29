package com.stip.stip.signup.customview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stip.stip.R

class AgencyTermsBottomSheet(
    private val termsTitle: String,
    private val detailContent: List<String>
): BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.bottom_dialog_agency_terms, container, false)
        val closeImage = view.findViewById<AppCompatImageView>(R.id.iv_close)
        val title = view.findViewById<AppCompatTextView>(R.id.tv_agency_terms_title)
        val content = view.findViewById<AppCompatTextView>(R.id.tv_agency_terms_content)

        val agencyLG = view.findViewById<AppCompatTextView>(R.id.tv_agency_lg)
        val agencyKT = view.findViewById<AppCompatTextView>(R.id.tv_agency_kt)
        val agencySKT = view.findViewById<AppCompatTextView>(R.id.tv_agency_skt)

        if (termsTitle.isNotBlank() && detailContent.isNotEmpty() && detailContent.count() == 3) {
            title.text = termsTitle
            content.text = detailContent[0]

            agencyLG.setOnClickListener {
                agencyLG.setBackgroundResource(R.drawable.stroke_bottom_sky_2dp)
                agencyKT.setBackgroundResource(R.drawable.stroke_bottom_gray_dddddd_2dp)
                agencySKT.setBackgroundResource(R.drawable.stroke_bottom_gray_dddddd_2dp)
                content.text = detailContent[0]
            }

            agencyKT.setOnClickListener {
                agencyLG.setBackgroundResource(R.drawable.stroke_bottom_gray_dddddd_2dp)
                agencyKT.setBackgroundResource(R.drawable.stroke_bottom_sky_2dp)
                agencySKT.setBackgroundResource(R.drawable.stroke_bottom_gray_dddddd_2dp)
                content.text = detailContent[1]
            }

            agencySKT.setOnClickListener {
                agencyLG.setBackgroundResource(R.drawable.stroke_bottom_gray_dddddd_2dp)
                agencyKT.setBackgroundResource(R.drawable.stroke_bottom_gray_dddddd_2dp)
                agencySKT.setBackgroundResource(R.drawable.stroke_bottom_sky_2dp)
                content.text = detailContent[2]
            }
        }

        closeImage.setOnClickListener {
            dismiss()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view?.findViewById<AppCompatImageView>(R.id.iv_close)?.setOnClickListener {
            dismiss()
        }
    }
}