package com.stip.stip.signup.signup.bank.select

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.stip.stip.signup.Constants
import com.stip.stip.R
import com.stip.stip.signup.base.BaseFragment
import com.stip.stip.databinding.FragmentSignUpBankSelectBinding
import com.stip.stip.signup.model.BankData
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class SignUpBankSelectFragment: BaseFragment<FragmentSignUpBankSelectBinding, SignUpBankSelectViewModel>() {
    override val layoutResource: Int
        get() = R.layout.fragment_sign_up_bank_select

    override val viewModel: SignUpBankSelectViewModel by viewModels()

    private lateinit var signUpBankSelectAdapter: SignUpBankSelectAdapter
    private lateinit var bankList: List<BankData>
    private lateinit var filteredList: MutableList<BankData>

    override fun initStartView() {
        bankList = getBankList()
        filteredList = bankList.toMutableList()
        signUpBankSelectAdapter = SignUpBankSelectAdapter(filteredList) { bankData ->
            val bundle = Bundle().apply {
                putSerializable(Constants.BUNDLE_SELECT_BANK_KEY, bankData)
            }
            findNavController().navigate(R.id.action_SignUpBankSelectFragment_to_SignUpBankFragment, bundle)
        }
        binding.rvSignUpBankList.adapter = signUpBankSelectAdapter
        setRxTextChangeCallback(binding.etSignUpBankSelect) {
            filterBankList(it)
        }
    }

    override fun initDataBinding() {
        with(viewModel) {
            isLoading.observe(viewLifecycleOwner) {

            }
        }
    }

    override fun initAfterBinding() {
    }

    private fun filterBankList(query: String) {
        val lowerCaseQuery = query.lowercase(Locale.getDefault())

        filteredList.clear()
        if (lowerCaseQuery.isEmpty()) {
            filteredList.addAll(bankList) // 검색어가 없으면 전체 리스트 표시
        } else {
            filteredList.addAll(bankList.filter { it.name.lowercase(Locale.getDefault()).contains(lowerCaseQuery) })
        }

        signUpBankSelectAdapter.notifyDataSetChanged()
    }

    private fun getBankList(): List<BankData> {
        val bankNames = resources.getStringArray(R.array.bank_name)
        val bankCodes = resources.getStringArray(R.array.bank_code)
        val bankIcons = resources.obtainTypedArray(R.array.bank_icon)

        val bankList = mutableListOf<BankData>()

        for (i in bankNames.indices) {
            val iconResId = bankIcons.getResourceId(i, R.drawable.ic_bank_ibk) // 기본 아이콘 설정
            bankList.add(BankData(iconResId, bankNames[i], bankCodes[i]))
        }

        bankIcons.recycle() // TypedArray 메모리 해제
        return bankList
    }
}