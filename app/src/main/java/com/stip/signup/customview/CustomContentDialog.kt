package com.stip.stip.signup.customview

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import com.stip.stip.R
import com.stip.stip.databinding.DialogCustomContentBinding

class CustomContentDialog(
    context: Context,
    var callback: (View) -> Unit
): Dialog(context, R.style.PopUpDialog) {

    private lateinit var binding: DialogCustomContentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.dialog_custom_content, null, false)
        setContentView(binding.root)

        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        setCancelable(false)
        setCanceledOnTouchOutside(false)

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnConfirm.setOnClickListener {
            dismiss()
            callback.invoke(it)
        }
    }

    fun setText(title: String, comment: String, cancel: String, confirm: String, accent: Boolean = false) {
        show()

        if (title.isNotBlank()) {
            binding.tvTitle.text = title
            binding.tvTitle.visibility = View.VISIBLE
        } else {
            binding.tvTitle.visibility = View.GONE
        }

        if (comment.isNotBlank()) {
            binding.tvContent.visibility = View.VISIBLE
        } else {
            binding.tvContent.visibility = View.GONE
        }

        if (cancel.isNotBlank()) {
            binding.btnCancel.text = cancel
            binding.btnConfirm.visibility = View.VISIBLE
        } else {
            binding.btnCancel.visibility = View.GONE
        }

        if (confirm.isNotBlank()) {
            binding.btnConfirm.text = confirm
        }

        if (accent) {
            val result = String.format(comment)
            binding.tvContent.text = androidx.core.text.HtmlCompat.fromHtml(result, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY)
        } else {
            binding.tvContent.text = comment
        }
    }

    fun setText(titleId: Int, commentId: Int, cancelId: Int, confirmId: Int, accent: Boolean = false) {
        setText(
            if (titleId > 0) context.getText(titleId).toString() else "",
            if (commentId > 0) context.getText(commentId).toString() else "",
            if (cancelId > 0) context.getText(cancelId).toString() else "",
            if (confirmId > 0) context.getText(confirmId).toString() else "",
            accent
        )
    }

    fun setText(title: String, comment: String, cancelId: Int, confirmId: Int, accent: Boolean = false) {
        setText(
            title,
            comment,
            if (cancelId > 0) context.getText(cancelId).toString() else "",
            if (confirmId > 0) context.getText(confirmId).toString() else "",
            accent
        )
    }

    fun setText(title: String, detailText: String, cancel: String, confirm: String) {
        setText(
            title,
            detailText,
            cancel,
            confirm,
            false
        )
    }

    override fun show() {
        // StatusBar, NavigationBar Hide
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )
        window?.addFlags(
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11 이상에서는 WindowInsetsController 사용
            window?.decorView?.windowInsetsController?.let { controller ->
                controller.hide(android.view.WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            val flag = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            )
            @Suppress("DEPRECATION")
            window?.decorView?.systemUiVisibility = flag
        }
        super.show()
        window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }



}