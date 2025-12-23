package com.instacart.sample.imagestream

import java.nio.ByteBuffer

fun interface FrameProcessor {
    fun process(frame: ByteBuffer, metadata: Metadata)

    data class Metadata(
        val cameraId: String,
        val timestamp: Long,
        val width: Int,
        val height: Int,
    )
}
