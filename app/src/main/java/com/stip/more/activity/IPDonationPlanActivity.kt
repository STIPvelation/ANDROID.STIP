package com.stip.stip.more.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.stip.stip.R

class IPDonationPlanActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more_ip_donation_plan)
        
        // 뒤로가기 버튼 설정
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        // 커뮤니티 참여 버튼 클릭 이벤트
        findViewById<Button>(R.id.btnJoinCommunity).setOnClickListener {
            // IP 기부자 커뮤니티 화면으로 이동
            val intent = Intent(this, IPDonorCommunityActivity::class.java)
            startActivity(intent)
        }
    }
}
