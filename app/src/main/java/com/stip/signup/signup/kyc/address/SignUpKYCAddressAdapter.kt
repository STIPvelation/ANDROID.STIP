package com.stip.stip.signup.signup.kyc.address

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.databinding.ItemRvAddressBinding
import com.stip.stip.signup.base.BaseViewHolder
import com.stip.stip.signup.model.ZipCodeDocument

class SignUpKYCAddressAdapter(
    private val itemList: MutableList<ZipCodeDocument>,
    private val newClick: ((String?, String?) -> Unit),
    private val oldClick: ((String?, String?) -> Unit)
): RecyclerView.Adapter<BaseViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val binding = ItemRvAddressBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AddressViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class AddressViewHolder(private var binding: ItemRvAddressBinding): BaseViewHolder(binding.root) {
        override fun bind(position: Int) {
            val itemData = itemList[position]

            /*if (itemData.road_address?.zone_no?.isBlank() == true) {
                binding.tvSignUpKycAddressZipCode.visibility = View.GONE
            } else {
                binding.tvSignUpKycAddressZipCode.visibility = View.VISIBLE
            }

            if (itemData.address == null) {
                binding.clSignUpKycOldAddressSection.visibility = View.GONE
            } else {
                binding.clSignUpKycOldAddressSection.visibility = View.VISIBLE
            }

            if (itemData.road_address == null) {
                binding.clSignUpKycNewAddressSection.visibility = View.GONE
            } else {
                binding.clSignUpKycNewAddressSection.visibility = View.VISIBLE
            }*/

            val zipCode = itemData.road_address?.zone_no ?: ""
            val newAddress = itemData.road_address?.address_name ?: ""
            val oldAddress = itemData.address_name ?: ""

            binding.tvSignUpKycAddressZipCode.text = zipCode
            binding.tvSignUpKycAddressNewValue.text = newAddress
            binding.tvSignUpKycAddressOldValue.text = oldAddress

            binding.clSignUpKycNewAddressSection.setOnClickListener {
                newClick.invoke(
                    zipCode,
                    newAddress
                )
            }

            binding.clSignUpKycOldAddressSection.setOnClickListener {
                oldClick.invoke(
                    zipCode,
                    oldAddress
                )
            }
        }
    }
}