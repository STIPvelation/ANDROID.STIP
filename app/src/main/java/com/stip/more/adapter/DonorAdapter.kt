package com.stip.stip.more.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import com.stip.stip.more.model.DonorModel

class DonorAdapter(private val donors: List<DonorModel>) : 
    RecyclerView.Adapter<DonorAdapter.DonorViewHolder>() {

    class DonorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDonorInitial: TextView = view.findViewById(R.id.tvDonorInitial)
        val tvDonorName: TextView = view.findViewById(R.id.tvDonorName)
        val tvDonationCount: TextView = view.findViewById(R.id.tvDonationCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ip_donor_badge, parent, false)
        return DonorViewHolder(view)
    }

    override fun onBindViewHolder(holder: DonorViewHolder, position: Int) {
        val donor = donors[position]
        holder.tvDonorInitial.text = donor.name.first().toString()
        holder.tvDonorName.text = donor.name
        holder.tvDonationCount.text = "${donor.donationCount}개"
        
        // 현재 사용자인 경우 색상을 변경
        if (donor.isCurrentUser) {
            holder.tvDonorInitial.setBackgroundResource(R.drawable.bg_circle_accent)
        } else {
            holder.tvDonorInitial.setBackgroundResource(R.drawable.bg_circle_deep_navy)
        }
    }

    override fun getItemCount() = donors.size
}
