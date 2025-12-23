package com.instacart.sample.grpc

import ai.caper.cv.cart.domain.SideCameraImageServiceGrpcKt.SideCameraImageServiceCoroutineStub
import ai.caper.cv.cart.domain.SideCameraImageServiceOuterClass.ObservePredictionsResponse
import ai.caper.cv.cart.domain.SideCameraImageServiceOuterClass.SubmitCameraFrameRequest
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import com.instacart.sample.logger.Logger
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * gRPC client for submitting camera images to the SideCameraImageService.
 *
 * This client handles the connection to the gRPC server and provides streaming
 * APIs for submitting frames and observing predictions.
 */
class SideCameraGrpcClient(
    val config: GrpcConfig,
    private val coroutineScope: CoroutineScope,
) : AutoCloseable {

    private var channel: ManagedChannel? = null
    private val frameFlow = MutableSharedFlow<SubmitCameraFrameRequest>(
        replay = 0,
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private var streamJob: Job? = null
    private var predictionJob: Job? = null

    /**
     * Initialize the gRPC channel, stub, and start the frame streaming.
     */
    fun connect() {
        if (channel != null) {
            Logger.debug(TAG, "Already connected, skipping")
            return
        }

        Logger.info(TAG, "Connecting to ${config.host}:${config.port}...")
        with(SideCameraImageServiceCoroutineStub(channel = buildChannel())) {
            startFrameStream()
            startPredictionObserver()
        }
        Logger.info(TAG, "Connected successfully, frame stream and prediction observer started")
    }

    private fun buildChannel(): ManagedChannel {
        val channelBuilder = ManagedChannelBuilder.forAddress(config.host, config.port)
        if (!config.useTls) channelBuilder.usePlaintext()
        return channelBuilder.build().also { channel = it }
    }

    private fun SideCameraImageServiceCoroutineStub.startFrameStream() {
        streamJob = autoReconnect(context = "Frame stream") {
            submitSideCameraImage(frameFlow)
        }
    }

    private fun SideCameraImageServiceCoroutineStub.startPredictionObserver() {
        predictionJob = autoReconnect(context = "Prediction observer") {
            observePredictions(Empty.getDefaultInstance())
                .catch { e -> Logger.error(TAG, "Prediction stream error: ${e.status}", e) }
                .collect { it.logPayload() }
        }
    }

    private fun ObservePredictionsResponse.logPayload() = when (payloadCase) {
        ObservePredictionsResponse.PayloadCase.JSON_RAW -> {
            Logger.info(TAG, "Prediction: JSON (size=${jsonRaw.length})")
            Logger.debug(TAG, jsonRaw)
        }

        ObservePredictionsResponse.PayloadCase.FILE_RAW ->
            Logger.info(TAG, "Prediction: File (size=${fileRaw.size()})")

        ObservePredictionsResponse.PayloadCase.PAYLOAD_NOT_SET, null ->
            Logger.info(TAG, "Prediction received with no payload")
    }

    private fun autoReconnect(context: String, block: suspend () -> Unit) = coroutineScope.launch {
        var timeout = 1.seconds
        while (true) {
            try {
                Logger.debug(TAG, "Starting ${context.lowercase()}...")
                block()
                Logger.info(TAG, "$context completed normally")
                break
            } catch (e: Exception) {
                Logger.error(TAG, "$context error: ${e.status}, retrying in 1s...", e)
                delay(timeout)
                timeout = maxOf(timeout * 2, 1.minutes)
            }
        }
    }

    private val Throwable.status: Any? get() = (this as? StatusException)?.status ?: message

    /**
     * Submit a single frame to the stream.
     * The frame is emitted to the hot flow and will be consumed by the server stream.
     *
     * @param frameBuffer ByteBuffer containing raw image bytes (e.g., JPEG or PNG encoded)
     * @param cameraId Optional camera identifier
     * @param timestamp Optional timestamp in milliseconds
     * @return Result indicating success or failure of emitting to the flow
     */
    fun submitFrame(
        frameBuffer: ByteBuffer,
        cameraId: String? = null,
        timestamp: Long? = null,
    ): Result<Unit> = try {
        frameFlow.tryEmit(buildFrameRequest(frameBuffer, cameraId, timestamp))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(GrpcException("Failed to submit frame: ${e.message}", e))
    }

    private fun buildFrameRequest(
        frameBuffer: ByteBuffer,
        cameraId: String?,
        timestamp: Long?,
    ) = SubmitCameraFrameRequest.newBuilder().setImageData(ByteString.copyFrom(frameBuffer)).apply {
        cameraId?.let(this::setCameraId)
        timestamp?.let(this::setTimestamp)
    }.build()

    /**
     * Shutdown the gRPC channel and cleanup resources.
     */
    override fun close() {
        if (channel == null) {
            Logger.debug(TAG, "Already disconnected, skipping")
            return
        }

        Logger.info(TAG, "Disconnecting...")
        streamJob?.cancel()
        streamJob = null
        predictionJob?.cancel()
        predictionJob = null

        try {
            channel?.shutdown()?.awaitTermination(5, TimeUnit.SECONDS)
            Logger.info(TAG, "Disconnected successfully")
        } catch (_: InterruptedException) {
            Logger.warn(TAG, "Disconnect interrupted, forcing shutdown")
            channel?.shutdownNow()
        }
        channel = null
    }

    class GrpcException(message: String, cause: Throwable? = null) : Exception(message, cause)
}

private const val TAG = "SideCameraGrpcClient"
