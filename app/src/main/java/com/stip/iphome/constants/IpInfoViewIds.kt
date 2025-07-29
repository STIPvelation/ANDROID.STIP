package com.stip.stip.iphome.constants

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.stip.stip.R
import com.stip.stip.iphome.model.IpListingItem
import com.stip.stip.iphome.constants.PatentRegistrationNumbers
import com.stip.stip.iphome.constants.TokenIssuanceData

/**
 * IP 정보 표시 뷰의 ID 및 유틸리티 함수들을 관리하는 클래스
 */
object IpInfoViewIds {
    
    // XML에서 정의된 뷰 ID
    const val FIRST_ISSUANCE_BOX = "first_issuance_box"
    const val TOTAL_ISSUANCE_LIMIT_BOX = "total_issuance_limit_box"
    const val TICKER_INFO_BOX = "ticker_info_box"
    const val NAVIGATION_LINKS_BOX = "navigation_links_box"
    const val RECYCLER_VIEW_INFO_DETAILS = "recyclerViewInfoDetails"
    const val RECYCLER_VIEW_LICENSE_SCOPE = "recyclerViewLicenseScope"
    
    /**
     * 발행일 표시 박스를 업데이트하는 함수
     * 
     * @param view 발행일 박스 뷰
     * @param ticker 티커 이름
     */
    fun updateFirstIssuanceBox(view: View, ticker: String?) {
        val dateTextView = view.findViewById<TextView>(R.id.tv_first_issuance_date)
        dateTextView.text = TokenIssuanceData.getFirstIssuanceDateForTicker(ticker) ?: "정보 없음"
    }
    
    /**
     * 발행 한도 표시 박스를 업데이트하는 함수
     * 
     * @param view 발행 한도 박스 뷰
     */
    fun updateTotalIssuanceLimitBox(view: View) {
        val limitTextView = view.findViewById<TextView>(R.id.tv_total_issuance_limit)
        limitTextView.text = TokenIssuanceData.getTotalIssuanceLimit()
    }
    
    /**
     * IP 정보 화면의 모든 정보 박스를 업데이트
     * 
     * @param rootView 뷰의 루트
     * @param item 표시할 IP 정보 (null일 수 있음)
     * @param context 컨텍스트 (null일 수 있음)
     */
    fun updateAllInfoBoxes(rootView: View, item: IpListingItem?, context: Context?) {
        if (context == null) {
            return
        }
        
        // 티커 이름
        val ticker = item?.ticker
        
        // 티커 이름 업데이트
        val tickerNameTextView = rootView.findViewById<TextView>(R.id.tv_ticker_name)
        tickerNameTextView.text = context.getString(R.string.ticker_name_format, ticker ?: "N/A")
        
        // 첫 발행일 업데이트 - 항상 매핑된 데이터 사용
        val firstIssuanceBox = rootView.findViewById<LinearLayout>(R.id.first_issuance_box)
        updateFirstIssuanceBox(firstIssuanceBox, ticker)
        
        // 총 발행 한도 업데이트 - 항상 고정값 사용
        val totalIssuanceLimitBox = rootView.findViewById<LinearLayout>(R.id.total_issuance_limit_box)
        updateTotalIssuanceLimitBox(totalIssuanceLimitBox)
        
        // 특허 등록번호 업데이트 - PatentRegistrationNumbers 사용
        val registrationNumberBox = rootView.findViewById<TextView>(R.id.registration_number_box)
        registrationNumberBox.text = PatentRegistrationNumbers.getRegistrationNumberForTicker(ticker)
    }
}
