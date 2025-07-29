package com.stip.ipasset.usd.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stip.ipasset.usd.model.*
import com.stip.stip.R
import com.stip.stip.databinding.ItemUsdDepositTransactionBinding
import com.stip.stip.databinding.ItemUsdProcessTransactionBinding
import com.stip.stip.databinding.ItemUsdReturnTransactionBinding
import com.stip.stip.databinding.ItemUsdWithdrawalTransactionBinding

/**
 * USD 입출금 트랜잭션 어댑터
 * 입금 완료, 출금 완료, 입금 진행중, 출금 진행중, 입금 반려, 출금 반려 여섯 가지 타입의 트랜잭션을 처리합니다.
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
            VIEW_TYPE_RETURN -> {
                val binding = ItemUsdReturnTransactionBinding.inflate(
                    inflater, parent, false
                )
                ReturnViewHolder(binding, onItemClick)
            }
            VIEW_TYPE_PROCESS -> {
                val binding = ItemUsdProcessTransactionBinding.inflate(
                    inflater, parent, false
                )
                ProcessViewHolder(binding, onItemClick)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is DepositViewHolder -> holder.bind(item as USDDepositTransaction)
            is WithdrawalViewHolder -> holder.bind(item as USDWithdrawalTransaction)
            is ReturnViewHolder -> holder.bind(item as USDReturnTransaction)
            is ProcessViewHolder -> holder.bind(item as USDProcessTransaction)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is USDDepositTransaction -> VIEW_TYPE_DEPOSIT
            is USDWithdrawalTransaction -> VIEW_TYPE_WITHDRAWAL
            is USDReturnTransaction -> VIEW_TYPE_RETURN
            is USDProcessTransaction -> VIEW_TYPE_PROCESS
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
     * 반환됨 ViewHolder
     */
    class ReturnViewHolder(
        private val binding: ItemUsdReturnTransactionBinding,
        private val onItemClick: (USDReturnTransaction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: USDReturnTransaction) {
            binding.apply {
                // 날짜 및 시간 설정
                tvDate.text = item.date
                tvTime.text = item.time

                // 금액 설정
                tvAmountUsd.text = String.format("%.2f USD", item.usdAmount)
                tvAmountKrw.text = String.format("%,d KRW", item.krwAmount)

                // 상태 설정 (반환됨)
                tvStatus.text = item.status
                tvStatus.background = ContextCompat.getDrawable(
                    itemView.context,
                    R.drawable.bg_status_return
                )

                // 클릭 이벤트
                root.setOnClickListener { onItemClick(item) }
            }
        }
    }

    /**
     * 진행중 ViewHolder
     */
    class ProcessViewHolder(
        private val binding: ItemUsdProcessTransactionBinding,
        private val onItemClick: (USDProcessTransaction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: USDProcessTransaction) {
            binding.apply {
                // 날짜 및 시간 설정
                tvDate.text = item.date
                tvTime.text = item.time

                // 금액 설정
                tvAmountUsd.text = String.format("%.2f USD", item.usdAmount)
                tvAmountKrw.text = String.format("%,d KRW", item.krwAmount)

                // 상태 설정 (진행중)
                tvStatus.text = item.status
                tvStatus.background = ContextCompat.getDrawable(
                    itemView.context,
                    R.drawable.bg_status_in_progress
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
                oldItem is USDReturnTransaction && newItem is USDReturnTransaction ->
                    oldItem.id == newItem.id
                oldItem is USDProcessTransaction && newItem is USDProcessTransaction ->
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
                oldItem is USDReturnTransaction && newItem is USDReturnTransaction ->
                    oldItem == newItem
                oldItem is USDProcessTransaction && newItem is USDProcessTransaction ->
                    oldItem == newItem
                else -> false
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_DEPOSIT = 1
        private const val VIEW_TYPE_WITHDRAWAL = 2
        private const val VIEW_TYPE_RETURN = 3
        private const val VIEW_TYPE_PROCESS = 4
    }
}