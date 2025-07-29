package com.stip.stip.signup.intro

import androidx.lifecycle.ViewModel
import com.stip.stip.signup.api.repository.member.MemberRepository
import com.stip.stip.signup.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class IntroViewModel @Inject constructor(
    private val memberRepository: MemberRepository
): BaseViewModel() {
    suspend fun checkUserExistenceByDi(di: String) = memberRepository.getExistenceMember(di)
    suspend fun getMemberInfo() = memberRepository.getMembers()
}