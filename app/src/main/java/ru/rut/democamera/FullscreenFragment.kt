package ru.rut.democamera

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import ru.rut.democamera.databinding.FragmentFullscreenBinding
import java.io.File

class FullscreenFragment : Fragment() {
    private var _binding: FragmentFullscreenBinding? = null
    private val binding get() = _binding!!
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
        val filePath = arguments?.getString("file_path") ?: run {
            Toast.makeText(requireContext(), "File path is missing", Toast.LENGTH_SHORT).show()
            return
        }
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(requireContext(), "File does not exist", Toast.LENGTH_SHORT).show()
            binding.fullscreenVideoView.visibility = View.GONE
            binding.fullscreenImageView.visibility = View.GONE
            return
        }
        if (file.extension.equals("mp4", ignoreCase = true)) {
            setupVideoPlayer(file)
        } else {
            setupImageView(file)
        }
        binding.closeButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
    private fun setupVideoPlayer(file: File) {
        binding.fullscreenVideoView.visibility = View.VISIBLE
        val mediaController = MediaController(requireContext())
        binding.fullscreenVideoView.setMediaController(mediaController)
        val videoUri = Uri.fromFile(file)
        binding.fullscreenVideoView.setVideoURI(videoUri)
        binding.fullscreenVideoView.setOnPreparedListener { mediaPlayer ->
            mediaController.setAnchorView(binding.fullscreenVideoView)
            mediaPlayer.start()
            Log.d("FullscreenFragment", "Video started: $videoUri")
        }
        binding.fullscreenVideoView.setOnErrorListener { _, what, extra ->
            Log.e("FullscreenFragment", "Error playing video: what=$what, extra=$extra")
            Toast.makeText(requireContext(), "Unable to play video", Toast.LENGTH_SHORT).show()
            binding.fullscreenVideoView.visibility = View.GONE
            true
        }
    }
    private fun setupImageView(file: File) {
        binding.fullscreenImageView.visibility = View.VISIBLE
        Glide.with(this)
            .load(file)
            .error(R.drawable.ic_close)
            .into(binding.fullscreenImageView)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        binding.fullscreenVideoView.stopPlayback()
        _binding = null
    }
}
