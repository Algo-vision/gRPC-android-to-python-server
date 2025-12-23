package com.instacart.sample.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import com.instacart.sample.grpc.SideCameraGrpcClient
import com.instacart.sample.imagestream.FrameProcessor
import com.instacart.sample.imagestream.ImageStream
import com.instacart.sample.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

class GrpcServiceViewModel(
    context: Context,
    private val client: SideCameraGrpcClient,
    private val imageStream: ImageStream,
) : ViewModel() {

    private val appContext = context.applicationContext

    private val _state =
        MutableStateFlow(ServiceState(config = "${client.config.host}:${client.config.port}"))
    val state: StateFlow<ServiceState> = _state.asStateFlow()

    private val testAssets: List<String> = try {
        context.assets.list("")?.filter { it.endsWith(".jpg") || it.endsWith(".jpeg") }
            ?: emptyList()
    } catch (e: Exception) {
        Logger.error(TAG, "Failed to list assets", e)
        emptyList()
    }.also { assets ->
        Logger.info(TAG, "${assets.size} test assets available")
    }

    fun connect() {
        try {
            client.connect()
            updateState(isConnected = true, serviceStatus = "Connected")
        } catch (e: Exception) {
            Logger.error(TAG, "Connection failed: ${e.message}", e)
            updateState(isConnected = false, serviceStatus = "Connection Failed")
        }
    }

    private fun updateState(
        isConnected: Boolean? = null,
        isCameraStarted: Boolean? = null,
        serviceStatus: String? = null,
        cameraStatus: String? = null,
    ) {
        val currentState = _state.value
        _state.value = currentState.copy(
            isConnected = isConnected ?: currentState.isConnected,
            isCameraStarted = isCameraStarted ?: currentState.isCameraStarted,
            serviceStatus = serviceStatus ?: currentState.serviceStatus,
            cameraStatus = cameraStatus ?: currentState.cameraStatus,
        )
    }

    fun disconnect() {
        try {
            client.close()
            updateState(isConnected = false, serviceStatus = "Disconnected")
        } catch (e: Exception) {
            Logger.error(TAG, "Disconnect error: ${e.message}", e)
        }
    }

    private val frameProcessor = FrameProcessor { frame, metadata ->
        try {
            val result = client.submitFrame(
                frameBuffer = frame,
                cameraId = metadata.cameraId,
                timestamp = metadata.timestamp,
            )

            result.onFailure { error ->
                Logger.error(
                    TAG,
                    "Failed to submit frame from ${metadata.cameraId}: ${error.message}",
                    error,
                )
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Error submitting frame: ${e.message}", e)
        }
    }

    fun startCamera() {
        try {
            imageStream.startStream(frameProcessor)
            updateState(isCameraStarted = true, cameraStatus = "Streaming")
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to start camera: ${e.message}", e)
            updateState(isCameraStarted = false, cameraStatus = "Failed to start")
        }
    }

    fun stopCamera() {
        try {
            imageStream.stopStream()
            updateState(isCameraStarted = false, cameraStatus = "Stopped")
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to stop camera: ${e.message}", e)
        }
    }

    fun sendTestFrame() {
        try {
            val testImageBuffer = createTestImage()
            if (testImageBuffer == null) {
                Logger.warn(TAG, "No test image available, skipping")
                return
            }

            val result = client.submitFrame(
                frameBuffer = testImageBuffer,
                cameraId = "test-camera",
                timestamp = System.currentTimeMillis(),
            )
            result.fold(
                onSuccess = { Logger.debug(TAG, "Frame sent successfully") },
                onFailure = { Logger.error(TAG, "Frame send failed: ${it.message}", it) },
            )
        } catch (e: Exception) {
            Logger.error(TAG, "Error sending frame: ${e.message}", e)
        }
    }

    private fun createTestImage(): ByteBuffer? {
        if (testAssets.isEmpty()) {
            Logger.warn(TAG, "No test assets available")
            return null
        }

        val selectedAsset = testAssets.random()
        Logger.debug(TAG, "Loading test image: $selectedAsset")

        return try {
            appContext.assets.open(selectedAsset).use { inputStream ->
                val bytes = inputStream.readBytes()
                ByteBuffer.wrap(bytes)
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to load asset: $selectedAsset", e)
            null
        }
    }

    override fun onCleared() {
        imageStream.stopStream()
        client.close()
    }

    data class ServiceState(
        val isConnected: Boolean = false,
        val isCameraStarted: Boolean = false,
        val serviceStatus: String = "Disconnected",
        val cameraStatus: String = "Disconnected",
        val config: String = "Not configured",
    )

    companion object {
        private const val TAG = "GrpcServiceViewModel"
    }
}
