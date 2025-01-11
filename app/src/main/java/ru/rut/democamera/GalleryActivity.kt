package ru.rut.democamera

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import ru.rut.democamera.databinding.ActivityGalleryBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GalleryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGalleryBinding
    private lateinit var files: List<File>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.galleryToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.galleryToolbar.setNavigationOnClickListener {
            finish()
        }

        val directory = File(externalMediaDirs[0].absolutePath)
        files = directory.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerView.adapter = GalleryAdapter(files, ::onFileClick, ::onFileDelete)
    }

    private fun onFileClick(file: File) {
        val intent = Intent(this, FullscreenActivity::class.java)
        intent.putExtra("file_path", file.absolutePath)
        startActivity(intent)
    }

    private fun onFileDelete(file: File) {
        if (file.delete()) {
            Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show()
            files = files.filter { it != file }
            setupRecyclerView()
        } else {
            Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show()
        }
    }
}
