package com.stip.stip.iptransaction.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import com.stip.stip.databinding.IpTradeProfitLossDetailBinding
import com.stip.stip.ipinfo.model.ProfitItem
import java.text.SimpleDateFormat
import java.util.Locale

class ProfitAdapter : RecyclerView.Adapter<ProfitAdapter.ProfitViewHolder>() {

    private var profitList = listOf<ProfitItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfitViewHolder {
        val binding = IpTradeProfitLossDetailBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProfitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfitViewHolder, position: Int) {
        holder.bind(profitList[position])
    }

    override fun getItemCount(): Int = profitList.size

    fun submitList(list: List<ProfitItem>) {
        profitList = list
        notifyDataSetChanged()
    }

    class ProfitViewHolder(private val binding: IpTradeProfitLossDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProfitItem) {
            binding.textDateItem.text = formatDate(item.date)
            binding.textDailyProfitItem.text = item.dailyProfit
            binding.textCumulativeProfitItem.text = item.cumulativeProfit
            binding.textDailyRateItem.text = item.dailyRate
            binding.textCumulativeRateItem.text = item.cumulativeRate
            binding.textEndingAssetItem.text = item.endingAsset
            binding.textStartingAssetItem.text = item.startingAsset
            binding.textDepositItem.text = item.deposit
            binding.textWithdrawalItem.text = item.withdrawal

            val context = binding.root.context
            val colorRise = ContextCompat.getColor(context, R.color.color_rise)
            val colorFall = ContextCompat.getColor(context, R.color.color_fall)

            fun getColorByValue(value: String): Int {
                return if (value.trim().startsWith("-")) colorFall else colorRise
            }

            binding.textDailyProfitItem.setTextColor(getColorByValue(item.dailyProfit))
            binding.textCumulativeProfitItem.setTextColor(getColorByValue(item.cumulativeProfit))
            binding.textDailyRateItem.setTextColor(getColorByValue(item.dailyRate))
            binding.textCumulativeRateItem.setTextColor(getColorByValue(item.cumulativeRate))
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MM월 dd일", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                outputFormat.format(date!!)
            } catch (e: Exception) {
                dateString // fallback
            }
        }
    }
}