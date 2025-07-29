package com.stip.stip.ipinfo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import com.stip.stip.databinding.ItemFxRateBinding
import com.stip.stip.ipinfo.model.FxRateItem

class FxRateAdapter(
    private val items: List<FxRateItem>
) : RecyclerView.Adapter<FxRateAdapter.FxRateViewHolder>() {

    inner class FxRateViewHolder(private val binding: ItemFxRateBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FxRateItem) {
            with(binding) {
                textCountry.text = item.country
                textCode.text = "(${item.code})"
                textRate.text = String.format("%.2f", item.rate)
                textChange.text = String.format("%.2f", item.change)
                textPercent.text = String.format("%+.2f%%", item.percent)

                val ZERO_THRESHOLD = 0.005f
                val isZero = Math.abs(item.change) < ZERO_THRESHOLD
                val isRise = item.change > 0
                val context = root.context

                // 0.00일 때는 검정색, 그 외에는 상승/하락 색상
                val colorRes = when {
                    isZero -> android.R.color.black
                    isRise -> R.color.color_increase
                    else -> R.color.color_decrease
                }
                val color = ContextCompat.getColor(context, colorRes)
                
                // 텍스트 컬러 적용
                textRate.setTextColor(color)
                textChange.setTextColor(color)
                textPercent.setTextColor(color)

                // 이미지 처리: 0.00일 때는 화살표 숨김
                if (isZero) {
                    imageArrow.visibility = android.view.View.INVISIBLE
                } else {
                    imageArrow.visibility = android.view.View.VISIBLE
                    val icon = if (isRise) R.drawable.ic_arrow_up_red else R.drawable.ic_arrow_down_blue
                    imageArrow.setImageResource(icon)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FxRateViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemFxRateBinding.inflate(inflater, parent, false)
        return FxRateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FxRateViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}