package com.stip.ipasset.usd.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.stip.stip.R
import com.stip.stip.databinding.ActivityDepositKrwBinding

class DepositKrwActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDepositKrwBinding
    private var lastCopiedView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var hideMessageRunnable: Runnable? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDepositKrwBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 뒤로가기 버튼 설정
        binding.backButton.setOnClickListener {
            finish()
        }
        
        // 각 은행 계좌의 복사 버튼 설정
        setupCopyButtons()
    }
    
    private fun setupCopyButtons() {
        // KB 은행 복사 버튼 설정
        val kbAccount = "102701-04-435574"
        val kbCopyButton = binding.root.findViewById<ImageButton>(R.id.kb_copy_button)
        val kbCopyMessage = binding.root.findViewById<TextView>(R.id.kb_copy_success_message)
        
        // 신한은행 복사 버튼 설정
        val shAccount = "140-015-070902"
        val shCopyButton = binding.root.findViewById<ImageButton>(R.id.sh_copy_button)
        val shCopyMessage = binding.root.findViewById<TextView>(R.id.sh_copy_success_message)
        
        // 우리은행 복사 버튼 설정
        val wrAccount = "1005-804-753434"
        val wrCopyButton = binding.root.findViewById<ImageButton>(R.id.wr_copy_button)
        val wrCopyMessage = binding.root.findViewById<TextView>(R.id.wr_copy_success_message)
        
        // KB 복사 버튼 클릭 이벤트
        kbCopyButton.setOnClickListener {
            copyAccountNumber(kbAccount, binding.kbBankAccount, kbCopyMessage)
        }
        
        // 신한 복사 버튼 클릭 이벤트
        shCopyButton.setOnClickListener {
            copyAccountNumber(shAccount, binding.shBankAccount, shCopyMessage)
        }
        
        // 우리 복사 버튼 클릭 이벤트
        wrCopyButton.setOnClickListener {
            copyAccountNumber(wrAccount, binding.wrBankAccount, wrCopyMessage)
        }
    }
    
    private fun copyAccountNumber(accountNumber: String, containerView: View, successMessage: TextView) {
        // 클립보드에 계좌번호 복사
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("계좌번호", accountNumber)
        clipboardManager.setPrimaryClip(clipData)
        
        // 기존에 표시된 복사 성공 메시지가 있다면 숨김
        if (lastCopiedView != null) {
            // Check which bank was last copied and hide that message
            when (lastCopiedView) {
                binding.kbBankAccount -> binding.root.findViewById<TextView>(R.id.kb_copy_success_message).visibility = View.GONE
                binding.shBankAccount -> binding.root.findViewById<TextView>(R.id.sh_copy_success_message).visibility = View.GONE
                binding.wrBankAccount -> binding.root.findViewById<TextView>(R.id.wr_copy_success_message).visibility = View.GONE
            }
        }
        
        // 새로운 복사 성공 메시지 표시
        successMessage.visibility = View.VISIBLE
        lastCopiedView = containerView
        
        // 기존에 예약된 메시지 숨기기 작업이 있다면 취소
        hideMessageRunnable?.let { handler.removeCallbacks(it) }
        
        // 2초 후 메시지 숨기기
        hideMessageRunnable = Runnable {
            successMessage.visibility = View.GONE
        }
        
        // 2초 후에 실행되도록 예약
        handler.postDelayed(hideMessageRunnable!!, 2000)
    }
}
