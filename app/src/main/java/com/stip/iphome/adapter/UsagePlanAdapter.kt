package com.stip.stip.iphome.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.databinding.ItemUsagePlanBinding
import com.stip.stip.iphome.model.IpListingItem

class UsagePlanAdapter(
    private val item: IpListingItem,
    private val onShowUsagePlanClick: () -> Unit,
    private val onShowLicenseAgreementClick: () -> Unit
) : RecyclerView.Adapter<UsagePlanAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemUsagePlanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            binding.tvValueCurrentCirculation.text = item.currentCirculation ?: "정보 없음"

            binding.tvValueDigitalIpView.setOnClickListener {
                onShowUsagePlanClick()
            }

            binding.tvValueLicenseView.setOnClickListener {
                onShowLicenseAgreementClick()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUsagePlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int = 1
}
