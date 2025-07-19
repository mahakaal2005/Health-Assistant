package com.example.health_assistant.features.discover.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil3.load
import com.example.health_assistant.R
import com.example.health_assistant.databinding.DialogContentSharingBinding
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Bottom sheet dialog for content sharing options
 * Provides various sharing methods including social media, email, and clipboard
 */
class ContentSharingBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogContentSharingBinding? = null
    private val binding get() = _binding!!

    private lateinit var content: DiscoverContent
    private lateinit var sharingManager: ContentSharingManager

    companion object {
        private const val ARG_CONTENT = "content"

        fun newInstance(content: DiscoverContent): ContentSharingBottomSheet {
            return ContentSharingBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CONTENT, content)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        content = arguments?.getSerializable(ARG_CONTENT) as DiscoverContent
        sharingManager = ContentSharingManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogContentSharingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Setup UI elements with content information
     */
    private fun setupUI() {
        binding.apply {
            // Set dialog title based on content type
            textDialogTitle.text = when (content) {
                is DiscoverContent.Article -> "Share Article"
                is DiscoverContent.News -> "Share News"
                is DiscoverContent.Video -> "Share Video"
            }

            // Set content preview
            textContentTitle.text = content.title
            textContentSummary.text = content.getContentSummary()

            // Load content image
            content.imageUrl?.let { imageUrl ->
                imageContentPreview.load(imageUrl)
            } ?: run {
                imageContentPreview.setImageResource(
                    when (content) {
                        is DiscoverContent.Article -> R.drawable.ic_article
                        is DiscoverContent.News -> R.drawable.ic_newspaper
                        is DiscoverContent.Video -> R.drawable.ic_play_arrow
                    }
                )
            }
        }
    }

    /**
     * Setup click listeners for all sharing options
     */
    private fun setupClickListeners() {
        binding.apply {
            // Close button
            buttonClose.setOnClickListener {
                dismiss()
            }

            // Quick share options
            buttonShareGeneral.setOnClickListener {
                sharingManager.shareContent(content)
                dismiss()
            }

            buttonCopyLink.setOnClickListener {
                val link = sharingManager.createShareableLink(content)
                sharingManager.copyToClipboard(content, ContentSharingManager.ClipboardCopyType.FULL_CONTENT)
                dismiss()
            }

            buttonShareEmail.setOnClickListener {
                sharingManager.shareViaEmail(content)
                dismiss()
            }

            buttonCopyText.setOnClickListener {
                sharingManager.copyToClipboard(content, ContentSharingManager.ClipboardCopyType.FULL_CONTENT)
                dismiss()
            }

            // Social media options
            buttonShareTwitter.setOnClickListener {
                sharingManager.shareToSocialMedia(content, ContentSharingManager.SocialPlatform.TWITTER)
                dismiss()
            }

            buttonShareFacebook.setOnClickListener {
                sharingManager.shareToSocialMedia(content, ContentSharingManager.SocialPlatform.FACEBOOK)
                dismiss()
            }

            buttonShareLinkedIn.setOnClickListener {
                sharingManager.shareToSocialMedia(content, ContentSharingManager.SocialPlatform.LINKEDIN)
                dismiss()
            }

            buttonShareWhatsApp.setOnClickListener {
                sharingManager.shareToSocialMedia(content, ContentSharingManager.SocialPlatform.WHATSAPP)
                dismiss()
            }

            // Advanced options
            buttonCopyQuote.setOnClickListener {
                sharingManager.copyToClipboard(content, ContentSharingManager.ClipboardCopyType.QUOTE)
                dismiss()
            }

            buttonCopyCitation.setOnClickListener {
                sharingManager.copyToClipboard(content, ContentSharingManager.ClipboardCopyType.CITATION)
                dismiss()
            }

            buttonCopyTitle.setOnClickListener {
                sharingManager.copyToClipboard(content, ContentSharingManager.ClipboardCopyType.TITLE_ONLY)
                dismiss()
            }
        }
    }
}