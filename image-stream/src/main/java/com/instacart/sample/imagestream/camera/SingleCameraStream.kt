package com.instacart.sample.imagestream.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import androidx.core.content.ContextCompat.checkSelfPermission
import com.instacart.sample.imagestream.FrameProcessor
import com.instacart.sample.imagestream.StreamConfig
import com.instacart.sample.imagestream.timer.Timer
import com.instacart.sample.logger.Logger
import java.nio.ByteBuffer

internal class SingleCameraStream(
    private val context: Context,
    private val cameraManager: CameraManager,
    private val cameraDetails: CameraDetails,
    private val config: StreamConfig,
) {
    private val tag = "$TAG[${cameraDetails.id}]"

    private var imageReader: ImageReader? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var cameraThread: HandlerThread? = null

    private val timer = Timer(rate = config.fps)

    private fun createStateCallback(handler: Handler) = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Logger.debug(tag, "Camera opened (${cameraDetails.id})")
            cameraDevice = camera
            createCaptureSession(handler)
        }

        override fun onDisconnected(camera: CameraDevice) {
            Logger.warn(tag, "Camera disconnected (${cameraDetails.id})")
            cleanup()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Logger.error(tag, "Camera error (${cameraDetails.id}): $error")
            cleanup()
        }
    }

    fun start(processor: FrameProcessor) {
        if (!hasCameraPermission()) {
            Logger.error(tag, "!!!! CAMERA PERMISSION NOT GRANTED! Cannot start camera stream. !!!!")
            return
        }

        val handler = startCameraThread()
        openCamera(processor, handler)
    }

    fun stop() {
        cleanup()
        stopCameraThread()
    }

    private fun startCameraThread(): Handler {
        val thread = HandlerThread("CameraThread-${cameraDetails.id}").apply {
            start()
        }
        cameraThread = thread
        return Handler(thread.looper)
    }

    private fun stopCameraThread() {
        cameraThread?.quitSafely()
        try {
            cameraThread?.join()
            cameraThread = null
        } catch (e: InterruptedException) {
            Logger.error(tag, "Error stopping camera thread", e)
        }
    }

    private fun openCamera(processor: FrameProcessor, handler: Handler) {
        if (!hasCameraPermission()) {
            Logger.error(tag, "Cannot open camera - permission denied")
            return
        }
        setImageReader(processor, handler)

        try {
            @Suppress("MissingPermission")
            cameraManager.openCamera(cameraDetails.id, createStateCallback(handler), handler)
            Logger.debug(tag, "Opening camera (${cameraDetails})")
        } catch (e: SecurityException) {
            Logger.error(tag, "!!!! SECURITY EXCEPTION: Camera permission denied! !!!!", e)
        } catch (e: Exception) {
            Logger.error(tag, "Error opening camera", e)
        }
    }

    private fun hasCameraPermission() =
        checkSelfPermission(context, Manifest.permission.CAMERA) == PERMISSION_GRANTED

    private fun setImageReader(processor: FrameProcessor, handler: Handler) {
        imageReader = ImageReader.newInstance(
            cameraDetails.width,
            cameraDetails.height,
            config.imageFormat,
            config.maxImages
        ).apply {
            setOnImageAvailableListener({ reader ->
                reader.acquireLatestImage()?.use { image ->
                    processImage(image, processor)
                }
            }, handler)
        }
    }

    @Suppress("DEPRECATION")
    private fun createCaptureSession(handler: Handler) {
        val camera = cameraDevice ?: return
        val reader = imageReader ?: return

        try {
            val surface = reader.surface
            val captureRequestBuilder =
                camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    addTarget(surface)
                }

            camera.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        Logger.debug(tag, "Capture session configured")
                        captureSession = session
                        startRepeatingCapture(captureRequestBuilder, handler)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Logger.error(tag, "Failed to configure capture session")
                    }
                },
                handler
            )
        } catch (e: Exception) {
            Logger.error(tag, "Error creating capture session", e)
        }
    }

    private fun startRepeatingCapture(
        captureRequestBuilder: CaptureRequest.Builder,
        handler: Handler,
    ) {
        try {
            captureSession?.setRepeatingRequest(
                captureRequestBuilder.build(),
                null,
                handler,
            )
            Logger.debug(tag, "Started repeating capture")
        } catch (e: Exception) {
            Logger.error(tag, "Error starting repeating capture", e)
        }
    }

    private fun processImage(image: Image, processor: FrameProcessor) = timer.tick {
        try {
            val buffer = convertImageToByteBuffer(image) ?: return@tick
            val metadata = FrameProcessor.Metadata(
                cameraId = cameraDetails.id,
                timestamp = image.timestamp,
                width = image.width,
                height = image.height,
            )

            processor.process(buffer, metadata)
        } catch (e: Exception) {
            Logger.error(tag, "Error processing image", e)
        }
    }

    private fun convertImageToByteBuffer(image: Image): ByteBuffer? {
        return when (image.format) {
            ImageFormat.JPEG -> image.planes[0].buffer

            ImageFormat.YUV_420_888 -> {
                // For YUV, we need to combine all planes
                // This is a simplified version - in production you might want to convert to JPEG
                val yPlane = image.planes[0]
                val uPlane = image.planes[1]
                val vPlane = image.planes[2]

                val ySize = yPlane.buffer.remaining()
                val uSize = uPlane.buffer.remaining()
                val vSize = vPlane.buffer.remaining()

                val data = ByteArray(ySize + uSize + vSize)

                yPlane.buffer.get(data, 0, ySize)
                uPlane.buffer.get(data, ySize, uSize)
                vPlane.buffer.get(data, ySize + uSize, vSize)

                ByteBuffer.wrap(data)
            }

            else -> {
                Logger.warn(tag, "Unsupported image format: ${image.format}")
                null
            }
        }
    }

    private fun cleanup() {
        captureSession?.close()
        captureSession = null

        cameraDevice?.close()
        cameraDevice = null

        imageReader?.close()
        imageReader = null

        timer.reset()
    }

    companion object {
        private const val TAG = "SingleCameraStream"
    }
}
