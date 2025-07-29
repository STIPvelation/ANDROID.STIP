package com.stip.stip.more.fragment.ipentertainment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.stip.stip.R
import com.stip.stip.databinding.FragmentMoreIpSwapRegistrationBinding
import com.stip.stip.more.dialog.SectorFilterDialog
import com.google.android.material.card.MaterialCardView

class IPSwapRegistrationFragment : Fragment() {

    private var _binding: FragmentMoreIpSwapRegistrationBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null
    private var selectedSector: String? = null
    
    // List of available sectors
    private val sectors = listOf(
        "IT/소프트웨어", "바이오테크", "제약", "식품", "의료기기", 
        "반도체", "전자기기", "자동차", "화학", "환경", "에너지", 
        "금융", "교육", "엔터테인먼트", "게임", "인공지능", "로봇"
    )

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                showSelectedImage(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreIpSwapRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        // Initialize sector selector with default text
        binding.tvSelectedSector.hint = "기술 섹터를 선택하세요"
    }

    private fun setupListeners() {
        // Image upload
        binding.cardImageUpload.setOnClickListener {
            openGallery()
        }
        
        // Sector selection
        binding.cardSectorSelector.setOnClickListener {
            showSectorFilterDialog()
        }

        // Register button click
        binding.btnRegister.setOnClickListener {
            validateAndSubmit()
        }
    }
    
    private fun showSectorFilterDialog() {
        val dialog = SectorFilterDialog(
            requireContext(),
            sectors,
            selectedSector
        ) { sector ->
            selectedSector = sector
            binding.tvSelectedSector.text = sector
            binding.tvSelectedSector.setTextColor(resources.getColor(R.color.black, null))
        }
        dialog.show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getContent.launch(intent)
    }

    private fun showSelectedImage(uri: Uri) {
        // Replace the placeholder with the actual image
        // This would need a custom implementation to show the image in the CardView
        // For now, we'll just show a toast
        Toast.makeText(requireContext(), "이미지가 선택되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun validateAndSubmit() {
        // Check if required fields are filled
        val ipNumber = binding.etIpNumber.text.toString().trim()
        val contact = binding.etContact.text.toString().trim()
        val lawyerName = binding.etLawyerName.text.toString().trim()
        val lawyerCompany = binding.etLawyerCompany.text.toString().trim()
        val lawyerContact = binding.etLawyerContact.text.toString().trim()

        // Check if sector is selected
        if (selectedSector == null) {
            Toast.makeText(requireContext(), "섹터를 선택해주세요.", Toast.LENGTH_SHORT).show()
            // Highlight the sector selector to draw attention
            (binding.cardSectorSelector as MaterialCardView).strokeColor = resources.getColor(R.color.error_red, null)
            return
        }

        if (ipNumber.isEmpty() || contact.isEmpty() || lawyerName.isEmpty() 
            || lawyerCompany.isEmpty() || lawyerContact.isEmpty()) {
            Toast.makeText(requireContext(), "필수 입력 항목을 모두 작성해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // If all validation passes, submit the form
        // In a real app, this would send the data to a server
        Toast.makeText(requireContext(), "IP스왑이 등록되었습니다.", Toast.LENGTH_SHORT).show()
        
        // Go back to the previous screen or to a success screen
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
