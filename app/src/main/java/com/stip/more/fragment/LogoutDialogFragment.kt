package com.stip.stip.ui.dialog // 패키지 경로는 프로젝트에 맞게 수정

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.stip.stip.R

class LogoutDialogFragment(
    private val onConfirm: () -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_logout, null)

        val cancelTextView = view.findViewById<TextView>(R.id.dialog_logout_button_cancel)
        val confirmTextView = view.findViewById<TextView>(R.id.dialog_logout_button_confirm)

        val dialog = Dialog(requireContext())
        dialog.setContentView(view)
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        cancelTextView.setOnClickListener {
            dialog.dismiss()
        }

        confirmTextView.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        return dialog
    }
}
