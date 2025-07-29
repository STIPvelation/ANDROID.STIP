package com.stip.stip.more.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.stip.stip.databinding.DialogForgetPinNumberBinding

class ForgetPinNumberDialogFragment(
    private val onConfirm: () -> Unit,  // 확인 시 동작할 함수
    private val onCancel: () -> Unit    // 취소 시 동작할 함수
) : DialogFragment() {

    private var _binding: DialogForgetPinNumberBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogForgetPinNumberBinding.inflate(LayoutInflater.from(context))

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()

        // 확인 버튼
        binding.dialogLogoutButtonConfirm.setOnClickListener {
            onConfirm()  // 본인 인증 화면으로 이동하는 동작을 처리
            dismiss()
        }

        // 취소 버튼
        binding.dialogLogoutButtonCancel.setOnClickListener {
            onCancel()  // 취소 시 아무 작업도 안 할 수 있음
            dismiss()
        }

        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
