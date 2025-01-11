package ru.rut.democamera

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import ru.rut.democamera.databinding.FragmentFullscreenBinding
import java.io.File

class FullscreenFragment : Fragment() {
    private var _binding: FragmentFullscreenBinding? = null
    private val binding get() = _binding!!
    private var player: ExoPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFullscreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val filePath = arguments?.getString("file_path") ?: return
        val file = File(filePath)

        if (file.extension.equals("mp4", ignoreCase = true)) {
            binding.fullscreenPlayerView.visibility = View.VISIBLE
            player = ExoPlayer.Builder(requireContext()).build()
            binding.fullscreenPlayerView.player = player

            val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()
        } else {
            binding.fullscreenImageView.visibility = View.VISIBLE
            Glide.with(this)
                .load(file)
                .into(binding.fullscreenImageView)
        }

        binding.closeButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
        _binding = null
    }
}
