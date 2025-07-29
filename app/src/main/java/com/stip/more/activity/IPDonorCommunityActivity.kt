package com.stip.stip.more.activity

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.stip.stip.more.adapter.OrganizationAdapter
import com.stip.stip.more.adapter.IpListingAdapter
import com.stip.stip.R
import com.stip.stip.more.adapter.ChatMessageAdapter
import com.stip.stip.more.adapter.DonorAdapter
import com.stip.stip.more.model.ChatMessage
import com.stip.stip.more.model.DonorModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IPDonorCommunityActivity : AppCompatActivity() {

    private lateinit var recyclerDonors: RecyclerView
    private lateinit var recyclerMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnMenu: ImageButton
    private lateinit var tvMessageCount: TextView

    private val donors = mutableListOf(
        DonorModel("홍길동", 3, true),
        DonorModel("김지식", 2),
        DonorModel("이기부", 5),
        DonorModel("박나눔", 1),
        DonorModel("최창의", 4)
    )

    private val messages = mutableListOf(
        ChatMessage(
            "1",
            "김지식",
            "안녕하세요! 저는 특허 2개를 기부했습니다.",
            "오후 3:15",
            false
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more_ip_donor_community)

        initViews()
        setupRecyclerViews()
        setupClickListeners()
    }

    private fun initViews() {
        recyclerDonors = findViewById(R.id.recyclerDonors)
        recyclerMessages = findViewById(R.id.recyclerMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnMenu = findViewById(R.id.btnMenu)
        tvMessageCount = findViewById(R.id.tvMessageCount)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun setupRecyclerViews() {
        // 기부자 목록
        recyclerDonors.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerDonors.adapter = DonorAdapter(donors)

        // 메시지 목록
        recyclerMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerMessages.adapter = ChatMessageAdapter(messages)

        // 메시지 수 표시
        tvMessageCount.text = "총 ${messages.size}개의 메시지"
    }

    private fun setupClickListeners() {
        // 메시지 전송 버튼
        btnSend.setOnClickListener {
            val messageText = etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
            }
        }

        // 메뉴 버튼
        btnMenu.setOnClickListener {
            showBottomSheet()
        }

        // 첨부 파일 버튼
        findViewById<ImageButton>(R.id.btnAttachment).setOnClickListener {
            Toast.makeText(this, "파일 첨부 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendMessage(content: String) {
        val timeStamp = SimpleDateFormat("a h:mm", Locale.KOREA).format(Date())
        
        val newMessage = ChatMessage(
            System.currentTimeMillis().toString(),
            "홍길동",
            content,
            timeStamp,
            true
        )

        messages.add(newMessage)
        recyclerMessages.adapter?.notifyItemInserted(messages.size - 1)
        recyclerMessages.smoothScrollToPosition(messages.size - 1)
        
        // 입력 필드 초기화
        etMessage.text.clear()

        // 메시지 수 업데이트
        tvMessageCount.text = "총 ${messages.size}개의 메시지"
    }

    private fun showBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_donor_menu, null)
        
        // 기부처 선정하기 버튼
        view.findViewById<android.view.View>(R.id.layoutDonationSelection).setOnClickListener {
            dialog.dismiss()
            showDonationTargetDialog()
        }
        
        // IP 거래소 상장 버튼
        view.findViewById<android.view.View>(R.id.layoutIPExchange).setOnClickListener {
            dialog.dismiss()
            showIpExchangeListingDialog()
        }
        
        dialog.setContentView(view)
        dialog.show()
    }
    
    private fun showDonationTargetDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_ip_donation_target)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // 투명 배경으로 둥근 모서리 표시
        
        // 닫기 버튼 설정
        dialog.findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }
        
        val organizations = listOf(
            "대한의료봉사협회",
            "세계아동구호재단",
            "한국자연환경보전협회",
            "국경없는의사회",
            "한국장애인재단",
            "푸르메재단"
        )

        val recyclerView = dialog.findViewById<RecyclerView>(R.id.recyclerDonationTargets)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.isItemPrefetchEnabled = true
        recyclerView.layoutManager = layoutManager
        recyclerView.itemAnimator = null // 애니메이션 제거로 렌더링 최소화
        
        // 새로운 어댑터 사용
        val adapter = OrganizationAdapter(organizations)
        recyclerView.adapter = adapter

        // 닫기 버튼 설정
        dialog.findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        // 확인 버튼 설정
        dialog.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            val selectedOrg = adapter.getSelectedOrganization()
            if (selectedOrg != null) {
                Toast.makeText(this, "'$selectedOrg'(으)로 기부처가 선정되었습니다.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "기부처를 선택해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }
    
    private fun showIpExchangeListingDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_ip_exchange_listing)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // 투명 배경으로 설정하여 둥근 모서리 표시
        
        // 더미 IP 데이터 - 이미지에서 보이는 IP 목록으로 확장
        val ipItems = listOf(
            "데이터 처리 특허 #56723",
            "모바일 앱 UI 특허 #78932",
            "웨어러블 디자인 특허 #12890",
            "의료기기 특허 #23541",
            "헬스케어 알고리즘 특허 #45632",
            "인공지능 학습모델 #67890",
            "블록체인 인증 특허 #34567",
            "결제 시스템 특허 #89012",
            "통신 프로토콜 특허 #45678",
            "AR/VR 특허 #12345"
        )
        
        // IP 카운터 및 UI 요소 세팅
        val ipCountView = dialog.findViewById<TextView>(R.id.tvIpCount)
        val tvMinimumRequired = dialog.findViewById<TextView>(R.id.tvMinimumRequired)
        val btnApply = dialog.findViewById<Button>(R.id.btnApply)
        
        // 최소 필요 개수 (예: 50개)
        val minimumRequired = 50
        tvMinimumRequired.text = "최소 ${minimumRequired}개 필요"
        
        // 리사이클러뷰 설정
        val recyclerView = dialog.findViewById<RecyclerView>(R.id.recyclerIpList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // 새 어댑터 사용
        val adapter = IpListingAdapter(ipItems)
        recyclerView.adapter = adapter
        
        // 선택 변경 시 카운트 및 버튼 상태 업데이트
        adapter.setOnSelectionChangedListener { selectedCount ->
            ipCountView.text = "${selectedCount}개"
            
            // 최소 필요 개수보다 적으면 버튼 비활성화, 색상 변경
            val isEnough = selectedCount >= minimumRequired
            btnApply.isEnabled = isEnough
            btnApply.alpha = if (isEnough) 1.0f else 0.5f
            
            // 필요 개수 여부에 따른 안내문구 색상 변경
            tvMinimumRequired.setTextColor(
                if (isEnough) getColor(R.color.colorPrimary) else getColor(R.color.error_red)
            )
        }
        
        // 닫기 버튼 설정
        dialog.findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }
        
        // 신청 버튼 설정 - 처음에는 비활성화
        btnApply.isEnabled = false
        btnApply.alpha = 0.5f
        
        btnApply.setOnClickListener {
            val selectedCount = adapter.getSelectedCount()
            
            if (selectedCount >= minimumRequired) {
                Toast.makeText(this, "${selectedCount}개의 IP가 거래소 상장 신청되었습니다.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "최소 ${minimumRequired}개의 IP를 선택해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialog.show()
    }
}
