package com.stip.stip.ipinfo

import com.stip.stip.ipinfo.model.FxRateItem

/**
 * 환율 데이터를 저장하는 싱글톤 클래스
 */
object FxRateDataHolder {
    var fxRateItems: List<FxRateItem> = emptyList()
}
