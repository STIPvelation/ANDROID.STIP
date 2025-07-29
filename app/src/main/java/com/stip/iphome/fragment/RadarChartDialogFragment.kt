package com.stip.stip.iphome.fragment

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.content.Context
import androidx.fragment.app.DialogFragment
import com.stip.stip.R
import com.stip.stip.databinding.DialogRadarChartBinding
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.*
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.animation.Easing
import java.util.ArrayList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window

class RadarChartDialogFragment : DialogFragment() {


    private var _binding: DialogRadarChartBinding? = null

    private val binding get() = _binding!!


    // --- 멤버 변수 ---

    private var grade: String = "N/A"

    private var institutionalValues: ArrayList<Float>? = null

    private var stipValues: ArrayList<Float>? = null

    private var category: String = "Unknown" // 카테고리 멤버 변수 추가

    private lateinit var currentLabels: List<String> // 현재 사용할 레이블 리스트


    // --- ✅ 기본 레이블 정의 (Fallback 용) ---

    private val defaultLabels = listOf("항목1", "항목2", "항목3", "항목4", "항목5", "항목6")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            grade = it.getString(ARG_GRADE) ?: "N/A"
            institutionalValues = getFloatArrayListCompat(it, ARG_INST_VALUES)
            stipValues = getFloatArrayListCompat(it, ARG_STIP_VALUES)
            category = it.getString(ARG_CATEGORY) ?: "Unknown"
        }

        val localizedLabels = getCategoryLabels(requireContext())
        currentLabels = localizedLabels[category] ?: defaultLabels
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = DialogRadarChartBinding.inflate(inflater, container, false)

        dialog?.window?.apply {

            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            requestFeature(Window.FEATURE_NO_TITLE)

        }

        return binding.root

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ 랜덤 등급 설정 및 로그
        val possibleGrades = listOf("S", "A", "B", "C", "D")
        val randomGrade = possibleGrades.random()
        Log.d(TAG, "Setting random grade: $randomGrade (Original grade from args was: $grade)")

        // ✅ 등급 오버레이 텍스트 설정 및 스타일 적용
        binding.tvDialogGradeOverlayChart.apply {
            text = randomGrade
            setTextColor(Color.parseColor("#30C6E8"))
            setTypeface(null, Typeface.BOLD)
            textSize = 100f
        }

        // ✅ 차트 외형 및 데이터 설정
        setupRadarChartAppearance(binding.dialogRadarChart)
        loadRadarChartData(binding.dialogRadarChart)

        // ✅ 닫기 버튼 리스너
        binding.buttonCloseChartDialog.setOnClickListener { dismiss() }
    }

    override fun onStart() {

        super.onStart()

        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

    }


    private fun setupRadarChartAppearance(chart: RadarChart) {

        chart.apply {

            description.isEnabled = false

            webLineWidth = 1f

            webColor = Color.LTGRAY

            webLineWidthInner = 1f

            webColorInner = Color.LTGRAY

            isRotationEnabled = false


            // 웹 라인 점선 효과 시도

            // val dashPathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)

            // webLineDashPathEffect = dashPathEffect

            // webLineInnerDashPathEffect = dashPathEffect


            xAxis.apply {

                textSize = 11f

                yOffset = 0f

                textColor = Color.DKGRAY

                // --- ✅ 동적으로 결정된 레이블 사용 ---

                valueFormatter = IndexAxisValueFormatter(currentLabels)

            }



            yAxis.apply {

                setLabelCount(5, true)

                axisMinimum = 0f

                setDrawLabels(false)

            }



            legend.apply {

                // 범례 설정 (기존 유지)

                isEnabled = true

                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM

                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER

                orientation = Legend.LegendOrientation.HORIZONTAL

                setDrawInside(false)

                xEntrySpace = 10f

                yEntrySpace = 5f

                textColor = Color.DKGRAY

                textSize = 12f

                yOffset = 5f

                isWordWrapEnabled = true

            }

        }

    }


    private fun loadRadarChartData(chart: RadarChart) {
        val dataSize = currentLabels.size // 동적으로 결정된 레이블 사용
        // 데이터가 없을 경우 랜덤 샘플 데이터 사용 (기존 유지)
        val instValues = institutionalValues?.takeIf { it.size == dataSize }
            ?: List(dataSize) { (Math.random() * 80 + 20).toFloat() }
        val stipVals = stipValues?.takeIf { it.size == dataSize }
            ?: List(dataSize) { (Math.random() * 80 + 20).toFloat() }

        Log.d(TAG, "Using instValues: $instValues")
        Log.d(TAG, "Using stipValues: $stipVals")

        val entriesInstitutional = instValues.map { RadarEntry(it) }
        val entriesStip = stipVals.map { RadarEntry(it) }

        // --- ✅ 색상 및 투명도 정의 (요청사항 반영) ---
        // 기관 평가 (도형색 #FF7593, 40% Alpha)
        val institutionalColor = Color.parseColor("#FF7593") // 라인 색상도 일단 도형색과 동일하게 설정 (조정 가능)
        val institutionalFill = Color.parseColor("#FF7593")   // 도형(채우기) 색상
        val institutionalFillAlpha = (255 * 0.40).toInt()     // 40% 투명도 (102)

        // STIP 평가 (#F1E974, 10% Alpha)
        val stipColor = Color.parseColor("#F1E974")           // 라인 색상도 일단 도형색과 동일하게 설정 (조정 가능)
        val stipFill = Color.parseColor("#F1E974")            // 도형(채우기) 색상
        val stipFillAlpha = (255 * 0.10).toInt()              // 10% 투명도 (25)
        // --- ✅ 색상 정의 끝 ---

        // --- 기관 평가 데이터셋 ---
        val dataSetInstitutional = RadarDataSet(
            entriesInstitutional,
            getString(R.string.legend_label_institutional)
        ).apply {
            color = institutionalColor        // 라인 색상
            fillColor = institutionalFill     // 채우기 색상
            setDrawFilled(true)
            fillAlpha = institutionalFillAlpha // ✅ 수정된 투명도
            lineWidth = 2f
            setDrawValues(false)
            setDrawHighlightCircleEnabled(false)
            setDrawHighlightIndicators(false)
        }

        // --- STIP 평가 데이터셋 ---
        val dataSetStip = RadarDataSet(entriesStip, getString(R.string.legend_label_stip)).apply {
            color = stipColor                // ✅ 수정된 라인 색상
            fillColor = stipFill             // ✅ 수정된 채우기 색상
            setDrawFilled(true)
            fillAlpha = stipFillAlpha        // ✅ 수정된 투명도
            lineWidth = 2f
            setDrawValues(false)
            setDrawHighlightCircleEnabled(false)
            setDrawHighlightIndicators(false)
        }

        // --- 차트 데이터 설정 ---
        val radarData = RadarData(dataSetInstitutional, dataSetStip).apply {
            setValueTextSize(0f) // 값 텍스트 숨김
        }
        chart.data = radarData

        // --- 사용자 정의 범례 설정 (수정된 라인 색상 사용) ---
        chart.legend.setCustom(
            listOf(
                LegendEntry(
                    getString(R.string.legend_label_institutional),
                    Legend.LegendForm.SQUARE,
                    10f,
                    2f,
                    null,
                    institutionalColor
                ), // ✅ 수정된 기관 색상
                LegendEntry(
                    getString(R.string.legend_label_stip),
                    Legend.LegendForm.SQUARE,
                    10f,
                    2f,
                    null,
                    stipColor
                ) // ✅ 수정된 STIP 색상
            )
        )

        // 차트 새로고침 및 애니메이션
        chart.invalidate()
        chart.animateXY(800, 800, Easing.EaseInOutQuad)
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null

    }


    companion object {

        const val TAG = "RadarChartDialog"

        // --- Argument 키 정의 ---

        private const val ARG_GRADE = "arg_grade"

        private const val ARG_INST_VALUES = "arg_inst_values"

        private const val ARG_STIP_VALUES = "arg_stip_values"

        private const val ARG_CATEGORY = "arg_category" // ✅ 카테고리 키 추가


        // --- ✅ categoryLabels 맵 정의 ---

        private fun getCategoryLabels(context: Context): Map<String, List<String>> {
            val r = context.resources // ✅ 수정된 context 접근

            return mapOf(
                "Patent" to listOf(
                    r.getString(R.string.label_right),
                    r.getString(R.string.label_utilization),
                    r.getString(R.string.label_regulation),
                    r.getString(R.string.label_trade),
                    r.getString(R.string.label_capability),
                    r.getString(R.string.label_technology)
                ),
                "BM" to listOf(
                    r.getString(R.string.label_sustainability),
                    r.getString(R.string.label_collaboration),
                    r.getString(R.string.label_capability),
                    r.getString(R.string.label_right),
                    r.getString(R.string.label_trade),
                    r.getString(R.string.label_partnership)
                ),
                "Movie" to listOf(
                    r.getString(R.string.label_scalability),
                    r.getString(R.string.label_distribution),
                    r.getString(R.string.label_trade),
                    r.getString(R.string.label_partnership),
                    r.getString(R.string.label_ip_utilization),
                    r.getString(R.string.label_design)
                ),
                "Drama" to listOf(
                    r.getString(R.string.label_distribution),
                    r.getString(R.string.label_awareness),
                    r.getString(R.string.label_utilization),
                    r.getString(R.string.label_partnership),
                    r.getString(R.string.label_legal_stability),
                    r.getString(R.string.label_marketability)
                ),
                "Dance" to listOf(
                    r.getString(R.string.label_design),
                    r.getString(R.string.label_collaboration),
                    r.getString(R.string.label_awareness),
                    r.getString(R.string.label_trade),
                    r.getString(R.string.label_sustainability),
                    r.getString(R.string.label_partnership)
                ),
                "Music" to listOf(
                    r.getString(R.string.label_operational_ability),
                    r.getString(R.string.label_trade),
                    r.getString(R.string.label_sustainability),
                    r.getString(R.string.label_legal_stability),
                    r.getString(R.string.label_design),
                    r.getString(R.string.label_marketability)
                ),
                "Art" to listOf(
                    r.getString(R.string.label_monetization),
                    r.getString(R.string.label_collaboration),
                    r.getString(R.string.label_sustainability),
                    r.getString(R.string.label_regulation),
                    r.getString(R.string.label_scalability),
                    r.getString(R.string.label_branding)
                ),
                "Game" to listOf(
                    r.getString(R.string.label_collaboration),
                    r.getString(R.string.label_capability),
                    r.getString(R.string.label_content),
                    r.getString(R.string.label_creativity),
                    r.getString(R.string.label_operational_ability),
                    r.getString(R.string.label_scalability)
                ),
                "Comics" to listOf(
                    r.getString(R.string.label_partnership),
                    r.getString(R.string.label_marketability),
                    r.getString(R.string.label_monetization),
                    r.getString(R.string.label_sustainability),
                    r.getString(R.string.label_utilization),
                    r.getString(R.string.label_operational_ability)
                ),
                "Character" to listOf(
                    r.getString(R.string.label_monetization),
                    r.getString(R.string.label_ip_utilization),
                    r.getString(R.string.label_collaboration),
                    r.getString(R.string.label_regulation),
                    r.getString(R.string.label_trade),
                    r.getString(R.string.label_scalability)
                ),
                "Franchise" to listOf(
                    r.getString(R.string.label_capability),
                    r.getString(R.string.label_technology),
                    r.getString(R.string.label_creativity),
                    r.getString(R.string.label_marketability),
                    r.getString(R.string.label_operational_ability),
                    r.getString(R.string.label_ip_utilization)
                ),
                "Trademark" to listOf(
                    r.getString(R.string.label_distribution),
                    r.getString(R.string.label_content),
                    r.getString(R.string.label_awareness),
                    r.getString(R.string.label_trade),
                    r.getString(R.string.label_marketability),
                    r.getString(R.string.label_monetization)
                )
            )
        }


        // --- ✅ newInstance 메소드 수정 (모든 데이터 외부에서 받도록) ---

        fun newInstance(

            grade: String?,

            institutionalValues: List<Float>?,

            stipValues: List<Float>?,

            category: String? // <<< 카테고리 파라미터 추가

        ): RadarChartDialogFragment {

            // val model = dummyRadarData[ticker] ?: ... (제거)


            return RadarChartDialogFragment().apply {

                arguments = Bundle().apply {

                    putString(ARG_GRADE, grade ?: "N/A")

                    // List를 ArrayList로 변환하여 Bundle에 저장

                    putSerializable(ARG_INST_VALUES, ArrayList(institutionalValues ?: emptyList()))

                    putSerializable(ARG_STIP_VALUES, ArrayList(stipValues ?: emptyList()))

                    putString(ARG_CATEGORY, category ?: "Unknown") // <<< 카테고리 저장

                }

            }

        }


        // dummyRadarData, RadarDataModel 제거


        // 타입 캐스팅 및 API 레벨 호환성 처리 함수 (기존 유지)

        @Suppress("DEPRECATION", "UNCHECKED_CAST")

        private fun getFloatArrayListCompat(bundle: Bundle, key: String): ArrayList<Float>? {

            return try {

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {

                    bundle.getParcelableArrayList(key, Float::class.java) as? ArrayList<Float>

                } else {

                    bundle.getSerializable(key) as? ArrayList<Float>

                }

            } catch (e: Exception) {

                Log.e(TAG, "Error retrieving float array list for key $key", e)

                null

            }

        }

    }

}