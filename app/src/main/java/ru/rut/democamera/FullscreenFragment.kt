package ru.rut.democamera

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
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

        val filePath = arguments?.getString("file_path") ?: return
        val file = File(filePath)

        if (!file.exists()) {
            binding.fullscreenVideoView.visibility = View.GONE
            binding.fullscreenImageView.visibility = View.GONE
            return
        }

        if (file.extension.equals("mp4", ignoreCase = true)) {
            binding.fullscreenVideoView.visibility = View.VISIBLE
            val mediaController = MediaController(requireContext())
            binding.fullscreenVideoView.setMediaController(mediaController)

            val videoUri = Uri.fromFile(file)
            binding.fullscreenVideoView.setVideoURI(videoUri)
            binding.fullscreenVideoView.setOnPreparedListener { mediaPlayer ->
                mediaController.setAnchorView(binding.fullscreenVideoView)
                mediaPlayer.start()
            }
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
        _binding = null
    }
}
