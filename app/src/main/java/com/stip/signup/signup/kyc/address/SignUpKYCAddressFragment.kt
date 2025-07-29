package com.stip.stip.signup.signup.kyc.address

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.stip.stip.R
import com.stip.stip.databinding.FragmentSignUpKycAddressBinding
import com.stip.stip.more.activity.AddressSearchActivity
import com.stip.stip.signup.Constants
import com.stip.stip.signup.base.BaseFragment
import com.stip.stip.signup.clear
import com.stip.stip.signup.customview.AddressBottomSheet
import com.stip.stip.signup.focusCallback
import com.stip.stip.signup.signup.SignUpViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpKYCAddressFragment :
    BaseFragment<FragmentSignUpKycAddressBinding, SignUpKYCAddressViewModel>() {
    override val layoutResource: Int
        get() = R.layout.fragment_sign_up_kyc_address

    override val viewModel: SignUpKYCAddressViewModel by viewModels()
    private val activityViewModel: SignUpViewModel by activityViewModels()
    lateinit var addressAdapter: SignUpKYCAddressAdapter

    //   초기 뷰 설정
    override fun initStartView() {
        binding.etSignUpKycSearchAddress.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // 검색 실행 코드
                viewModel.requestGetZipCodeKakao(
                    Constants.KAKAO_LOCATION_KEY,
                    binding.etSignUpKycSearchAddress.text.toString()
                )
                true
            } else {
                false
            }
        }

//        포커스가 될 경우 카카오 API 호출
        binding.etSignUpKycSearchAddress.focusCallback { isFocus ->
            if (!isFocus) {
                binding.ivSignUpKycDel.visibility = View.GONE
                binding.ivSignUpKycSearchHint.visibility = View.VISIBLE
            } else {
                setRxTextChangeCallback(binding.etSignUpKycSearchAddress) {
                    binding.ivSignUpKycSearchHint.visibility = View.GONE

                    if (it.isNotBlank()) {
                        binding.ivSignUpKycDel.visibility = View.VISIBLE
                    } else {
                        binding.ivSignUpKycDel.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun initDataBinding() {
        with(viewModel) {
            signUpKYCZipCodeKakaoLiveData.observe(viewLifecycleOwner) {
                if (it.documents.isEmpty()) {
                    showToast(R.string.sign_up_kyc_address_not_found)
                } else {
                    addressAdapter = SignUpKYCAddressAdapter(
                        it.documents.toMutableList(),
                        { zipCode, newAddress ->

                            // zipCode 검증
                            if (zipCode.isNullOrBlank()) {
                                showToast(R.string.sign_up_kyc_address_zipcode_missing)
                                return@SignUpKYCAddressAdapter
                            }

                            if (newAddress.isNullOrBlank()) {
                                showToast(R.string.sign_up_kyc_address_not_found)
                            } else {
                                AddressBottomSheet(
                                    zipCode ?: "",
                                    newAddress,
                                    { detailAddress ->
                                        // KYC 화면으로 데이터 전달
                                        activityViewModel.address.value = newAddress
                                        activityViewModel.addressDetail.value = detailAddress
                                        activityViewModel.postalCode.value = zipCode

                                        val resultAddress = "$zipCode $newAddress $detailAddress"
                                        val bundle = Bundle().apply {
                                            putString(
                                                Constants.BUNDLE_KYC_ADDRESS_KEY,
                                                resultAddress
                                            )
                                        }
                                        if (requireActivity() is AddressSearchActivity) {
                                            Log.d("SignUpKYCAddressFragment", "AddressSearchActivity로 돌아감 + ${bundle.toString()}")
                                            requireActivity().setResult(
                                                AppCompatActivity.RESULT_OK,
                                                Intent().apply { putExtras(Bundle().apply {
                                                    putString(AddressSearchActivity.EXTRA_ADDRESS, newAddress)
                                                    putString(AddressSearchActivity.EXTRA_ADDRESS_DETAIL, detailAddress)
                                                    putString(AddressSearchActivity.EXTRA_POSTAL_CODE, zipCode)
                                                }) }
                                            )
                                            requireActivity().finish()
                                        } else {
                                            // 회원가입 플로우: 기존처럼 다음 프래그먼트로 이동
                                            findNavController().navigate(
                                                R.id.action_SignUpKYCAddressFragment_to_SignUpKYCInformFragment,
                                                bundle
                                            )
                                        }
                                    }
                                ).show(parentFragmentManager, "address")
                            }
                            // 도로명 클릭

                        },
                        { zipCode, oldAddress ->

                            // zipCode 검증
                            if (zipCode.isNullOrBlank()) {
                                showToast(R.string.sign_up_kyc_address_zipcode_missing)
                                return@SignUpKYCAddressAdapter
                            }

                            if (oldAddress.isNullOrBlank()) {
                                // 해당 주소 값 없음
                                showToast(R.string.sign_up_kyc_address_not_found)
                            } else {
                                AddressBottomSheet(
                                    zipCode ?: "",
                                    oldAddress,
                                    { detailAddress ->
                                        // KYC 화면으로 데이터 전달
                                        activityViewModel.address.value = oldAddress
                                        activityViewModel.addressDetail.value = detailAddress
                                        activityViewModel.postalCode.value = zipCode

                                        val resultAddress = "$zipCode $oldAddress $detailAddress"
                                        val bundle = Bundle().apply {
                                            putString(
                                                Constants.BUNDLE_KYC_ADDRESS_KEY,
                                                resultAddress
                                            )
                                        }

                                        findNavController().navigate(
                                            R.id.action_SignUpKYCAddressFragment_to_SignUpKYCInformFragment,
                                            bundle
                                        )
                                    }
                                ).show(parentFragmentManager, "address")
                            }

                        })
                    binding.rvAddress.adapter = addressAdapter
                }
            }
        }
    }

    override fun initAfterBinding() {
        setOnClick(binding.ivBack) {
            findNavController().navigateUp()
        }

        setOnClick(binding.ivSignUpKycDel) {
            binding.etSignUpKycSearchAddress.clear()
        }

        setOnClick(binding.ivSignUpKycSearch) {
            viewModel.requestGetZipCodeKakao(
                Constants.KAKAO_LOCATION_KEY,
                binding.etSignUpKycSearchAddress.text.toString()
            )
        }
    }
}