package com.stip.stip.ipinfo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import com.stip.stip.ipinfo.model.StipIndexItem

class StipIndexAdapter(private val items: List<StipIndexItem>) :
    RecyclerView.Adapter<StipIndexAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // 레이아웃 파일(item_stip_index.xml)에 정의된 ID와 일치해야 합니다.
        val nameText: TextView = view.findViewById(R.id.stip_index_text)
        val percentText: TextView = view.findViewById(R.id.stip_index_percentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stip_index, parent, false) // 레이아웃 파일 이름 확인
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // 현재 위치의 StipIndexItem 객체를 가져옵니다.
        val item = items[position]

        // --- ▼▼▼ 수정된 부분 ▼▼▼ ---

        // 1. 이름 설정: item.name 대신 item.title 사용
        holder.nameText.text = item.title

        // 2. 퍼센트 텍스트 설정: item.percent 대신 item.percentageString 사용
        holder.percentText.text = item.percentageString

        // 3. 색상 결정 로직: item.percentageString 기반 수정
        // 파싱하여 정확하게 0.00% 판별
        val percentValue = try {
            // "%" 문자와 앞에 있을 수 있는 부호 제거 후 숫자로 변환
            item.percentageString
                .replace("%", "")
                .replace("+", "")
                .toFloat()
        } catch (e: Exception) {
            0.0f // 변환 오류 시 기본값
        }
        
        val isZero = Math.abs(percentValue) < 0.005f  // 0.00으로 표시될 수준의 값
        val isPositive = percentValue > 0f
        val isNegative = percentValue < 0f

        val colorRes = when {
            isZero -> android.R.color.black   // 0.00% 인 경우 검정색
            isPositive -> R.color.color_rise // 상승 시 색상
            isNegative -> R.color.color_fall // 하락 시 색상
            else -> android.R.color.black   // 기본값
        }

        // 4. 퍼센트 텍스트 색상 설정
        holder.percentText.setTextColor(ContextCompat.getColor(holder.itemView.context, colorRes))

        // --- ▲▲▲ 수정된 부분 ▲▲▲ ---
    }

    override fun getItemCount(): Int = items.size
}