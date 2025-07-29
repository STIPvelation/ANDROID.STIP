package com.stip.stip.more.activity

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.stip.stip.R

class MoreIpDonationAgreementActivity : AppCompatActivity() {

    private lateinit var checkAllAgree: CheckBox
    private lateinit var checkAgree1: CheckBox
    private lateinit var checkAgree2: CheckBox
    private lateinit var checkAgree3: CheckBox
    private lateinit var checkAgree4: CheckBox
    private lateinit var btnAgree: Button
    
    private val allCheckboxes = mutableListOf<CheckBox>()
    private val termDetails = mapOf(
        1 to "기부하는 IP의 사용 및 라이선싱 권한을 STIP에 위임하며, STIP가 이를 활용해 수익을 창출하고 관리하는 것에 동의합니다. IP의 소유권은 기부자에게 유지됩니다.",
        2 to "STIP은 위임받은 IP의 가치를 높이기 위해 최선을 다하며, 관리/마케팅/법률 자문 등 모든 과정을 공정하고 투명하게 처리할 의무를 가집니다.",
        3 to "IP 활용으로 발생한 수익은 기부자님의 의견을 수렴하여 투명한 절차에 따라 사회 공헌을 위해 사용됩니다.",
        4 to "기부된 IP의 권리가 제3자에게 이전되거나 소멸될 경우, 본 계약은 자동으로 종료되며 STIP에 대한 권리 위임도 해지됩니다."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more_ip_donation_agreement)
        
        initViews()
        setupListeners()
    }
    
    private fun initViews() {
        // 체크박스 초기화
        checkAllAgree = findViewById(R.id.checkAllAgree)
        checkAgree1 = findViewById(R.id.checkAgree1)
        checkAgree2 = findViewById(R.id.checkAgree2)
        checkAgree3 = findViewById(R.id.checkAgree3)
        checkAgree4 = findViewById(R.id.checkAgree4)
        
        // 버튼 초기화
        btnAgree = findViewById(R.id.btnAgree)
        
        // 체크박스 리스트 설정
        allCheckboxes.add(checkAgree1)
        allCheckboxes.add(checkAgree2)
        allCheckboxes.add(checkAgree3)
        allCheckboxes.add(checkAgree4)
    }
    
    private fun setupListeners() {
        // 뒤로가기 버튼
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        // 전체 동의 체크박스
        checkAllAgree.setOnCheckedChangeListener { _, isChecked ->
            allCheckboxes.forEach { it.isChecked = isChecked }
            updateAgreeButtonState()
        }
        
        // 개별 약관 체크박스들의 리스너
        allCheckboxes.forEach { checkbox ->
            checkbox.setOnCheckedChangeListener { _, _ ->
                updateAllAgreeCheckbox()
                updateAgreeButtonState()
            }
        }
        
        // 약관 상세보기 화살표 설정
        setupTermExpand(R.id.expandAgree1, 1)
        setupTermExpand(R.id.expandAgree2, 2)
        setupTermExpand(R.id.expandAgree3, 3)
        setupTermExpand(R.id.expandAgree4, 4)

        // 동의하기 버튼
        btnAgree.setOnClickListener {
            Toast.makeText(this, "IP 기부 동의가 완료되었습니다.", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }
    
    private fun setupTermExpand(viewId: Int, termIndex: Int) {
        findViewById<ImageView>(viewId).setOnClickListener {
            showTermDetailDialog(termIndex)
        }
    }
    
    private fun showTermDetailDialog(termIndex: Int) {
        val title = "제 ${termIndex}조 상세 내용"
        val content = termDetails[termIndex] ?: "상세 내용이 준비되고 있습니다."
        
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(content)
            .setPositiveButton("확인") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    
    private fun updateAllAgreeCheckbox() {
        val allChecked = allCheckboxes.all { it.isChecked }
        checkAllAgree.isChecked = allChecked
    }
    
    private fun updateAgreeButtonState() {
        val allChecked = allCheckboxes.all { it.isChecked }
        btnAgree.isEnabled = allChecked
        
        if (allChecked) {
            btnAgree.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
        } else {
            btnAgree.setBackgroundColor(Color.parseColor("#E0E0E0"))
        }
    }
}
