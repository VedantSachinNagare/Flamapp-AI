package com.example.flappai.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.math.abs

class CameraController(
    private val context: Context,
    lifecycleOwner: LifecycleOwner,
    private val textureView: TextureView,
    private val onFrameAvailable: (FrameData) -> Unit
) : DefaultLifecycleObserver {

    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var imageReader: ImageReader? = null
    private var previewSize: Size? = null

    private val backgroundThread = HandlerThread("CameraBackground").apply { start() }
    private val backgroundHandler = Handler(backgroundThread.looper)

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            startPreviewInternal()
        }

        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) = Unit

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
    }

    init {
        textureView.surfaceTextureListener = textureListener
        lifecycleOwner.lifecycle.addObserver(this)
    }

    fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    fun startPreview() {
        if (textureView.isAvailable) {
            startPreviewInternal()
        } else {
            textureView.surfaceTextureListener = textureListener
        }
    }

    fun resume() {
        if (textureView.isAvailable) {
            startPreviewInternal()
        }
    }

    fun pause() {
        closeCamera()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        stopBackgroundThread()
    }

    private fun startPreviewInternal() {
        if (!hasCameraPermission()) return
        if (cameraDevice != null) return
        try {
            val cameraId = selectCameraId()
            if (cameraId != null) {
                cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler)
            }
        } catch (ex: CameraAccessException) {
            Log.e(TAG, "Camera access error", ex)
        }
    }

    private fun selectCameraId(): String? {
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing =
                    characteristics.get(CameraCharacteristics.LENS_FACING) ?: continue
                if (facing != CameraCharacteristics.LENS_FACING_BACK) continue

                val map =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        ?: continue
                previewSize = chooseOptimalSize(map)
                return cameraId
            }
        } catch (ex: CameraAccessException) {
            Log.e(TAG, "Unable to enumerate cameras", ex)
        }
        return null
    }

    private fun chooseOptimalSize(map: StreamConfigurationMap): Size {
        val preferred = Size(1280, 720)
        val available =
            map.getOutputSizes(ImageFormat.YUV_420_888) ?: return preferred

        var best = available.first()
        var minDiff = Int.MAX_VALUE
        for (option in available) {
            val diff = abs(option.width * option.height - preferred.width * preferred.height)
            if (diff < minDiff) {
                best = option
                minDiff = diff
            }
        }
        return best
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.e(TAG, "Camera error: $error")
            camera.close()
            cameraDevice = null
        }
    }

    private fun createCameraPreviewSession() {
        val texture = textureView.surfaceTexture ?: return
        val size = previewSize ?: return

        texture.setDefaultBufferSize(size.width, size.height)
        val previewSurface = Surface(texture)

        imageReader?.close()
        imageReader = ImageReader.newInstance(
            size.width,
            size.height,
            ImageFormat.YUV_420_888,
            2
        ).apply {
            setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
        }

        val camera = cameraDevice ?: return
        try {
            previewRequestBuilder =
                camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    addTarget(previewSurface)
                    addTarget(imageReader!!.surface)
                }

            camera.createCaptureSession(
                listOf(previewSurface, imageReader!!.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return
                        captureSession = session
                        previewRequestBuilder?.let { builder ->
                            builder.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Failed to configure camera session")
                    }
                },
                backgroundHandler
            )
        } catch (ex: CameraAccessException) {
            Log.e(TAG, "Failed to start preview session", ex)
        }
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
        val frame = convertToFrameData(image)
        image.close()
        frame?.let(onFrameAvailable)
    }

    private fun convertToFrameData(image: Image): FrameData? {
        if (image.format != ImageFormat.YUV_420_888) return null

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val ySize = yPlane.buffer.remaining()
        val uSize = uPlane.buffer.remaining()
        val vSize = vPlane.buffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yPlane.buffer.rewind()
        yPlane.buffer.get(nv21, 0, ySize)

        val chromaRowStride = uPlane.rowStride
        val chromaPixelStride = uPlane.pixelStride
        val width = image.width
        val height = image.height
        var offset = ySize

        val uBuffer = uPlane.buffer.duplicate()
        val vBuffer = vPlane.buffer.duplicate()
        uBuffer.rewind()
        vBuffer.rewind()
        for (row in 0 until height / 2) {
            var col = 0
            while (col < width) {
                val uIndex = row * chromaRowStride + col * chromaPixelStride
                val vIndex = row * chromaRowStride + col * chromaPixelStride
                nv21[offset++] = vBuffer.get(vIndex)
                nv21[offset++] = uBuffer.get(uIndex)
                col += 2
            }
        }

        return FrameData(
            nv21 = nv21,
            width = width,
            height = height,
            timestampNs = image.timestamp
        )
    }

    private fun closeCamera() {
        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
        } catch (ex: Exception) {
            Log.e(TAG, "Error closing camera", ex)
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread.quitSafely()
        try {
            backgroundThread.join()
        } catch (ex: InterruptedException) {
            Log.e(TAG, "Interrupted while stopping background thread", ex)
        }
    }

    companion object {
        private const val TAG = "CameraController"
    }
}

