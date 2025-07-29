package com.stip.stip.order.adapter

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.databinding.ItemFilledOrderBinding
import com.stip.stip.iptransaction.model.IpInvestmentItem
import com.stip.stip.R

class FilledOrderAdapter :
    ListAdapter<IpInvestmentItem, FilledOrderAdapter.FilledOrderViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<IpInvestmentItem>() {
            override fun areItemsTheSame(
                oldItem: IpInvestmentItem,
                newItem: IpInvestmentItem
            ): Boolean {
                return oldItem.executionTime == newItem.executionTime && oldItem.name == newItem.name
            }

            override fun areContentsTheSame(
                oldItem: IpInvestmentItem,
                newItem: IpInvestmentItem
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class FilledOrderViewHolder(private val binding: ItemFilledOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: IpInvestmentItem) {
            val context = binding.root.context

            val isBuy = item.type == context.getString(R.string.order_type_buy)
            val typeTextRes = if (isBuy) R.string.order_type_buy else R.string.order_type_sell
            val colorRes = if (isBuy) R.color.percentage_positive_red else R.color.percentage_negative_blue
            val color = ContextCompat.getColor(context, colorRes)

            val ticker = item.name
            val typeText = context.getString(typeTextRes)
            val fullText = "$ticker $typeText"
            val spannable = SpannableString(fullText).apply {
                setSpan(
                    ForegroundColorSpan(color),
                    ticker.length + 1,
                    fullText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            binding.textNameType.text = spannable
            binding.textExecutionTime.text = item.executionTime
            binding.textUnitPrice.text = item.unitPrice
            binding.textQuantity.text = item.quantity
            binding.textAmount.text = item.amount
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilledOrderViewHolder {
        val binding = ItemFilledOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FilledOrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FilledOrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
