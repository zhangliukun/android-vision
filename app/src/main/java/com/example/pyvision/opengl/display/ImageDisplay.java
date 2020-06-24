package com.example.pyvision.opengl.display;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.example.pyvision.opengl.OpenGLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static com.example.pyvision.opengl.CameraDrawer.getImageProgram;

/**
 * Created by zale.zhang on 2020/6/16.
 *
 * @author zale.zhang
 */
public class ImageDisplay {

    private static final String TAG = ImageDisplay.class.getSimpleName();
    private static final String LS = System.lineSeparator();

    private final int[] textures = new int[1];

    private FloatBuffer mGLCubeBuffer;
    private FloatBuffer mGLTextureBuffer;

    private int mProgram;
    private int mGLAttribPosition;
    private int mGLUniformTexture;
    private int mGLAttribTextureCoordinate;

    private float mOutputWidth, mOutputHeight; // 窗口大小
    private int mImageWidth, mImageHeight; // bitmap图片实际大小

    // 绘制图片的原理：定义一组矩形区域的顶点，然后根据纹理坐标把图片作为纹理贴在该矩形区域内。

    // 原始的矩形区域的顶点坐标，因为后面使用了顶点法绘制顶点，所以不用定义绘制顶点的索引。无论窗口的大小为多少，在OpenGL二维坐标系中都是为下面表示的矩形区域
//    private static final float[] CUBE = { // 窗口中心为OpenGL二维坐标系的原点（0,0）
//            -0.3f, -0.3f, // v1
//            0.3f, -0.3f,  // v2
//            -0.3f, 0.3f,  // v3
//            0.3f, 0.3f,   // v4
//    };
    private static final float[] CUBE = { // 窗口中心为OpenGL二维坐标系的原点（0,0）
            -1f, -1f, // v1
            1f, -1f,  // v2
            -1f, 1f,  // v3
            1f, 1f,   // v4
    };
    // 纹理也有坐标系，称UV坐标，或者ST坐标。UV坐标定义为左上角（0，0），右下角（1，1），一张图片无论大小为多少，在UV坐标系中都是图片左上角为（0，0），右下角（1，1）
    // 纹理坐标，每个坐标的纹理采样对应上面顶点坐标。
//    private static final float[] TEXTURE_NO_ROTATION = {
//            0.0f, 1.0f, // v1
//            1.0f, 1.0f, // v2
//            0.0f, 0.0f, // v3
//            1.0f, 0.0f, // v4
//    };

    private static final float[] TEXTURE_NO_ROTATION = {
            1.0f, 1.0f, // v2
            1.0f, 0.0f, // v1
            0.0f, 1.0f, // v4
            0.0f, 0.0f, // v1
    };

    // 数据中有多少个顶点，管线就调用多少次顶点着色器
    public static final String NO_FILTER_VERTEX_SHADER = "" +
            "attribute vec4 position;\n" + // 顶点着色器的顶点坐标,由外部程序传入
            "attribute vec4 inputTextureCoordinate;\n" + // 传入的纹理坐标
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" + // 最终顶点位置
            "}";

    // 光栅化后产生了多少个片段，就会插值计算出多少个varying变量，同时渲染管线就会调用多少次片段着色器
    public static final String NO_FILTER_FRAGMENT_SHADER = "" +
            "varying highp vec2 textureCoordinate;\n" + // 最终顶点位置，上面顶点着色器的varying变量会传递到这里
            " \n" +
            "uniform sampler2D inputImageTexture;\n" + // 外部传入的图片纹理 即代表整张图片的数据
            " \n" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +  // 调用函数 进行纹理贴图
            "}";


    public void init(Bitmap bitmap){

        mImageWidth = bitmap.getWidth();
        mImageHeight = bitmap.getHeight();

        createProgram();
        GLES20.glGenTextures(1,textures,0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textures[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        OpenGLUtils.checkGlError(TAG, "Program parameters");

        // 顶点数组缓冲器
        mGLCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(CUBE).position(0);

        // 纹理数组缓冲器
        mGLTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(TEXTURE_NO_ROTATION).position(0);


    }

    private void createProgram() {
        mProgram = getImageProgram();
        OpenGLUtils.checkGlError(TAG, "program");
        mGLAttribPosition = GLES20.glGetAttribLocation(mProgram, "position"); // 顶点着色器的顶点坐标
        mGLUniformTexture = GLES20.glGetUniformLocation(mProgram, "inputImageTexture"); // 传入的图片纹理
        mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate"); // 顶点着色器的纹理坐标
        OpenGLUtils.checkGlError(TAG, "program params");
    }

    public void onDrawFrame(Bitmap bitmap) {
        GLES20.glUseProgram(mProgram);
        // 顶点着色器的顶点坐标
        mGLCubeBuffer.position(0); // Sets this buffer's position.
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mGLCubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        // 顶点着色器的纹理坐标
        mGLTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glUniform1i(mGLUniformTexture, 0); //glform1i设置uniform采样器的位置值，或者说纹理单元。 通过glUniform1i的设置，我们保证每个uniform采样器对应着正确的纹理单元。这里是纹理0则设置0

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        // 绘制顶点 ，方式有顶点法和索引法
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4); // 顶点法，按照传入渲染管线的顶点顺序及采用的绘制方式将顶点组成图元进行绘制
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

    }


    // 调整图片显示大小为居中显示
    public void adjustImageScaling(float outputWidth,float outputHeight) {
        mOutputWidth = outputWidth;
        mOutputHeight = outputHeight;

        float ratio1 = outputWidth / mImageWidth;
        float ratio2 = outputHeight / mImageHeight;
        float ratioMax = Math.min(ratio1, ratio2);
        // 居中后图片显示的大小
        int imageWidthNew = Math.round(mImageWidth * ratioMax);
        int imageHeightNew = Math.round(mImageHeight * ratioMax);

        // 图片被拉伸的比例
        float ratioWidth = outputWidth / imageWidthNew;
        float ratioHeight = outputHeight / imageHeightNew;
        // 根据拉伸比例还原顶点
        float[] cube = new float[]{
                CUBE[0] / ratioWidth, CUBE[1] / ratioHeight,
                CUBE[2] / ratioWidth, CUBE[3] / ratioHeight,
                CUBE[4] / ratioWidth, CUBE[5] / ratioHeight,
                CUBE[6] / ratioWidth, CUBE[7] / ratioHeight,
        };

        mGLCubeBuffer.clear();
        mGLCubeBuffer.put(cube).position(0);
    }
}
