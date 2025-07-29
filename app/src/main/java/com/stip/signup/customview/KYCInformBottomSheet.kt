package com.stip.stip.signup.customview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stip.stip.R
import com.stip.stip.signup.signup.kyc.inform.SignUpKYCInformAdapter

class KYCInformBottomSheet(
    private val title: String,
    private val itemList: List<String>,
    private val itemClick: ((String) -> Unit)
): BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.bottom_dialog_kyc_inform, container, false)
        val title = view.findViewById<AppCompatTextView>(R.id.tv_sign_up_kyc_inform_selct_title)
        val list = view.findViewById<RecyclerView>(R.id.rv_sign_up_kyc_list)

        title.text = this.title
        val signUpKYCInformAdapter = SignUpKYCInformAdapter(itemList) {
            itemClick.invoke(it)
            dismiss()
        }
        list.adapter = signUpKYCInformAdapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view?.findViewById<AppCompatImageView>(R.id.iv_close)?.setOnClickListener {
            dismiss()
        }
    }
}