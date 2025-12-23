package com.instacart.sample.imagestream.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
import android.hardware.camera2.CameraManager
import com.instacart.sample.imagestream.FrameProcessor
import com.instacart.sample.imagestream.ImageStream
import com.instacart.sample.imagestream.StreamConfig
import com.instacart.sample.logger.Logger

/**
 * ImageStream implementation that captures from all available cameras simultaneously
 * using Android Camera2 API.
 *
 * This class:
 * - Discovers all available cameras on initialization
 * - Extracts the highest resolution for each camera
 * - Manages individual camera streams via SingleCameraStream components
 * - Provides captured frames to a FrameProcessor callback
 */
class CameraImageStream(
    private val context: Context,
    private val cameraManager: CameraManager,
    private val config: StreamConfig,
) : ImageStream {

    private val availableCameras: List<CameraDetails>
    private val cameraStreams = mutableListOf<SingleCameraStream>()
    private var isStreaming = false

    init {
        availableCameras = discoverCameras()
        Logger.info(TAG, "Discovered ${availableCameras.size} cameras: ${availableCameras.map { it.id }}")
    }

    override fun startStream(onFrame: FrameProcessor) {
        if (isStreaming) {
            Logger.warn(TAG, "Stream already started")
            return
        }

        Logger.info(TAG, "Starting streams for ${availableCameras.size} cameras")
        isStreaming = true

        availableCameras.forEach { cameraDetails ->
            val stream = SingleCameraStream(
                context = context,
                cameraManager = cameraManager,
                cameraDetails = cameraDetails,
                config = config
            )
            stream.start(onFrame)
            cameraStreams.add(stream)
        }

        Logger.info(TAG, "All camera streams started")
    }

    override fun stopStream() {
        if (!isStreaming) {
            Logger.warn(TAG, "Stream not started")
            return
        }

        Logger.info(TAG, "Stopping all camera streams")
        isStreaming = false

        cameraStreams.forEach(SingleCameraStream::stop)
        cameraStreams.clear()

        Logger.info(TAG, "All camera streams stopped")
    }

    private fun discoverCameras() = try {
        cameraManager.cameraIdList.mapNotNull(::getCameraDetails)
    } catch (e: Exception) {
        Logger.error(TAG, "Error discovering cameras", e)
        emptyList()
    }

    private fun getCameraDetails(cameraId: String): CameraDetails? {
        return try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val configMap = characteristics.get(SCALER_STREAM_CONFIGURATION_MAP)

            if (configMap == null) {
                Logger.warn(TAG, "Camera $cameraId has no stream configuration map")
                return null
            }

            // Get output sizes for the configured format
            val outputSizes = configMap.getOutputSizes(config.imageFormat)
            if (outputSizes.isNullOrEmpty()) {
                Logger.warn(
                    TAG,
                    "Camera $cameraId has no output sizes for format ${config.imageFormat}"
                )
                return null
            }

            // Find the highest resolution (largest area)
            val maxResolution = outputSizes
                .filter { it.width * 3 == it.height * 4 }
                .maxByOrNull { it.width * it.height }
                ?: outputSizes[0]

            Logger.debug(
                TAG,
                "Camera $cameraId max resolution: ${maxResolution.width}x${maxResolution.height}"
            )

            CameraDetails(
                id = cameraId,
                width = maxResolution.width,
                height = maxResolution.height
            )
        } catch (e: Exception) {
            Logger.error(TAG, "Error getting characteristics for camera $cameraId", e)
            null
        }
    }

    companion object {
        private const val TAG = "CameraImageStream"
    }
}
