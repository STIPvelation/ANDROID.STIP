package com.stip.more.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.stip.stip.R

/**
 * 준비중 안내 다이얼로그 프래그먼트
 */
class UnderConstructionDialogFragment : DialogFragment() {

    private var title: String = "안내"
    private var message: String = "현재 준비중입니다."

    companion object {
        fun newInstance(title: String? = null, message: String? = null): UnderConstructionDialogFragment {
            val fragment = UnderConstructionDialogFragment()
            val args = Bundle()
            title?.let { args.putString("title", it) }
            message?.let { args.putString("message", it) }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("title")?.let { title = it }
        arguments?.getString("message")?.let { message = it }
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.YourCustomDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_under_construction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.dialogLoginInformTitle).text = title
        view.findViewById<TextView>(R.id.dialogLoginInformMessage).text = message

        // 확인 버튼 클릭 시 다이얼로그 닫기
        view.findViewById<View>(R.id.dialogLoginInformButtonConfirm).setOnClickListener {
            dismiss()
        }
    }
}
