package com.example.health_assistant.features.discover.presentation

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.example.health_assistant.R
import com.example.health_assistant.databinding.DialogReportContentBinding
import com.example.health_assistant.features.discover.domain.model.ContentReportType
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog for reporting content issues
 */
class ReportContentDialog(
    context: Context,
    private val contentId: String,
    private val contentType: String,
    private val contentTitle: String,
    private val onReportSubmitted: (ContentReportType, String) -> Unit
) : Dialog(context) {
    
    private lateinit var binding: DialogReportContentBinding
    private var selectedReportType: ContentReportType? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = DialogReportContentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupDialog()
        setupReportTypeSpinner()
        setupDescriptionField()
        setupButtons()
    }
    
    private fun setupDialog() {
        // Make dialog non-cancelable when submitting
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        
        // Set dialog title with content info
        // The title is already set in the layout, but we could customize it here if needed
    }
    
    private fun setupReportTypeSpinner() {
        val reportTypes = ContentReportType.values()
        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            reportTypes.map { it.displayName }
        )
        
        binding.reportTypeSpinner.setAdapter(adapter)
        binding.reportTypeSpinner.setOnItemClickListener { _, _, position, _ ->
            selectedReportType = reportTypes[position]
            updateDescriptionRequirement()
            validateForm()
        }
    }
    
    private fun setupDescriptionField() {
        binding.descriptionEditText.doAfterTextChanged {
            validateForm()
        }
    }
    
    private fun updateDescriptionRequirement() {
        val isRequired = selectedReportType == ContentReportType.OTHER
        binding.descriptionLayout.helperText = if (isRequired) {
            "Please describe the issue (required)"
        } else {
            "Optional: Provide additional details about the issue"
        }
    }
    
    private fun setupButtons() {
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
        
        binding.submitButton.setOnClickListener {
            submitReport()
        }
        
        // Initially disable submit button
        binding.submitButton.isEnabled = false
    }
    
    private fun validateForm(): Boolean {
        val hasReportType = selectedReportType != null
        val description = binding.descriptionEditText.text?.toString()?.trim() ?: ""
        val hasRequiredDescription = selectedReportType != ContentReportType.OTHER || description.isNotEmpty()
        
        val isValid = hasReportType && hasRequiredDescription
        binding.submitButton.isEnabled = isValid
        
        return isValid
    }
    
    private fun submitReport() {
        if (!validateForm()) return
        
        val reportType = selectedReportType ?: return
        val description = binding.descriptionEditText.text?.toString()?.trim() ?: ""
        
        // Show loading state
        setLoadingState(true)
        
        // Submit report
        onReportSubmitted(reportType, description)
    }
    
    fun setLoadingState(loading: Boolean) {
        binding.apply {
            progressIndicator.isVisible = loading
            submitButton.isEnabled = !loading
            cancelButton.isEnabled = !loading
            reportTypeSpinner.isEnabled = !loading
            descriptionEditText.isEnabled = !loading
            
            setCancelable(!loading)
            setCanceledOnTouchOutside(!loading)
        }
    }
    
    fun showSuccess() {
        // Show success message and dismiss
        MaterialAlertDialogBuilder(context)
            .setTitle("Report Submitted")
            .setMessage("Thank you for your feedback. We'll review this content and take appropriate action.")
            .setPositiveButton("OK") { _, _ ->
                dismiss()
            }
            .show()
    }
    
    fun showError(message: String) {
        setLoadingState(false)
        
        MaterialAlertDialogBuilder(context)
            .setTitle("Error")
            .setMessage("Failed to submit report: $message")
            .setPositiveButton("OK", null)
            .show()
    }
    
    companion object {
        fun show(
            context: Context,
            contentId: String,
            contentType: String,
            contentTitle: String,
            onReportSubmitted: (ContentReportType, String) -> Unit
        ): ReportContentDialog {
            return ReportContentDialog(
                context,
                contentId,
                contentType,
                contentTitle,
                onReportSubmitted
            ).apply {
                show()
            }
        }
    }
}