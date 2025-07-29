package com.stip.stip.signup.customview

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import com.stip.stip.R
import android.view.Gravity
import com.stip.stip.databinding.DialogCustomLoadingBinding

class LoadingDialog(
    context: Context
) : Dialog(context, R.style.PopUpDialog) { // PopUpDialog 스타일이 너비를 제한하지 않는지 확인 필요

    private lateinit var binding: DialogCustomLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.dialog_custom_loading, // 이 XML의 루트 레이아웃 설정을 확인하세요.
            null,
            false
        )
        setContentView(binding.root)

        window?.apply {
            // 1. 다이얼로그 윈도우 자체의 크기 설정 (가장 중요)
            // 너비를 화면 전체로, 높이는 내용에 맞게 설정합니다.
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            // 2. 다이얼로그 윈도우 배경을 투명하게 처리 (커스텀 배경을 온전히 표시하기 위함)
            setBackgroundDrawableResource(android.R.color.transparent)

            // 3. 다이얼로그 외부 어둡게 처리 (Dim Amount)
            setDimAmount(0.5f)

            // 4. 다이얼로그 위치 설정 (보통 중앙)
            // attributes를 수정할 때는 null 가능성을 고려하는 것이 더 안전합니다.
            this.attributes = this.attributes?.apply {
                gravity = Gravity.CENTER
            }

            // 5. (선택 사항) 시스템 기본 패딩 제거
            // 간혹 시스템 테마에 의해 원치 않는 패딩이 적용될 수 있습니다.
            // 필요하다면 decorView의 패딩을 0으로 설정할 수 있습니다.
            // decorView.setPadding(0, 0, 0, 0)
        }

        // 6. ContentView (binding.root)의 LayoutParams 설정
        // R.layout.dialog_custom_loading XML 파일의 루트 레이아웃 (예: FrameLayout)에서
        // android:layout_width="match_parent"
        // android:layout_height="wrap_content" (또는 의도에 따라 match_parent)
        // 로 이미 설정되어 있다면 아래 코드는 중복일 수 있습니다.
        // 하지만 명시적으로 설정하여 의도를 확실히 할 수 있습니다.
        // 이 설정은 다이얼로그 윈도우 내에서 콘텐츠 뷰가 어떻게 동작할지를 결정합니다.
        // FrameLayout.LayoutParams를 사용하여 MarginLayoutParams 문제 해결
        binding.root.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )

        // 7. 다이얼로그 취소 관련 설정
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }
}