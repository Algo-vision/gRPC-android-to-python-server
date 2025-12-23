package com.instacart.sample.imagestream

interface ImageStream {
    fun startStream(onFrame: FrameProcessor)
    fun stopStream()
}
