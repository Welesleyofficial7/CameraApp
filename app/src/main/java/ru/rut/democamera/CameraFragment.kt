package ru.rut.democamera

import ModeAdapter
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.*
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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import ru.rut.democamera.databinding.ActivityMainBinding
import ru.rut.democamera.databinding.FragmentCameraBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private var imageCapture: ImageCapture? = null
    private lateinit var imageCaptureExecutor: ExecutorService
    private var videoCapture: androidx.camera.video.VideoCapture<Recorder>? = null
    private var isRecording = false
    private val RECORD_AUDIO_REQUEST_CODE = 1001
    private var isPhotoMode = true
    private lateinit var recording: Recording
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
    private var elapsedSeconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            binding.videoTimer.text = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60)
            elapsedSeconds++
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
//        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//        imageCaptureExecutor = Executors.newSingleThreadExecutor()
//
//        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
//            == PackageManager.PERMISSION_GRANTED
//        ) {
//            startCamera()
//        } else {
//            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1001)
//        }
//
//        binding.galleryBtn.setOnClickListener {
//            findNavController().navigate(R.id.action_CameraFragment_to_GalleryFragment)
//        }
//    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            try {
                cameraProvider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.preview.surfaceProvider)
                }

                imageCapture = ImageCapture.Builder()
                    .setTargetRotation(binding.preview.display.rotation)
                    .build()

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build()

                videoCapture = VideoCapture.withOutput(recorder)

                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    videoCapture
                )
            } catch (e: Exception) {
                Log.e("CameraFragment", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }



    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1001)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        imageCaptureExecutor = Executors.newSingleThreadExecutor()

        requestCameraPermission()

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
            findNavController().navigate(R.id.action_CameraFragment_to_GalleryFragment)
        }
    }

    private fun takePhoto() {
        imageCapture?.let {
            animateFlash()

            val fileName = "JPEG_${System.currentTimeMillis()}.jpg"
            val externalDir = requireContext().externalMediaDirs.firstOrNull()
            val file = File(externalDir, fileName)

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
                            requireContext(),
                            "Error taking photo: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.d("TAG", "Error taking photo: $exception")
                    }
                }
            )
        } ?: Toast.makeText(requireContext(), "Camera is not ready", Toast.LENGTH_SHORT).show()
    }


    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
            return
        }

        videoCapture?.let { videoCapture ->
            val fileName = "VID_${System.currentTimeMillis()}.mp4"
            val externalDir = requireContext().externalMediaDirs.firstOrNull()
            val file = File(externalDir, fileName)
            val outputOptions = FileOutputOptions.Builder(file).build()

            recording = videoCapture.output
                .prepareRecording(requireContext(), outputOptions)
                .apply {
                    withAudioEnabled()
                }
                .start(ContextCompat.getMainExecutor(requireContext())) { event: VideoRecordEvent ->
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
                Toast.makeText(this.requireContext(), "Audio recording permission is required for video recording.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startTimer() {
        elapsedSeconds = 0
        binding.videoTimer.text = "00:00"
        handler.post(runnable)
    }

    private fun stopTimer() {
        handler.removeCallbacks(runnable)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        stopTimer()
        imageCaptureExecutor.shutdown()
    }
}
