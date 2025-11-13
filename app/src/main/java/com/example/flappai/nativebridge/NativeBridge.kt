package com.example.flappai.nativebridge

class NativeBridge {

    init {
        System.loadLibrary(LIB_NAME)
    }

    fun initialize(width: Int, height: Int) {
        nativeInit(width, height)
    }

    fun processFrame(nv21Buffer: ByteArray, width: Int, height: Int): ByteArray =
        nativeProcessFrame(nv21Buffer, width, height)

    fun getLastProcessingFps(): Float = nativeGetLastProcessingFps()

    fun release() {
        nativeRelease()
    }

    private external fun nativeInit(width: Int, height: Int)
    private external fun nativeProcessFrame(
        nv21Buffer: ByteArray,
        width: Int,
        height: Int
    ): ByteArray

    private external fun nativeGetLastProcessingFps(): Float
    private external fun nativeRelease()

    companion object {
        private const val LIB_NAME = "flappnative"
    }
}

