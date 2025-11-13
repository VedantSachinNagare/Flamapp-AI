package com.example.flappai.camera

data class FrameData(
    val nv21: ByteArray,
    val width: Int,
    val height: Int,
    val timestampNs: Long
)

