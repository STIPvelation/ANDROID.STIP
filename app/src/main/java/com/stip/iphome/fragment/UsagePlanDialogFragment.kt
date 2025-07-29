package com.stip.stip.iphome.fragment // 실제 프로젝트 패키지

// ... (다른 import 문들 동일) ...
import android.graphics.Color
import android.graphics.Typeface // Typeface import 확인
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.stip.stip.databinding.DipUsagePlanBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointF
import java.io.Serializable
import java.util.ArrayList
import java.util.HashMap

class UsagePlanDialogFragment : DialogFragment() {

    // ... (멤버 변수 _binding, dynamicUsageData, sampleUsageData 등 동일) ...
    private var _binding: DipUsagePlanBinding? = null
    private val binding get() = _binding!!
    private var dynamicUsageData: Map<String, Float>? = null
    private val sampleUsageData = mapOf( /* ... 샘플 데이터 ... */
        "해외 지사" to 153.43f, "회사 브랜딩" to 229.81f, "급여" to 80.13f,
        "공과금" to 205.97f, "사업확장" to 154.54f, "설비 확충 " to 176.34f,
        "사내 복지" to 182.76f, "사내 시스템" to 205.89f, "클라우드 비용" to 136.61f,
        "출장비" to 160.83f, "마케팅" to 154.45f, "그외" to 150.29f
    )


    // ... (onCreate, onCreateView, onViewCreated, onStart 등 동일) ...
    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val hashMap = getSerializableCompat(it, ARG_USAGE_DATA, HashMap::class.java)
            if (hashMap != null) {
                // Safely convert HashMap to Map<String, Float> by verifying types
                try {
                    // Create a new map with verified types
                    val typedMap = hashMap.entries.associate { entry ->
                        val key = entry.key as? String
                        val value = entry.value as? Float
                        if (key == null || value == null) {
                            throw ClassCastException("Invalid key or value type in HashMap")
                        }
                        key to value
                    }
                    dynamicUsageData = typedMap
                } catch (e: Exception) {
                    Log.e(TAG, "Error casting usage data: ${e.message}", e)
                    dynamicUsageData = null
                }
            }
            Log.d(TAG, "Received usage data: ${dynamicUsageData?.size ?: "null (using sample)"}")
        } ?: Log.d(TAG, "No arguments received, using sample data.")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DipUsagePlanBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ticker = arguments?.getString(ARG_TICKER) ?: "IP"
        binding.usagePlanPieChart.centerText = ticker

        setupPieChart()
        loadPieChartData()

        binding.buttonCloseUsageDialog.setOnClickListener {
            dismiss()
        }
    }



    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }


    // --- setupPieChart 함수 수정: setCenterTextSize 추가 ---
    private fun setupPieChart() {
        val ticker = arguments?.getString(ARG_TICKER) ?: "IP" // ← 티커 연동

        binding.usagePlanPieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f

            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f

            setDrawCenterText(true)
            centerText = ticker // ← generateCenterSpannableText() 대신 티커 직접 설정
            setCenterTextSize(38f)

            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400, Easing.EaseInOutQuad)
            legend.isEnabled = false
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(0f)
            setDrawEntryLabels(false)
        }
    }


    private fun loadPieChartData() {
        // ... (이전과 동일) ...
        val entries = ArrayList<PieEntry>()
        val dataToUse = dynamicUsageData ?: sampleUsageData
        Log.d(TAG, "Loading chart with ${if(dynamicUsageData != null) "dynamic" else "sample"} data.")

        dataToUse.forEach { (label, value) ->
            entries.add(PieEntry(value, ""))
        }

        if (entries.isEmpty()) {
            Log.w(TAG, "No data to display in PieChart.")
            binding.usagePlanPieChart.data = null
            binding.usagePlanPieChart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "Usage Plan")
        dataSet.setDrawIcons(false); dataSet.sliceSpace = 3f; dataSet.iconsOffset = MPPointF(0f, 40f); dataSet.selectionShift = 5f;
        val colors = ArrayList<Int>(); for (c in ColorTemplate.VORDIPLOM_COLORS) colors.add(c); for (c in ColorTemplate.JOYFUL_COLORS) colors.add(c); for (c in ColorTemplate.COLORFUL_COLORS) colors.add(c); for (c in ColorTemplate.LIBERTY_COLORS) colors.add(c); for (c in ColorTemplate.PASTEL_COLORS) colors.add(c); colors.add(ColorTemplate.getHoloBlue()); dataSet.colors = colors;

        val data = PieData(dataSet); data.setValueFormatter(PercentFormatter(binding.usagePlanPieChart)); data.setValueTextSize(11f); data.setValueTextColor(Color.BLACK);

        binding.usagePlanPieChart.data = data
        binding.usagePlanPieChart.highlightValues(null)
        binding.usagePlanPieChart.invalidate()
    }

    // --- generateCenterSpannableText 함수 수정: RelativeSizeSpan 주석 처리 확인 ---
    private fun generateCenterSpannableText(): SpannableString {
        val s = SpannableString("")
        // 스타일 적용 (Bold 만 적용)
        // s.setSpan(RelativeSizeSpan(1.7f), 0, s.length, 0) // 이 줄은 주석 처리하거나 삭제 (setCenterTextSize 사용)
        s.setSpan(StyleSpan(Typeface.BOLD), 0, s.length, 0)
        // s.setSpan(ForegroundColorSpan(Color.DKGRAY), 0, s.length, 0) // 필요시 색상 설정
        return s
    }
    // --- generateCenterSpannableText 함수 수정 끝 ---


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "UsagePlanDialog"
        private const val ARG_USAGE_DATA = "arg_usage_data"
        private const val ARG_TICKER = "arg_ticker"

        fun newInstance(
            usagePlanData: Map<String, Float>? = null,
            ticker: String? = null
        ): UsagePlanDialogFragment {
            return UsagePlanDialogFragment().apply {
                arguments = Bundle().apply {
                    if (usagePlanData != null) {
                        putSerializable(ARG_USAGE_DATA, HashMap(usagePlanData))
                    }
                    if (ticker != null) {
                        putString(ARG_TICKER, ticker)
                    }
                }
            }
        }
    }
}


@Suppress("DEPRECATION", "UNCHECKED_CAST")
        private fun <T : Serializable?> getSerializableCompat(bundle: Bundle, key: String, clazz: Class<T>): T? {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                bundle.getSerializable(key, clazz)
            } else {
                bundle.getSerializable(key) as? T
            }
        }

