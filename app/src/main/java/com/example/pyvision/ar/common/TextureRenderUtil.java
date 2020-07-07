/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.example.pyvision.ar.common;

import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import com.huawei.hiar.ARFrame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * This is a common class for drawing camera texture. With this class,
 * you can display the pictures taken by the camera on the screen.
 *
 * @author hw
 * @since 2020-03-25
 */
public class TextureRenderUtil {
    private static final String TAG = TextureRenderUtil.class.getSimpleName();

    private static final String LINE_SEPARATOR = System.lineSeparator();

    private static final String BASE_FRAGMENT =
        "#extension GL_OES_EGL_image_external : require" + LINE_SEPARATOR
        + "precision mediump float;" + LINE_SEPARATOR
        + "varying vec2 textureCoordinate;" + LINE_SEPARATOR
        + "uniform samplerExternalOES vTexture;" + LINE_SEPARATOR
        + "void main() {" + LINE_SEPARATOR
        + "    gl_FragColor = texture2D(vTexture, textureCoordinate );" + LINE_SEPARATOR
//                "  float fGrayColor = (0.3*gl_FragColor.r + 0.59*gl_FragColor.g + 0.11*gl_FragColor.b);"+ LINE_SEPARATOR+
//                "  gl_FragColor = vec4(fGrayColor,fGrayColor,fGrayColor,1.0);" + LINE_SEPARATOR
        + "}";

    private static final String BASE_VERTEX =
        "attribute vec4 vPosition;" + LINE_SEPARATOR
        + "attribute vec2 vCoord;" + LINE_SEPARATOR
        + "uniform mat4 vMatrix;" + LINE_SEPARATOR
        + "uniform mat4 vCoordMatrix;" + LINE_SEPARATOR
        + "varying vec2 textureCoordinate;" + LINE_SEPARATOR
        + "void main(){" + LINE_SEPARATOR
        + "    gl_Position = vMatrix*vPosition;" + LINE_SEPARATOR
        + "    textureCoordinate = (vCoordMatrix*vec4(vCoord,0,1)).xy;" + LINE_SEPARATOR
        + "}";

    // Vertex coordinates
    private static final float[] POS = {-1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f,};

    // Texture coordinates
    private static final float[] COORD = {0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f,};

    private static final int MATRIX_SIZE = 16;

    private static final float RGB_CLEAR_VALUE = 0.8157f;

    private int mExternalTextureId;

    private int mProgram;

    private int mPosition;

    private int mCoord;

    private int mMatrix;

    private int mTexture;

    private int mCoordMatrix;

    private FloatBuffer mVerBuffer;

    private FloatBuffer mTexTransformedBuffer;

    private FloatBuffer mTexBuffer;

    private float[] mProjectionMatrix = new float[MATRIX_SIZE];

    private float[] coordMatrixs;

    private Bitmap mBitmap;

    /**
     * Texture render util.
     */
    public TextureRenderUtil() {
        coordMatrixs = MatrixUtil.getOriginalMatrix();
        initBuffers();
    }

    public void setBitmap(Bitmap bitmap){
        mBitmap = bitmap;
    }

    /**
     * This method should be called when {@link android.opengl.GLSurfaceView.Renderer#onSurfaceChanged}
     * when the plane size changes.
     *
     * @param width width
     * @param height height
     */
    public void onSurfaceChanged(int width, int height) {
        MatrixUtil.getProjectionMatrix(mProjectionMatrix, width, height);
    }

    /**
     * When OnSurfaceCreated, this method should be called, which initializes the texture ID,
     * generates the external texture, and creates the OpenGLES shader program.
     * This method will be called when {@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated}.
     */
    public void init() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mExternalTextureId = textures[0];
        generateExternalTexture();
        createProgram();
    }

    /**
     * When OnSurfaceCreated, and the texture ID has been created. This method should be called
     * when {@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated}.
     *
     * @param textureId texture id.
     */
    public void init(int textureId) {
        mExternalTextureId = textureId;
        generateExternalTexture();
        createProgram();
    }

    /**
     * Get texture ID.
     *
     * @return texture id.
     */
    public int getExternalTextureId() {
        return mExternalTextureId;
    }

    /**
     * Call this interface every frame.This method should be called
     * when {@link android.opengl.GLSurfaceView.Renderer#onDrawFrame}.
     *
     * @param frame ARFrame
     */
    public void onDrawFrame(ARFrame frame) {
        ShaderUtil.checkGlError(TAG, "before draw");
        if (frame == null) {
            return;
        }
        if (frame.hasDisplayGeometryChanged()) {
            frame.transformDisplayUvCoords(mTexBuffer, mTexTransformedBuffer);
        }
        clear();

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);

        GLES20.glUseProgram(mProgram);

        // Set texture
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mExternalTextureId);

        // Set projection matrix
        GLES20.glUniformMatrix4fv(mMatrix, 1, false, mProjectionMatrix, 0);

        // Set mapping matrix
        GLES20.glUniformMatrix4fv(mCoordMatrix, 1, false, coordMatrixs, 0);

        // Set vertices
        GLES20.glEnableVertexAttribArray(mPosition);
        GLES20.glVertexAttribPointer(mPosition, 2, GLES20.GL_FLOAT, false, 0, mVerBuffer);

        // Set texture coordinates
        GLES20.glEnableVertexAttribArray(mCoord);
        GLES20.glVertexAttribPointer(mCoord, 2, GLES20.GL_FLOAT, false, 0, mTexTransformedBuffer);

        // Number of vertices.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mPosition);
        GLES20.glDisableVertexAttribArray(mCoord);

        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        ShaderUtil.checkGlError(TAG, "after draw");
    }

    private void generateExternalTexture() {
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mExternalTextureId);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
    }

    private void createProgram() {
        mProgram = createGlProgram();
        // 相当于创建各个变量的索引
        mPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mCoord = GLES20.glGetAttribLocation(mProgram, "vCoord");
        mMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        mTexture = GLES20.glGetUniformLocation(mProgram, "vTexture");
        mCoordMatrix = GLES20.glGetUniformLocation(mProgram, "vCoordMatrix");
    }

    private static int createGlProgram() {
        int vertex = loadShader(GLES20.GL_VERTEX_SHADER, BASE_VERTEX);
        if (vertex == 0) {
            return 0;
        }
        int fragment = loadShader(GLES20.GL_FRAGMENT_SHADER, BASE_FRAGMENT);
        if (fragment == 0) {
            return 0;
        }
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertex);
            GLES20.glAttachShader(program, fragment);
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                glError("Could not link program:" + GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    private static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (0 != shader) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                glError("Could not compile shader:" + shaderType);
                glError("GLES20 Error:" + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private static void glError(Object index) {
        Log.e(TAG, "glError:" + "---" + index);
    }

    private void initBuffers() {
        // Initialize vertex buffer size.
        ByteBuffer byteBufferForVer = ByteBuffer.allocateDirect(32);
        byteBufferForVer.order(ByteOrder.nativeOrder());
        mVerBuffer = byteBufferForVer.asFloatBuffer();
        mVerBuffer.put(POS);
        mVerBuffer.position(0);

        // Initialize texture buffer size.
        ByteBuffer byteBufferForTex = ByteBuffer.allocateDirect(32);
        byteBufferForTex.order(ByteOrder.nativeOrder());
        mTexBuffer = byteBufferForTex.asFloatBuffer();
        mTexBuffer.put(COORD);
        mTexBuffer.position(0);

        // Initialize transformed texture buffer size.
        ByteBuffer byteBufferForTransformedTex = ByteBuffer.allocateDirect(32);
        byteBufferForTransformedTex.order(ByteOrder.nativeOrder());
        mTexTransformedBuffer = byteBufferForTransformedTex.asFloatBuffer();
    }

    /**
     * Clear canvas
     */
    private void clear() {
        GLES20.glClearColor(RGB_CLEAR_VALUE, RGB_CLEAR_VALUE, RGB_CLEAR_VALUE, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
    }
}