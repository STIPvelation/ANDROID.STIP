package com.stip.stip.more.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.databinding.ItemCountryBinding
import com.stip.stip.more.model.Country

class CountryAdapter : ListAdapter<Country, CountryAdapter.CountryViewHolder>(CountryDiffCallback()) {

    private var onItemClickListener: ((Country, Boolean) -> Unit)? = null

    fun setOnItemClickListener(listener: (Country, Boolean) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryViewHolder {
        val binding = ItemCountryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CountryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
        val country = getItem(position)
        holder.bind(country)
    }

    inner class CountryViewHolder(private val binding: ItemCountryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(country: Country) {
            binding.tvFlag.text = country.emoji
            binding.tvCountryName.text = country.name
            binding.cbCountrySelected.isChecked = country.isSelected
            
            // Korea (KR) should always be allowed and disabled from changing
            val isKorea = country.id == "KR"
            binding.cbCountrySelected.isEnabled = !isKorea
            if (isKorea) {
                country.isSelected = true
                binding.cbCountrySelected.isChecked = true
            }

            // Handle toggle switch change
            binding.cbCountrySelected.setOnCheckedChangeListener { _, isChecked ->
                if (!isKorea) { // Only allow changes for non-Korea countries
                    country.isSelected = isChecked
                    onItemClickListener?.invoke(country, isChecked)
                }
            }

            // Handle card click (also toggles switch)
            binding.root.setOnClickListener {
                if (!isKorea) { // Only allow changes for non-Korea countries
                    val newState = !binding.cbCountrySelected.isChecked
                    binding.cbCountrySelected.isChecked = newState
                    country.isSelected = newState
                    onItemClickListener?.invoke(country, newState)
                }
            }
        }
    }

    class CountryDiffCallback : DiffUtil.ItemCallback<Country>() {
        override fun areItemsTheSame(oldItem: Country, newItem: Country): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Country, newItem: Country): Boolean {
            return oldItem == newItem && oldItem.isSelected == newItem.isSelected
        }
    }
}
