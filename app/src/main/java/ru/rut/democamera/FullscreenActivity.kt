package ru.rut.democamera

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import ru.rut.democamera.databinding.ActivityFullscreenBinding
import java.io.File

class FullscreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFullscreenBinding
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val filePath = intent.getStringExtra("file_path") ?: return
        val file = File(filePath)

        if (file.extension.equals("mp4", ignoreCase = true)) {
            binding.fullscreenPlayerView.visibility = View.VISIBLE
            player = ExoPlayer.Builder(this).build()
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
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
