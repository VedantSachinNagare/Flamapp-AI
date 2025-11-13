package com.example.flappai.gl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.annotation.RawRes
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ProcessedFrameRenderer(
    private val context: Context,
    private val onFpsUpdate: (Float) -> Unit = {}
) : GLSurfaceView.Renderer {

    private var programHandle = 0
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var textureHandle = 0
    private var mvpMatrixHandle = 0
    private var texMatrixHandle = 0

    private val vertexBuffer: FloatBuffer = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    ).toFloatBuffer()

    private val texCoordBuffer: FloatBuffer = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    ).toFloatBuffer()

    private val textureIds = IntArray(1)
    private var frameBuffer: ByteBuffer? = null
    private var frameWidth = 0
    private var frameHeight = 0

    private val projectionMatrix = FloatArray(16)
    private val textureMatrix = FloatArray(16)

    private val frameTimestamps = ArrayDeque<Long>()
    private val fpsWindowMs = 1_000L

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        setupProgram()
        setupTexture()

        Matrix.setIdentityM(projectionMatrix, 0)
        Matrix.setIdentityM(textureMatrix, 0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val buffer = frameBuffer ?: return

        GLES20.glUseProgram(programHandle)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            vertexBuffer
        )

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(
            texCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            texCoordBuffer
        )

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])

        buffer.position(0)
        if (frameWidth > 0 && frameHeight > 0) {
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_RGBA,
                frameWidth,
                frameHeight,
                0,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                buffer
            )
        }

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, projectionMatrix, 0)
        GLES20.glUniformMatrix4fv(texMatrixHandle, 1, false, textureMatrix, 0)
        GLES20.glUniform1i(textureHandle, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)

        updateFps()
    }

    fun updateFrame(rgbaPixels: ByteArray, width: Int, height: Int) {
        if (frameBuffer == null || frameBuffer!!.capacity() != rgbaPixels.size) {
            frameBuffer = ByteBuffer.allocateDirect(rgbaPixels.size).apply {
                order(ByteOrder.nativeOrder())
            }
        }
        frameWidth = width
        frameHeight = height
        frameBuffer?.apply {
            position(0)
            put(rgbaPixels)
            position(0)
        }
    }

    fun setFrameSize(width: Int, height: Int) {
        Matrix.setIdentityM(projectionMatrix, 0)
        Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -1f, 1f, -1f, 1f)
        Matrix.setIdentityM(textureMatrix, 0)
    }

    fun onSurfaceDestroyed() {
        if (textureIds[0] != 0) {
            GLES20.glDeleteTextures(1, textureIds, 0)
            textureIds[0] = 0
        }
    }

    private fun setupProgram() {
        val vertexSource = readRawTextFile(R.raw.vertex_shader)
        val fragmentSource = readRawTextFile(R.raw.fragment_shader)

        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)

        programHandle = GLES20.glCreateProgram().also { program ->
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)

            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val error = GLES20.glGetProgramInfoLog(program)
                GLES20.glDeleteProgram(program)
                throw RuntimeException("Error linking program: $error")
            }
        }

        positionHandle = GLES20.glGetAttribLocation(programHandle, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(programHandle, "aTexCoord")
        textureHandle = GLES20.glGetUniformLocation(programHandle, "uTexture")
        mvpMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix")
        texMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uTexMatrix")
    }

    private fun setupTexture() {
        GLES20.glGenTextures(1, textureIds, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Could not compile shader of type $type: $error")
        }
        return shader
    }

    private fun readRawTextFile(@RawRes resId: Int): String =
        context.resources.openRawResource(resId).bufferedReader().use { it.readText() }

    private fun FloatArray.toFloatBuffer(): FloatBuffer =
        ByteBuffer.allocateDirect(size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(this@toFloatBuffer)
                position(0)
            }
        }

    private fun updateFps() {
        val now = System.currentTimeMillis()
        frameTimestamps.addLast(now)
        while (frameTimestamps.isNotEmpty() && now - frameTimestamps.first() > fpsWindowMs) {
            frameTimestamps.removeFirst()
        }

        val fps = if (frameTimestamps.size > 1) {
            (frameTimestamps.size - 1) * 1000f /
                (frameTimestamps.last() - frameTimestamps.first()).coerceAtLeast(1L)
        } else {
            0f
        }
        onFpsUpdate(fps)
    }
}

