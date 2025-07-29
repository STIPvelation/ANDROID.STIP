package com.stip.stip.ipinfo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.stip.stip.R

class TradingDetailFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_trading_detail.xml 레이아웃을 inflate
        return inflater.inflate(R.layout.fragment_trading_detail, container, false)
    }

    override fun onResume() {
        super.onResume()
        (activity as? com.stip.stip.MainActivity)?.hideHeaderAndTabs()
    }
}
