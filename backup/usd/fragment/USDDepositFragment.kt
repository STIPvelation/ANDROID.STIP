package com.stip.ipasset.usd.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.stip.stip.R
import com.stip.stip.databinding.FragmentIpAssetUsdDepositBinding
import com.stip.stip.databinding.LayoutDepositUsdBinding
import com.stip.ipasset.usd.model.DepositViewModel
import com.stip.stip.ipasset.adapter.AccountInfoDecorator
import com.stip.stip.ipasset.adapter.AccountInfoListAdapter
import com.stip.stip.ipasset.extension.dpToPx
import com.stip.stip.ipasset.fragment.BaseFragment
import com.stip.stip.ipasset.model.IpAsset
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * USD 입금을 처리하는 Fragment
 * 계좌 정보 목록을 표시합니다.
 */
@AndroidEntryPoint
class USDDepositFragment : BaseFragment<FragmentIpAssetUsdDepositBinding>() {
    private val args by navArgs<com.stip.ipasset.usd.fragment.USDDepositFragmentArgs>()
    private val ipAsset: IpAsset get() = args.ipAsset
    private val currencyCode: String get() = ipAsset.currencyCode

    private val viewModel by viewModels<DepositViewModel>()
    private val adapter = AccountInfoListAdapter()

    private fun bind() = with(viewBinding) {
        materialToolbar.title = "$currencyCode ${getString(R.string.common_deposit_action)}"
        materialToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // USD 입금 레이아웃 바인딩
        bindUsdLayout(layoutUsd)
    }

    private fun bindUsdLayout(binding: LayoutDepositUsdBinding) = with(binding) {
        val space = requireContext().dpToPx(4.0F)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.addItemDecoration(AccountInfoDecorator(space = space.toInt()))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind()

        // USD 계좌 정보 가져오기
        viewModel.fetchAccountInfo(currencyCode)

        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                viewModel.depositAccountInfoList.collect {
                    adapter.submitList(it)
                }
            }
        }
    }

    override fun inflate(inflater: LayoutInflater, container: ViewGroup?): FragmentIpAssetUsdDepositBinding {
        return FragmentIpAssetUsdDepositBinding.inflate(inflater, container, false)
    }
}
