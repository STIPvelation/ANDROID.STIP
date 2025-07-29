package com.stip.stip.iphome.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import com.stip.stip.iptransaction.model.UnfilledOrder
import com.stip.stip.databinding.ItemUnfilledHeaderBinding
import com.stip.stip.databinding.ItemUnfilledItemBinding

class UnfilledOrderAdapter(
    private var items: List<UnfilledOrder> = emptyList()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
        private const val TAG = "UnfilledOrderAdapter"
    }

    var onSelectionChanged: ((Boolean) -> Unit)? = null
    private var checkedStates = BooleanArray(items.size) { false }

    override fun getItemCount(): Int = items.size + 1

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            val binding = ItemUnfilledHeaderBinding.inflate(inflater, parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = ItemUnfilledItemBinding.inflate(inflater, parent, false)
            ItemViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            val index = position - 1
            if (index in items.indices) {
                holder.bind(items[index], index)
            } else {
                Log.e(TAG, "Attempting to bind item at invalid index: $index")
            }
        }
    }

    fun submitList(newItems: List<UnfilledOrder>) {
        items = newItems
        checkedStates = BooleanArray(newItems.size) { false }
        notifyDataSetChanged()
        onSelectionChanged?.invoke(false)
    }

    fun getSelectedOrderIds(): List<String> {
        return items.filterIndexed { index, _ -> checkedStates.getOrElse(index) { false } }
            .map { it.orderId }
    }

    fun hasCheckedItems(): Boolean = checkedStates.any { it }

    fun clearSelection() {
        // 선택된 항목이 있든 없든 항상 초기화
        checkedStates.fill(false)
        notifyDataSetChanged()
        onSelectionChanged?.invoke(false)
        Log.d(TAG, "선택 상태 초기화 완료")
    }

    inner class HeaderViewHolder(binding: ItemUnfilledHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class ItemViewHolder(private val binding: ItemUnfilledItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UnfilledOrder, index: Int) {
            val context = itemView.context

            binding.tickerText.text = item.ticker
            binding.watchPriceText.text = item.watchPrice
            binding.orderPriceText.text = item.orderPrice
            binding.orderQuantityText.text = item.orderQuantity
            binding.unfilledQuantityText.text = item.unfilledQuantity
            binding.orderTimeText.text = item.orderTime

            // ✅ tradeType 텍스트와 색상 처리
            val isBuy = item.tradeType == "매수"
            val tradeTextRes = if (isBuy) R.string.order_type_buy else R.string.order_type_sell
            val tradeColorRes = if (isBuy) R.color.percentage_positive_red else R.color.percentage_negative_blue

            binding.tradeTypeText.text = context.getString(tradeTextRes)
            binding.tradeTypeText.setTextColor(ContextCompat.getColor(context, tradeColorRes))

            // ✅ 체크박스 상태 처리
            val isChecked = checkedStates.getOrElse(index) { false }
            binding.checkIcon.setImageResource(
                if (isChecked) R.drawable.ic_circle_check_filled
                else R.drawable.ic_circle_check_outline
            )

            val toggle = { toggleSelection(index) }
            binding.checkIcon.setOnClickListener { toggle() }
            itemView.setOnClickListener { toggle() }
        }

        private fun toggleSelection(index: Int) {
            if (index in checkedStates.indices) {
                checkedStates[index] = !checkedStates[index]
                notifyItemChanged(index + 1)
                onSelectionChanged?.invoke(hasCheckedItems())
            } else {
                Log.e(TAG, "Invalid index on toggle: $index")
            }
        }
    }
}
