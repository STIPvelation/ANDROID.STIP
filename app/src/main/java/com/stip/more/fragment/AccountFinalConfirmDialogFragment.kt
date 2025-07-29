package com.stip.stip.more.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.stip.stip.R

class AccountFinalConfirmDialogFragment(
    private val onConfirm: () -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_account_deletion_confirm, container, false)

        val buttonCancel = view.findViewById<TextView>(R.id.button_cancel)
        val buttonConfirm = view.findViewById<TextView>(R.id.button_confirm)

        buttonCancel.setOnClickListener {
            dismiss()
        }

        buttonConfirm.setOnClickListener {
            dismiss()
            onConfirm()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
}
