package ru.rut.democamera

import ModeAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import ru.rut.democamera.databinding.ActivityMainBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private var imageCapture: ImageCapture? = null
    private var videoCapture: androidx.camera.video.VideoCapture<Recorder>? = null
    private var isRecording = false
    private val RECORD_AUDIO_REQUEST_CODE = 1001
    private var isPhotoMode = true
    private lateinit var recording: Recording
    private lateinit var imageCaptureExecutor: ExecutorService
    private val cameraPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (permissionGranted) {
                startCamera()
            } else {
                Snackbar.make(
                    binding.root,
                    "The camera permission is necessary",
                    Snackbar.LENGTH_INDEFINITE
                ).show()
            }
        }


    private fun startCamera() {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.preview.surfaceProvider)
        }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        imageCapture = ImageCapture.Builder()
            .setTargetRotation(binding.preview.display.rotation)
            .build()

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    videoCapture
                )
            } catch (e: Exception) {
                Log.e("TAG", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        imageCaptureExecutor = Executors.newSingleThreadExecutor()

        cameraPermissionResult.launch(android.Manifest.permission.CAMERA)

        binding.modeViewPager.adapter = ModeAdapter(
            onPhotoClick = { takePhoto() },
            onVideoClick = {
                if (isRecording) stopRecording() else startRecording()
            }
        )

        binding.switchBtn.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }

        binding.galleryBtn.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        imageCaptureExecutor.shutdown()
    }

    private fun takePhoto() {
        imageCapture?.let {
            animateFlash()
            val fileName = "JPEG_${System.currentTimeMillis()}.jpg"
            val file = File(externalMediaDirs[0], fileName)
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
            it.takePicture(
                outputFileOptions,
                imageCaptureExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Log.i("TAG", "The image has been saved in ${file.toUri()}")
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(
                            binding.root.context,
                            "Error taking photo",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.d("TAG", "Error taking photo:$exception")
                    }

                })
        }
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
            return
        }

        videoCapture?.let { videoCapture ->
            val fileName = "VID_${System.currentTimeMillis()}.mp4"
            val file = File(externalMediaDirs[0], fileName)
            val outputOptions = FileOutputOptions.Builder(file).build()

            recording = videoCapture.output
                .prepareRecording(this, outputOptions)
                .apply {
                    withAudioEnabled()
                }
                .start(ContextCompat.getMainExecutor(this)) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            isRecording = true
                            binding.videoTimer.visibility = View.VISIBLE
                            updateVideoRecordButtonIcon(true)
                            updateVideoRecordButtonText(true)
                            updateVideoRecordButtonUI(true, Color.RED)
                            startTimer()
                        }
                        is VideoRecordEvent.Finalize -> {
                            isRecording = false
                            binding.videoTimer.visibility = View.GONE
                            updateVideoRecordButtonIcon(false)
                            updateVideoRecordButtonText(false)
                            updateVideoRecordButtonUI(false, Color.BLUE)
                            stopTimer()
                            if (event.hasError()) {
                                Log.e("TAG", "Video recording error: ${event.error}")
                            } else {
                                Log.i("TAG", "Video saved to ${event.outputResults.outputUri}")
                            }
                        }
                    }
                }
        }
    }

    private fun updateVideoRecordButtonText(isRecording: Boolean) {
        val currentView = binding.modeViewPager.getChildAt(0) as? RecyclerView
        currentView?.findViewHolderForAdapterPosition(1)?.itemView?.findViewById<ImageView>(R.id.video_record_btn)?.apply {
            if (isRecording) {
                this.setImageResource(R.drawable.ic_pause)
            } else {
                this.setImageResource(R.drawable.ic_video)
            }
        }
    }

    private fun updateVideoRecordButtonUI(isRecording: Boolean, color: Int) {
        val currentView = binding.modeViewPager.getChildAt(0) as? RecyclerView
        currentView?.findViewHolderForAdapterPosition(1)?.itemView?.findViewById<ImageView>(R.id.video_record_btn)?.apply {
            if (isRecording) {
                this.setImageResource(R.drawable.ic_pause)
            } else {
                this.setImageResource(R.drawable.ic_video)
            }
        }
    }

    private fun updateVideoRecordButtonIcon(isRecording: Boolean) {
        val currentView = binding.modeViewPager.getChildAt(0) as? RecyclerView
        currentView?.findViewHolderForAdapterPosition(1)?.itemView?.findViewById<ImageView>(R.id.video_record_btn)?.apply {
            if (isRecording) {
                this.setImageResource(R.drawable.ic_pause)
            } else {
                this.setImageResource(R.drawable.ic_video)
            }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                Toast.makeText(this, "Audio recording permission is required for video recording.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var timer: CountDownTimer? = null

    private fun startTimer() {
        val startTime = System.currentTimeMillis()
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
                binding.videoTimer.text = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60)
            }

            override fun onFinish() {}
        }.start()
    }

    private fun stopTimer() {
        timer?.cancel()
    }


    private fun stopRecording() {
        if (isRecording) {
            recording.stop()
            isRecording = false
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun animateFlash() {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }
}