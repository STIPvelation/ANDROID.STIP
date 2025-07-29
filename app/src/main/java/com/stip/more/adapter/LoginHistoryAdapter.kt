package com.stip.stip.more.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.databinding.ItemLoginHistoryBinding
import com.stip.stip.more.api.LoginHistoryItem

class LoginHistoryAdapter(private var items: List<LoginHistoryItem>) :
    RecyclerView.Adapter<LoginHistoryAdapter.LoginViewHolder>() {

    inner class LoginViewHolder(val binding: ItemLoginHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoginViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemLoginHistoryBinding.inflate(inflater, parent, false)
        return LoginViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LoginViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvDatetime.text = item.loginTime.toString()
        holder.binding.tvIpAddress.text = item.ipAddress
        holder.binding.tvOsVersion.text = item.deviceInfo
        holder.binding.tvLocation.text = item.location
    }

    override fun getItemCount(): Int = items.size
    
    fun updateItems(newItems: List<LoginHistoryItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}
