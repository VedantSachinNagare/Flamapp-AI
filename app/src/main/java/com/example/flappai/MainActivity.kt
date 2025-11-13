package com.example.flappai

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.flappai.camera.CameraController
import com.example.flappai.camera.FrameData
import com.example.flappai.databinding.ActivityMainBinding
import com.example.flappai.nativebridge.NativeBridge
import com.example.flappai.ui.ProcessedFrameSurfaceView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraController: CameraController
    private val nativeBridge = NativeBridge()

    private var isProcessingInitialized = false

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCameraPreview()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.camera_permission_needed),
                    Toast.LENGTH_LONG
                ).show()
                binding.permissionHint.isVisible = true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGlView(binding.glView)
        setupCameraController()
        requestCameraPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        binding.glView.onResume()
        cameraController.resume()
    }

    override fun onPause() {
        cameraController.pause()
        binding.glView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        nativeBridge.release()
    }

    private fun setupGlView(view: ProcessedFrameSurfaceView) {
        view.onFpsMeasured = { fps ->
            binding.fpsLabel.text = getString(R.string.fps_template, fps)
        }
    }

    private fun setupCameraController() {
        cameraController = CameraController(
            context = this,
            lifecycleOwner = this,
            textureView = binding.cameraPreview,
            onFrameAvailable = ::onFrameAvailable
        )
    }

    private fun requestCameraPermissionIfNeeded() {
        if (!cameraController.hasCameraPermission()) {
            binding.permissionHint.isVisible = true
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startCameraPreview()
        }
    }

    private fun startCameraPreview() {
        binding.permissionHint.isVisible = false
        cameraController.startPreview()
    }

    private fun onFrameAvailable(frame: FrameData) {
        if (!isProcessingInitialized) {
            nativeBridge.initialize(frame.width, frame.height)
            binding.glView.setTargetResolution(frame.width, frame.height)
            isProcessingInitialized = true
        }

        val rgbaFrame = nativeBridge.processFrame(frame.nv21, frame.width, frame.height)
        val fps = nativeBridge.getLastProcessingFps()

        binding.glView.updateFrame(rgbaFrame, frame.width, frame.height)
        updateStats(fps, frame.width, frame.height)
    }

    private fun updateStats(fps: Float, width: Int, height: Int) {
        binding.fpsLabel.text = getString(R.string.fps_template, fps)
        binding.resolutionLabel.text = getString(R.string.resolution_template, width, height)
    }
}

