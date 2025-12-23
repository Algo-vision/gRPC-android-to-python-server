package com.instacart.sample.imagestream

import android.graphics.ImageFormat

data class StreamConfig(
    val imageFormat: Int = ImageFormat.JPEG,
    val fps: Int = 5,
    val maxImages: Int = 3, // Number of images that can be queued in ImageReader
)
