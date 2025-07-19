package com.example.health_assistant.features.discover.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.health_assistant.databinding.FragmentVideoPlayerBinding
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.Util
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment for playing educational videos with ExoPlayer
 * Supports playback controls, progress tracking, and resume functionality
 */
@AndroidEntryPoint
class VideoPlayerFragment : Fragment() {

    private var _binding: FragmentVideoPlayerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VideoPlayerViewModel by viewModels()
    private val args: VideoPlayerFragmentArgs by navArgs()

    private var exoPlayer: ExoPlayer? = null
    private var playWhenReady = true
    private var currentPosition = 0L
    private var playbackPosition = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeViewModel()
        
        // Load video data
        viewModel.loadVideo(args.videoId)
    }

    private fun setupUI() {
        binding.apply {
            // Back button
            backButton.setOnClickListener {
                findNavController().navigateUp()
            }

            // Bookmark button
            bookmarkButton.setOnClickListener {
                viewModel.toggleBookmark()
            }

            // Share button
            shareButton.setOnClickListener {
                viewModel.currentVideo.value?.let { video ->
                    shareVideo(video)
                }
            }

            // Quality selector button
            qualityButton.setOnClickListener {
                showQualitySelector()
            }

            // Download button
            downloadButton.setOnClickListener {
                viewModel.toggleOfflineDownload()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.currentVideo.observe(viewLifecycleOwner) { video ->
            video?.let {
                setupVideoInfo(it)
                initializePlayer(it)
            }
        }

        viewModel.playbackPosition.observe(viewLifecycleOwner) { position ->
            playbackPosition = position
        }

        viewModel.isBookmarked.observe(viewLifecycleOwner) { isBookmarked ->
            updateBookmarkButton(isBookmarked)
        }

        viewModel.downloadState.observe(viewLifecycleOwner) { state ->
            updateDownloadButton(state)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupVideoInfo(video: DiscoverContent.Video) {
        binding.apply {
            videoTitle.text = video.title
            videoDescription.text = video.description
            expertName.text = "By ${video.expertName}"
            expertCredentials.text = video.expertCredentials
            videoDuration.text = formatDuration(video.durationSeconds)
            difficultyLevel.text = video.difficultyLevel.replaceFirstChar { it.uppercase() }
            categoryChip.text = video.category
            
            // Set progress if video was previously watched
            if (video.watchProgress > 0f) {
                watchProgressBar.progress = (video.watchProgress * 100).toInt()
                watchProgressText.text = "${(video.watchProgress * 100).toInt()}% watched"
                watchProgressContainer.visibility = View.VISIBLE
            } else {
                watchProgressContainer.visibility = View.GONE
            }
        }
    }

    private fun initializePlayer(video: DiscoverContent.Video) {
        exoPlayer = ExoPlayer.Builder(requireContext()).build().also { player ->
            binding.playerView.player = player

            // Create media item
            val mediaItem = MediaItem.fromUri(video.videoUrl)
            
            // Use HLS source if URL contains .m3u8
            val mediaSource = if (video.videoUrl.contains(".m3u8")) {
                val dataSourceFactory = DefaultDataSource.Factory(requireContext())
                HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            } else {
                null
            }

            if (mediaSource != null) {
                player.setMediaSource(mediaSource)
            } else {
                player.setMediaItem(mediaItem)
            }

            // Set playback position if resuming
            if (playbackPosition > 0) {
                player.seekTo(playbackPosition)
            } else if (video.watchProgress > 0f) {
                val resumePosition = (video.durationSeconds * video.watchProgress * 1000).toLong()
                player.seekTo(resumePosition)
            }

            player.playWhenReady = playWhenReady
            player.prepare()

            // Add listener for playback events
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            binding.progressBar.visibility = View.GONE
                        }
                        Player.STATE_BUFFERING -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        Player.STATE_ENDED -> {
                            // Mark video as completed
                            viewModel.updateWatchProgress(1.0f)
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Toast.makeText(
                        requireContext(),
                        "Playback error: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }
    }

    private fun updateBookmarkButton(isBookmarked: Boolean) {
        binding.bookmarkButton.isSelected = isBookmarked
        binding.bookmarkButton.contentDescription = if (isBookmarked) {
            "Remove from bookmarks"
        } else {
            "Add to bookmarks"
        }
    }

    private fun updateDownloadButton(state: VideoPlayerViewModel.DownloadState) {
        binding.apply {
            when (state) {
                VideoPlayerViewModel.DownloadState.NOT_DOWNLOADED -> {
                    downloadButton.text = "Download"
                    downloadButton.isEnabled = true
                    downloadProgressBar.visibility = View.GONE
                }
                VideoPlayerViewModel.DownloadState.DOWNLOADING -> {
                    downloadButton.text = "Downloading..."
                    downloadButton.isEnabled = false
                    downloadProgressBar.visibility = View.VISIBLE
                }
                VideoPlayerViewModel.DownloadState.DOWNLOADED -> {
                    downloadButton.text = "Downloaded"
                    downloadButton.isEnabled = true
                    downloadProgressBar.visibility = View.GONE
                }
                VideoPlayerViewModel.DownloadState.FAILED -> {
                    downloadButton.text = "Download Failed"
                    downloadButton.isEnabled = true
                    downloadProgressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun shareVideo(video: DiscoverContent.Video) {
        val sharingBottomSheet = ContentSharingBottomSheet.newInstance(video)
        sharingBottomSheet.show(childFragmentManager, "ContentSharingBottomSheet")
    }

    private fun showQualitySelector() {
        // For now, show a simple toast. In a real implementation,
        // this would show a dialog with available quality options
        Toast.makeText(
            requireContext(),
            "Quality selection will be available based on network conditions",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%d:%02d", minutes, secs)
        }
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            viewModel.currentVideo.value?.let { initializePlayer(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23 || exoPlayer == null) {
            viewModel.currentVideo.value?.let { initializePlayer(it) }
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
        
        // Save current playback position
        exoPlayer?.let { player ->
            val progress = if (player.duration > 0) {
                player.currentPosition.toFloat() / player.duration.toFloat()
            } else 0f
            
            if (progress > 0f) {
                viewModel.updateWatchProgress(progress)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        exoPlayer?.let { player ->
            playbackPosition = player.currentPosition
            playWhenReady = player.playWhenReady
            player.release()
        }
        exoPlayer = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releasePlayer()
        _binding = null
    }
}