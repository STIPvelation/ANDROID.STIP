package com.stip.stip.more.activity

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.databinding.ActivityOverseasLoginBlockBinding
import com.stip.stip.more.adapter.CountryAdapter
import com.stip.stip.more.model.Country
import java.util.Locale

class OverseasLoginBlockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOverseasLoginBlockBinding
    private val countryAdapter = CountryAdapter()
    private var allCountries = mutableListOf<Country>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOverseasLoginBlockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupToggle()
        setupSearch()
        setupCountriesList()
        loadAllCountries()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }
    }

    private fun setupToggle() {
        val sharedPref = getSharedPreferences("security_pref", Context.MODE_PRIVATE)
        val isBlocked = sharedPref.getBoolean("overseas_login_blocked", false)
        
        binding.switchOverseasLoginBlock.isChecked = isBlocked
        updateUIBasedOnBlockStatus(isBlocked)
        
        binding.switchOverseasLoginBlock.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("overseas_login_blocked", isChecked).apply()
            updateUIBasedOnBlockStatus(isChecked)
            
            if (isChecked) {
                Toast.makeText(this, "해외 로그인 차단이 활성화되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "해외 로그인 차단이 비활성화되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUIBasedOnBlockStatus(isBlocked: Boolean) {
        // If not blocked, first show a message in place of the list
        binding.tvNoBlockMessage.visibility = if (!isBlocked) View.VISIBLE else View.GONE
        
        // Animate the visibility change for a smoother experience like in iOS
        if (isBlocked) {
            binding.tvAllowCountriesHeader.alpha = 0f
            binding.cardSearch.alpha = 0f
            binding.cardCountriesContainer.alpha = 0f
            
            binding.tvAllowCountriesHeader.visibility = View.VISIBLE
            binding.cardSearch.visibility = View.VISIBLE
            binding.cardCountriesContainer.visibility = View.VISIBLE
            
            // Animate fade-in with cloud-like appearance
            binding.tvAllowCountriesHeader.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start()
                
            binding.cardSearch.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .start()
                
            binding.cardCountriesContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .start()
        } else {
            // Animate fade-out
            binding.tvAllowCountriesHeader.animate()
                .alpha(0f)
                .translationY(50f)
                .setDuration(200)
                .withEndAction {
                    binding.tvAllowCountriesHeader.visibility = View.GONE
                    binding.tvAllowCountriesHeader.translationY = -20f
                }.start()
            
            binding.cardSearch.animate()
                .alpha(0f)
                .translationY(50f)
                .setDuration(300)
                .withEndAction {
                    binding.cardSearch.visibility = View.GONE
                    binding.cardSearch.translationY = -20f
                }.start()
            
            binding.cardCountriesContainer.animate()
                .alpha(0f)
                .translationY(50f)
                .setDuration(400)
                .withEndAction {
                    binding.cardCountriesContainer.visibility = View.GONE
                    binding.cardCountriesContainer.translationY = -20f
                }.start()
        }
    }

    private fun setupSearch() {
        binding.etSearchCountry.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCountries(s.toString())
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupCountriesList() {
        binding.rvCountries.apply {
            layoutManager = LinearLayoutManager(this@OverseasLoginBlockActivity)
            adapter = countryAdapter
        }

        countryAdapter.setOnItemClickListener { country, isChecked ->
            // Save allowed countries to preferences
            val sharedPref = getSharedPreferences("security_pref", Context.MODE_PRIVATE)
            val allowedCountriesSet = sharedPref.getStringSet("allowed_countries", HashSet())?.toMutableSet() ?: mutableSetOf()
            
            if (isChecked) {
                allowedCountriesSet.add(country.id)
            } else {
                allowedCountriesSet.remove(country.id)
            }
            
            sharedPref.edit().putStringSet("allowed_countries", allowedCountriesSet).apply()
        }
    }

    private fun loadAllCountries() {
        // Load countries and flags
        allCountries = mutableListOf(
            Country("KR", "대한민국", "🇰🇷", true), // Korea is always selected
            Country("GR", "그리스", "🇬🇷"),
            Country("ZA", "남아프리카 공화국", "🇿🇦"),
            Country("NL", "네덜란드", "🇳🇱"),
            Country("NO", "노르웨이", "🇳🇴"),
            Country("NZ", "뉴질랜드", "🇳🇿"),
            Country("TW", "대만", "🇹🇼"),
            Country("DK", "덴마크", "🇩🇰"),
            Country("DE", "독일", "🇩🇪"),
            Country("RU", "러시아", "🇷🇺"),
            Country("MY", "말레이시아", "🇲🇾"),
            Country("MX", "멕시코", "🇲🇽"),
            Country("US", "미국", "🇺🇸"),
            Country("VN", "베트남", "🇻🇳"),
            Country("BE", "벨기에", "🇧🇪"),
            Country("BR", "브라질", "🇧🇷"),
            Country("SA", "사우디아라비아", "🇸🇦"),
            Country("SE", "스웨덴", "🇸🇪"),
            Country("CH", "스위스", "🇨🇭"),
            Country("ES", "스페인", "🇪🇸"),
            Country("SG", "싱가포르", "🇸🇬"),
            Country("AE", "아랍에미리트", "🇦🇪"),
            Country("AR", "아르헨티나", "🇦🇷"),
            Country("IE", "아일랜드", "🇮🇪"),
            Country("GB", "영국", "🇬🇧"),
            Country("AT", "오스트리아", "🇦🇹"),
            Country("AU", "호주", "🇦🇺"),
            Country("IL", "이스라엘", "🇮🇱"),
            Country("EG", "이집트", "🇪🇬"),
            Country("IT", "이탈리아", "🇮🇹"),
            Country("IN", "인도", "🇮🇳"),
            Country("ID", "인도네시아", "🇮🇩"),
            Country("JP", "일본", "🇯🇵"),
            Country("CN", "중국", "🇨🇳"),
            Country("CL", "칠레", "🇨🇱"),
            Country("CA", "캐나다", "🇨🇦"),
            Country("CO", "콜롬비아", "🇨🇴"),
            Country("TH", "태국", "🇹🇭"),
            Country("TR", "터키", "🇹🇷"),
            Country("PT", "포르투갈", "🇵🇹"),
            Country("PL", "폴란드", "🇵🇱"),
            Country("FR", "프랑스", "🇫🇷"),
            Country("FI", "핀란드", "🇫🇮"),
            Country("PH", "필리핀", "🇵🇭"),
            Country("HK", "홍콩", "🇭🇰")
        )

        // Load saved allowed countries
        val sharedPref = getSharedPreferences("security_pref", Context.MODE_PRIVATE)
        val allowedCountriesSet = sharedPref.getStringSet("allowed_countries", HashSet())?.toMutableSet() ?: mutableSetOf()
        
        // Make sure Korea is always in the allowed countries set
        if (!allowedCountriesSet.contains("KR")) {
            allowedCountriesSet.add("KR")
            sharedPref.edit().putStringSet("allowed_countries", allowedCountriesSet).apply()
        }
        
        // Mark countries as selected based on saved data
        allCountries.forEach { country ->
            // Korea is always selected, others based on saved preferences
            if (country.id == "KR") {
                country.isSelected = true
            } else {
                country.isSelected = allowedCountriesSet.contains(country.id)
            }
        }
        
        countryAdapter.submitList(allCountries)
    }
    
    private fun filterCountries(query: String) {
        if (query.isEmpty()) {
            countryAdapter.submitList(allCountries)
        } else {
            val filteredList = allCountries.filter { 
                it.name.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))
            }
            countryAdapter.submitList(filteredList)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
