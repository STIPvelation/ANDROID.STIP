package com.stip.stip.ipinfo

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import com.stip.stip.databinding.ItemInfoTrendFxRateBinding
import com.stip.stip.databinding.ItemInfoTrendMiniChartBinding
import com.stip.stip.ipinfo.adapter.FxRateAdapter
import com.stip.stip.ipinfo.model.FxRateItem
import com.stip.stip.ipinfo.model.IpTrendMiniChartItem
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class IpTrendMiniChartAdapter(
    private val miniChartItems: List<IpTrendMiniChartItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_MINI_CHART = 0
        private const val VIEW_TYPE_FX_RATE = 1
    }

    inner class MiniChartViewHolder(private val binding: ItemInfoTrendMiniChartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: IpTrendMiniChartItem) {
            val stipBase = when (item.stipLabel) {
                "STIP 1000" -> 1000f
                "특허 100" -> 100f
                "STIP 음악" -> 70f
                else -> 1000f
            }
            val etipBase = when (item.etipLabel) {
                "ETIP 500" -> 500f
                "ETIP 영화" -> 300f
                "ETIP AI" -> 10f
                else -> 500f
            }

            // 0.00인지 확인하기 위한 상수 (매우 작은 값)
            val ZERO_THRESHOLD = 0.005f
            
            // STIP 변화량과 상태 확인
            val stipChange = item.stipChange
            val isStipZero = Math.abs(stipChange) < ZERO_THRESHOLD
            val stipRise = stipChange > 0
            
            // ETIP 변화량과 상태 확인
            val etipChange = item.etipChange
            val isEtipZero = Math.abs(etipChange) < ZERO_THRESHOLD
            val etipRise = etipChange > 0

            with(binding) {
                // STIP 데이터
                stipLabel.text = item.stipLabel
                stipPrice.text = String.format("%.2f", item.stipPrice)
                changeStip.text = String.format("%.2f", Math.abs(item.stipChange))
                percentStip.text = String.format("%+.2f%%", item.stipPercent)

                // 0.00일 때는 검은색, 그 외에는 상승/하락 색상
                val stipColorRes = when {
                    isStipZero -> android.R.color.black
                    stipRise -> R.color.color_rise
                    else -> R.color.color_fall
                }
                val stipColor = ContextCompat.getColor(root.context, stipColorRes)
                stipPrice.setTextColor(stipColor)
                changeStip.setTextColor(stipColor)
                percentStip.setTextColor(stipColor)
                
                // 0.00일 때는 화살표 숨김
                if(isStipZero) {
                    increaseIconStip.visibility = View.INVISIBLE
                } else {
                    increaseIconStip.visibility = View.VISIBLE
                    increaseIconStip.setImageResource(if (stipRise) R.drawable.ic_arrow_up_red else R.drawable.ic_arrow_down_blue)
                }

                // ETIP 데이터
                etipLabel.text = item.etipLabel
                etipPrice.text = String.format("%.2f", item.etipPrice)
                changeEtip.text = String.format("%.2f", Math.abs(item.etipChange))
                percentEtip.text = String.format("%+.2f%%", item.etipPercent)

                // 0.00일 때는 검은색, 그 외에는 상승/하락 색상
                val etipColorRes = when {
                    isEtipZero -> android.R.color.black
                    etipRise -> R.color.color_rise
                    else -> R.color.color_fall
                }
                val etipColor = ContextCompat.getColor(root.context, etipColorRes)
                etipPrice.setTextColor(etipColor)
                changeEtip.setTextColor(etipColor)
                percentEtip.setTextColor(etipColor)
                
                // 0.00일 때는 화살표 숨김
                if(isEtipZero) {
                    increaseIconEtip.visibility = View.INVISIBLE
                } else {
                    increaseIconEtip.visibility = View.VISIBLE
                    increaseIconEtip.setImageResource(if (etipRise) R.drawable.ic_arrow_up_red else R.drawable.ic_arrow_down_blue)
                }

                // 차트 설정
                setupChart(chartStip, item.stipEntries, stipBase)
                setupChart(chartEtip, item.etipEntries, etipBase)
            }
        }

        private fun setupChart(
            chart: LineChart,
            entries: List<Entry>,
            basePrice: Float
        ) {
            if (entries.isEmpty()) {
                chart.clear()
                chart.invalidate()
                return
            }

            val context = chart.context
            val riseColor = ContextCompat.getColor(context, R.color.color_rise)
            val fallColor = ContextCompat.getColor(context, R.color.color_fall)

            val lastPrice = entries.last().y
            val isRise = lastPrice >= basePrice
            val lineColor = if (isRise) riseColor else fallColor

            val dataSet = LineDataSet(entries, "").apply {
                color = lineColor
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 1.5f
                mode = LineDataSet.Mode.CUBIC_BEZIER
                cubicIntensity = 0.2f
                highLightColor = Color.TRANSPARENT
                setDrawFilled(true)

                val gradientDrawable = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(lineColor, Color.TRANSPARENT)
                )
                fillDrawable = gradientDrawable
            }

            chart.apply {
                data = LineData(dataSet)
                setTouchEnabled(false)
                setPinchZoom(false)
                setScaleEnabled(false)
                setDrawGridBackground(false)

                axisLeft.apply {
                    isEnabled = true
                    setDrawLabels(false)
                    setDrawGridLines(false)
                    setDrawAxisLine(false)
                }

                axisRight.isEnabled = false
                xAxis.apply {
                    isEnabled = false
                }

                description.isEnabled = false
                legend.isEnabled = false

                invalidate()
            }
        }
    }

    inner class FxRateViewHolder(private val binding: ItemInfoTrendFxRateBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            binding.root.visibility = View.VISIBLE

            // FxRateDataHolder에서 실제 API 데이터 가져오기
            // 데이터가 있으면 사용하고, 없으면 빈 UI 구조만 표시
            val fxRates = if (FxRateDataHolder.fxRateItems.isNotEmpty()) {
                FxRateDataHolder.fxRateItems  // API에서 가져온 실제 데이터 사용
            } else {
                // 데이터가 없을 경우 빈 UI를 표시하기 위한 더미 구조(값은 빈 값) 생성
                // 국가/코드만 설정하고 나머지 값은 모두 0으로 설정
                listOf(
                    FxRateItem(
                        binding.root.context.getString(R.string.country_korea),
                        binding.root.context.getString(R.string.rate_krw_usd),
                        0f, 0f, 0f
                    ),
                    FxRateItem(
                        binding.root.context.getString(R.string.country_japan),
                        binding.root.context.getString(R.string.rate_jpn_usd),
                        0f, 0f, 0f
                    ),
                    FxRateItem(
                        binding.root.context.getString(R.string.country_china),
                        binding.root.context.getString(R.string.rate_cny_usd),
                        0f, 0f, 0f
                    ),
                    FxRateItem(
                        binding.root.context.getString(R.string.country_euro),
                        binding.root.context.getString(R.string.rate_eur_usd),
                        0f, 0f, 0f
                    )
                )
            }
            
            // 실제 데이터가 있는지 로그로 확인
            android.util.Log.d("FxRateViewHolder", "현재 FX 데이터 개수: ${FxRateDataHolder.fxRateItems.size}")

            val adapter = FxRateAdapter(fxRates)

            binding.recyclerViewFxRate.apply {
                layoutManager = GridLayoutManager(context, 2)
                this.adapter = adapter
                isNestedScrollingEnabled = false
            }
        }

    }
        override fun getItemViewType(position: Int): Int {
        return if (position < miniChartItems.size) VIEW_TYPE_MINI_CHART else VIEW_TYPE_FX_RATE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_MINI_CHART) {
            val binding = ItemInfoTrendMiniChartBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            MiniChartViewHolder(binding)
        } else {
            val binding = ItemInfoTrendFxRateBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            FxRateViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MiniChartViewHolder) {
            holder.bind(miniChartItems[position])
        } else if (holder is FxRateViewHolder) {
            holder.bind()
        }
    }

    override fun getItemCount(): Int = miniChartItems.size + 1 // 미니차트 + 환율
}