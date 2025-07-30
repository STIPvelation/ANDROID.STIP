package com.stip.stip.iphome.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import com.stip.stip.iphome.constants.LicenseScope

data class LicenseScopeItem(
    val title: String,
    val licenseScopes: List<LicenseScope> // LicenseScope 객체 리스트 사용
)

class LicenseScopeAdapter(
    private val items: List<LicenseScopeItem>
) : RecyclerView.Adapter<LicenseScopeAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHeader: TextView = view.findViewById(R.id.tv_license_scope_header)
        val tvArea10: TextView = view.findViewById(R.id.tv_area_10)
        val tvRatio10: TextView = view.findViewById(R.id.tv_ratio_10)
        val tvArea7: TextView = view.findViewById(R.id.tv_area_7)
        val tvRatio7: TextView = view.findViewById(R.id.tv_ratio_7)
        val tvArea5: TextView = view.findViewById(R.id.tv_area_5)
        val tvRatio5: TextView = view.findViewById(R.id.tv_ratio_5)
        val tvArea1: TextView = view.findViewById(R.id.tv_area_1)
        val tvRatio1: TextView = view.findViewById(R.id.tv_ratio_1)
        
        // 모든 비율/영역 TextView들을 리스트로 관리
        val ratioViews = listOf(tvRatio10, tvRatio7, tvRatio5, tvRatio1)
        val areaViews = listOf(tvArea10, tvArea7, tvArea5, tvArea1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_license_scope, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvHeader.text = item.title

        // 모든 뷰를 먼저 숨김
        holder.ratioViews.forEach { it.visibility = View.GONE }
        holder.areaViews.forEach { it.visibility = View.GONE }

        // 실제 데이터만큼만 표시 (최대 4개)
        val scopesToShow = item.licenseScopes.take(4)
        scopesToShow.forEachIndexed { index, scope ->
            if (index < holder.ratioViews.size) {
                holder.ratioViews[index].apply {
                    text = scope.percentage
                    visibility = View.VISIBLE
                }
                holder.areaViews[index].apply {
                    text = scope.usageArea
                    visibility = View.VISIBLE
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
