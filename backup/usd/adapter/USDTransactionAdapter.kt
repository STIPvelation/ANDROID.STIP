package com.stip.ipasset.usd.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stip.ipasset.usd.model.USDDepositTransaction
import com.stip.ipasset.usd.model.USDWithdrawalTransaction
import com.stip.stip.R
import com.stip.stip.databinding.ItemUsdDepositTransactionBinding
import com.stip.stip.databinding.ItemUsdWithdrawalTransactionBinding

/**
 * USD 입출금 트랜잭션 어댑터
 * 입금 완료와 출금 완료 두 가지 타입의 트랜잭션을 처리합니다.
 */
class USDTransactionAdapter(
    private val onItemClick: (Any) -> Unit
) : ListAdapter<Any, RecyclerView.ViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            VIEW_TYPE_DEPOSIT -> {
                val binding = ItemUsdDepositTransactionBinding.inflate(
                    inflater, parent, false
                )
                DepositViewHolder(binding, onItemClick)
            }
            VIEW_TYPE_WITHDRAWAL -> {
                val binding = ItemUsdWithdrawalTransactionBinding.inflate(
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
            is DepositViewHolder -> holder.bind(item as USDDepositTransaction)
            is WithdrawalViewHolder -> holder.bind(item as USDWithdrawalTransaction)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is USDDepositTransaction -> VIEW_TYPE_DEPOSIT
            is USDWithdrawalTransaction -> VIEW_TYPE_WITHDRAWAL
            else -> throw IllegalArgumentException("Unknown item type at position $position")
        }
    }

    /**
     * 입금 완료 ViewHolder
     */
    class DepositViewHolder(
        private val binding: ItemUsdDepositTransactionBinding,
        private val onItemClick: (USDDepositTransaction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: USDDepositTransaction) {
            binding.apply {
                // 날짜 및 시간 설정
                tvDate.text = item.getFormattedDate()
                tvTime.text = item.getFormattedTime()

                // 금액 설정
                tvAmountUsd.text = item.getFormattedUsdAmount()
                tvAmountKrw.text = item.getFormattedKrwAmount()

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
        private val binding: ItemUsdWithdrawalTransactionBinding,
        private val onItemClick: (USDWithdrawalTransaction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: USDWithdrawalTransaction) {
            binding.apply {
                // 날짜 및 시간 설정
                tvDate.text = item.getFormattedDate()
                tvTime.text = item.getFormattedTime()

                // 금액 설정
                tvAmountUsd.text = item.getFormattedUsdAmount()
                tvAmountKrw.text = item.getFormattedKrwAmount()

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
                oldItem is USDDepositTransaction && newItem is USDDepositTransaction ->
                    oldItem.id == newItem.id
                oldItem is USDWithdrawalTransaction && newItem is USDWithdrawalTransaction ->
                    oldItem.id == newItem.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is USDDepositTransaction && newItem is USDDepositTransaction ->
                    oldItem == newItem
                oldItem is USDWithdrawalTransaction && newItem is USDWithdrawalTransaction ->
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