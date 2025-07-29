package com.stip.stip.more.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.stip.stip.databinding.DialogPinIncorrectBinding

class PinIncorrectDialogFragment : DialogFragment() {

    private var _binding: DialogPinIncorrectBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogPinIncorrectBinding.inflate(LayoutInflater.from(context))

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()

        dialog.setCanceledOnTouchOutside(false)

        binding.buttonConfirm.setOnClickListener {
            // 휴대폰 본인인증을 통한 PIN 번호 재설정으로 이동
            com.stip.stip.signup.signup.SignUpActivity.startSignUpActivityPinNumberChange(
                requireActivity(),
                true, // 본인인증 실행
                true  // PIN 잊었을 때 플래그 설정 - 휴대폰 인증으로 바로 이동
            )
            requireActivity().finishAffinity()
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
