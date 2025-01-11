package ru.rut.democamera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.rut.democamera.databinding.ItemGalleryBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GalleryAdapter(
    private val files: List<File>,
    private val onFileClick: (File) -> Unit,
    private val onFileDelete: (File) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

    class GalleryViewHolder(private val binding: ItemGalleryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: File, onFileClick: (File) -> Unit, onFileDelete: (File) -> Unit) {
            val isVideo = file.extension.equals("mp4", ignoreCase = true)
            binding.fileType.text = if (isVideo) "Video" else "Photo"

            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            binding.fileDate.text = sdf.format(Date(file.lastModified()))

            Glide.with(binding.root.context)
                .load(file)
                .centerCrop()
                .into(binding.filePreview)

            binding.root.setOnClickListener { onFileClick(file) }
            binding.deleteButton.setOnClickListener { onFileDelete(file) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val binding = ItemGalleryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GalleryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        holder.bind(files[position], onFileClick, onFileDelete)
    }

    override fun getItemCount(): Int = files.size
}
