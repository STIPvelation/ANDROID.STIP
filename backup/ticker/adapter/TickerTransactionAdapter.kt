package com.stip.ipasset.ticker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stip.ipasset.ticker.model.TickerDepositTransaction
import com.stip.ipasset.ticker.model.TickerWithdrawalTransaction
import com.stip.stip.R
import com.stip.stip.databinding.ItemTickerDepositTransactionBinding
import com.stip.stip.databinding.ItemTickerWithdrawalTransactionBinding

/**
 * 티커 입출금 트랜잭션 어댑터
 * 입금 완료와 출금 완료 두 가지 타입의 트랜잭션을 처리합니다.
 */
class TickerTransactionAdapter(
    private val onItemClick: (Any) -> Unit
) : ListAdapter<Any, RecyclerView.ViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            VIEW_TYPE_DEPOSIT -> {
                val binding = ItemTickerDepositTransactionBinding.inflate(
                    inflater, parent, false
                )
                DepositViewHolder(binding, onItemClick)
            }
            VIEW_TYPE_WITHDRAWAL -> {
                val binding = ItemTickerWithdrawalTransactionBinding.inflate(
                    inflater, parent, false
                )
                WithdrawalViewHolder(binding, onItemClick)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is DepositViewHolder -> holder.bind(item as TickerDepositTransaction)
            is WithdrawalViewHolder -> holder.bind(item as TickerWithdrawalTransaction)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TickerDepositTransaction -> VIEW_TYPE_DEPOSIT
            is TickerWithdrawalTransaction -> VIEW_TYPE_WITHDRAWAL
            else -> throw IllegalArgumentException("Unknown item type at position $position")
        }
    }

    /**
     * 입금 완료 ViewHolder
     */
    class DepositViewHolder(
        private val binding: ItemTickerDepositTransactionBinding,
        private val onItemClick: (TickerDepositTransaction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TickerDepositTransaction) {
            binding.apply {
                // 날짜 및 시간 설정
                tvDate.text = item.getFormattedDate()
                tvTime.text = item.getFormattedTime()

                // 금액 설정
                tvAmountTicker.text = item.getFormattedTickerAmount()
                tvAmountUsd.text = item.getFormattedUsdAmount()

                // 상태 설정 (입금 완료)
                tvStatus.text = item.status
                tvStatus.background = ContextCompat.getDrawable(
                    itemView.context,
                    R.drawable.bg_status_deposit
                )

                // 클릭 이벤트
                root.setOnClickListener { onItemClick(item) }
            }
        }
    }

    /**
     * 출금 완료 ViewHolder
     */
    class WithdrawalViewHolder(
        private val binding: ItemTickerWithdrawalTransactionBinding,
        private val onItemClick: (TickerWithdrawalTransaction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TickerWithdrawalTransaction) {
            binding.apply {
                // 날짜 및 시간 설정
                tvDate.text = item.getFormattedDate()
                tvTime.text = item.getFormattedTime()

                // 금액 설정
                tvAmountTicker.text = item.getFormattedTickerAmount()
                tvAmountUsd.text = item.getFormattedUsdAmount()

                // 상태 설정 (출금 완료)
                tvStatus.text = item.status
                tvStatus.background = ContextCompat.getDrawable(
                    itemView.context,
                    R.drawable.bg_status_withdrawal
                )

                // 클릭 이벤트
                root.setOnClickListener { onItemClick(item) }
            }
        }
    }

    /**
     * DiffUtil Callback
     */
    class TransactionDiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is TickerDepositTransaction && newItem is TickerDepositTransaction ->
                    oldItem.id == newItem.id
                oldItem is TickerWithdrawalTransaction && newItem is TickerWithdrawalTransaction ->
                    oldItem.id == newItem.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is TickerDepositTransaction && newItem is TickerDepositTransaction ->
                    oldItem == newItem
                oldItem is TickerWithdrawalTransaction && newItem is TickerWithdrawalTransaction ->
                    oldItem == newItem
                else -> false
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_DEPOSIT = 1
        private const val VIEW_TYPE_WITHDRAWAL = 2
    }
}
