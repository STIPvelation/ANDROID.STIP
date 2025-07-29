package com.stip.stip.more.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stip.stip.R
import com.stip.stip.databinding.ActivityAddressSearchBinding
import com.stip.stip.signup.signup.kyc.address.SignUpKYCAddressFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * 회원정보 수정 화면에서 주소 검색을 위한 액티비티
 * 회원가입 시 사용되는 주소 검색 Fragment를 재사용함
 */

@AndroidEntryPoint
class AddressSearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddressSearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddressSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (savedInstanceState == null) {
            // 회원가입 화면과 동일한 주소 검색 Fragment 사용
            val fragment = SignUpKYCAddressFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }

    companion object {
        const val EXTRA_ADDRESS = "address"
        const val EXTRA_ADDRESS_DETAIL = "detailAddress"    
        const val EXTRA_POSTAL_CODE = "zipCode"
    }
}
