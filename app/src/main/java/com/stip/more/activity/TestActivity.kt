package com.stip.stip.more.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.stip.stip.R

class TestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        val testButton = findViewById<Button>(R.id.test_button)
        testButton.setOnClickListener {
            try {
                val intent = Intent()
                intent.setClassName("com.stip.stip", "com.stip.stip.more.activity.MemberInfoEditActivity")
                startActivity(intent)
                Toast.makeText(this, "액티비티 이동 시도", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "오류: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
}
