package com.stip.stip.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.stip.stip.R
import com.stip.stip.more.fragment.AccountFinalConfirmDialogFragment

class AccountDeletionGuideFragment(
    private val onConfirm: () -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_account_deletion_guide, container, false)

        // 취소 버튼
        view.findViewById<TextView>(R.id.dialog_deletion_button_close).setOnClickListener {
            dismiss()
        }

        // 확인 버튼 → 다음 다이얼로그 호출
        view.findViewById<TextView>(R.id.dialog_deletion_button_confirm).setOnClickListener {
            dismiss()
            AccountFinalConfirmDialogFragment(onConfirm)
                .show(parentFragmentManager, "AccountFinalConfirmDialog")
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
