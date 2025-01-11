package ru.rut.democamera

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import ru.rut.democamera.databinding.FragmentGalleryBinding
import java.io.File

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var files: List<File>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val directory = File(requireContext().externalMediaDirs[0].absolutePath)
        files = directory.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()

        setupRecyclerView()

        binding.galleryToolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerView.adapter = GalleryAdapter(files, ::onFileClick, ::onFileDelete)
    }

    private fun onFileClick(file: File) {
        val action = GalleryFragmentDirections.actionGalleryFragmentToFullscreenFragment(file.absolutePath)
        findNavController().navigate(action)
    }


    private fun onFileDelete(file: File) {
        if (file.delete()) {
            Toast.makeText(requireContext(), "File deleted", Toast.LENGTH_SHORT).show()
            files = files.filter { it != file }
            setupRecyclerView()
        } else {
            Toast.makeText(requireContext(), "Failed to delete file", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
