package com.example.flappai.ui

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.opengl.GLSurfaceView
import com.example.flappai.gl.ProcessedFrameRenderer

class ProcessedFrameSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val rendererImpl: ProcessedFrameRenderer

    var onFpsMeasured: ((Float) -> Unit)? = null

    init {
        setEGLContextClientVersion(2)
        rendererImpl = ProcessedFrameRenderer(context.applicationContext) { fps ->
            onFpsMeasured?.invoke(fps)
        }
        setRenderer(rendererImpl)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        super.surfaceDestroyed(holder)
        rendererImpl.onSurfaceDestroyed()
    }

    fun updateFrame(rgbaPixels: ByteArray, width: Int, height: Int) {
        queueEvent {
            rendererImpl.updateFrame(rgbaPixels, width, height)
        }
    }

    fun setTargetResolution(width: Int, height: Int) {
        queueEvent {
            rendererImpl.setFrameSize(width, height)
        }
    }
}

