package com.stip.stip.more.fragment

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.stip.stip.MainViewModel
import com.stip.stip.R
import com.stip.stip.databinding.DialogCustomerContactOptionsBinding
import com.stip.stip.databinding.FragmentMoreCustomerCenterBinding

class MoreCustomerCenterFragment : Fragment() {

    private var _binding: FragmentMoreCustomerCenterBinding? = null
    private val binding get() = _binding!!

    private val TAG = "MoreCustomerCenterFragment"
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        try {
            Log.d(TAG, "onCreateView started")
            _binding = FragmentMoreCustomerCenterBinding.inflate(inflater, container, false)
            return binding.root
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreateView: ${e.message}", e)
            // Return a fallback view if binding fails
            return inflater.inflate(R.layout.fragment_more_customer_center, container, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            Log.d(TAG, "onViewCreated started")

            // FAQ 아이템 1 클릭 처리
            view.findViewById<View>(R.id.faq_header_1)?.setOnClickListener {
                toggleFaqAnswer(view.findViewById(R.id.tv_faq_answer_1))
                toggleArrowRotation(view.findViewById(R.id.iv_faq_arrow_1))
            }

            // FAQ 아이템 2 클릭 처리
            view.findViewById<View>(R.id.faq_header_2)?.setOnClickListener {
                toggleFaqAnswer(view.findViewById(R.id.tv_faq_answer_2))
                toggleArrowRotation(view.findViewById(R.id.iv_faq_arrow_2))
            }

            // FAQ 아이템 3 클릭 처리
            view.findViewById<View>(R.id.faq_header_3)?.setOnClickListener {
                toggleFaqAnswer(view.findViewById(R.id.tv_faq_answer_3))
                toggleArrowRotation(view.findViewById(R.id.iv_faq_arrow_3))
            }

            // 문의하기 버튼 클릭 처리
            view.findViewById<View>(R.id.btn_contact_center)?.setOnClickListener {
                showContactOptions()
            }

            // Contact items are handled directly in showContactOptions method
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated: ${e.message}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (isAdded && !requireActivity().isFinishing) {
                Log.d(TAG, "onResume: updating header")
                viewModel.updateHeaderTitle(getString(R.string.header_customer_center))
                viewModel.enableBackNavigation {
                    if (isAdded && parentFragmentManager.backStackEntryCount > 0) {
                        parentFragmentManager.popBackStack()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            Log.d(TAG, "onDestroyView called")
            _binding = null
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroyView: ${e.message}", e)
        }
    }

    /**
     * FAQ 답변 표시 토글 함수
     */
    private fun toggleFaqAnswer(answerTextView: View?) {
        try {
            answerTextView?.let {
                if (it.visibility == View.VISIBLE) {
                    it.visibility = View.GONE
                } else {
                    it.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling FAQ answer: ${e.message}", e)
        }
    }

    /**
     * FAQ 화살표 회전 애니메이션 처리
     */
    private fun toggleArrowRotation(arrowImageView: View?) {
        try {
            arrowImageView?.let {
                if (it.rotation == 0f) {
                    it.animate().rotation(180f).setDuration(300).start()
                } else {
                    it.animate().rotation(0f).setDuration(300).start()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling arrow rotation: ${e.message}", e)
        }
    }

    /**
     * 문의 옵션 다이얼로그 표시 - iOS 스타일로 구현
     */
    private fun showContactOptions() {
        try {
            if (!isAdded || requireActivity().isFinishing) {
                Log.e(TAG, "Cannot show dialog - fragment not attached or activity finishing")
                return
            }
            
            val context = context ?: return
            
            val dialog = BottomSheetDialog(context)
            val contactBinding = DialogCustomerContactOptionsBinding.inflate(layoutInflater)
            dialog.setContentView(contactBinding.root)
            
            // 닫기 버튼 클릭 시
            contactBinding.btnClose.setOnClickListener {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
            
            // 전화 상담 클릭 시
            contactBinding.itemCallConsultation.setOnClickListener {
                showCallConfirmDialog()
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
            
            // 상담 문의 (이메일) 클릭 시
            contactBinding.itemEmailConsultation.setOnClickListener {
                try {
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:support@stip.global")
                        putExtra(Intent.EXTRA_SUBJECT, "[STIP] 1:1 문의")
                    }
                    startActivity(Intent.createChooser(emailIntent, "이메일 앱 선택"))
                } catch (e: Exception) {
                    Log.e(TAG, "Error launching email intent: ${e.message}", e)
                    Toast.makeText(context, "이메일 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
            
            // 카카오톡 상담 클릭 시
            contactBinding.itemKakaoConsultation.setOnClickListener {
                try {
                    val kakaoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://pf.kakao.com/_ahaGn"))
                    startActivity(kakaoIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error launching kakao intent: ${e.message}", e)
                    Toast.makeText(context, "카카오톡 링크를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
            
            // 사칭사기제보 클릭 시
            contactBinding.itemFraudReport.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://stipvelation.com/contact-contact.html"))
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error launching fraud report intent: ${e.message}", e)
                    Toast.makeText(context, "웹페이지를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
            
            // 불공정 거래 클릭 시
            contactBinding.itemUnfairTrade.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://stipvelation.com/contact-contact.html"))
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error launching unfair trade intent: ${e.message}", e)
                    Toast.makeText(context, "웹페이지를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
            
            // 거래내역서 발급 클릭 시
            contactBinding.itemTransactionStatement.setOnClickListener {
                Toast.makeText(context, "준비중", Toast.LENGTH_SHORT).show()
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
            
            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing contact options: ${e.message}", e)
            context?.let { ctx ->
                Toast.makeText(ctx, "오류가 발생했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 전화 확인 다이얼로그 표시
     */
    private fun showCallConfirmDialog() {
        try {
            if (!isAdded || requireActivity().isFinishing) {
                Log.e(TAG, "Cannot show dialog - fragment not attached or activity finishing")
                return
            }
            
            val context = context ?: return
            
            AlertDialog.Builder(context)
                .setTitle("고객센터에 전화하기")
                .setMessage("02-2238-4345 번호로 연결합니다.")
                .setPositiveButton("전화하기") { _, _ ->
                    try {
                        val phoneIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:0222384345")
                        }
                        startActivity(phoneIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error launching phone intent: ${e.message}", e)
                        Toast.makeText(context, "전화 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("취소", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing call confirm dialog: ${e.message}", e)
        }
    }


}
