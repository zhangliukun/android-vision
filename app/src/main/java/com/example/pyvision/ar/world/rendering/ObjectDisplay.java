/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.example.pyvision.ar.world.rendering;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;

import com.example.pyvision.ar.common.MatrixUtil;
import com.example.pyvision.ar.common.ShaderUtil;
import com.example.pyvision.ar.world.VirtualObject;
import com.example.pyvision.common.opencv.ColorTransferUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;

/**
 * This class demonstrates that if the hit ability of arengein is used, the
 * position information after colliding with the plane can be obtained through
 * the hit ability.
 *
 * @author HW
 * @since 2020-04-11
 */
public class ObjectDisplay {
    private static final String TAG = ObjectDisplay.class.getSimpleName();

    // Set light direction (x, y, z, w).
    private static final float[] LIGHT_DIRECTIONS = new float[]{0.0f, 1.0f, 0.0f, 0.0f};

    private static final int FLOAT_BYTE_SIZE = 4;

    private static final int INDEX_COUNT_RATIO = 2;

    private static final int MATRIX_SIZE = 16;

    // light direction (x, y, z, w).
    private float[] mViewLightDirections = new float[4];

    private int mVertexBufferId;

    private int mTexCoordsBaseAddress;

    private int mNormalsBaseAddress;

    private int mIndexBufferId;

    private int mIndexCount;

    private int mProgram;

    private int[] mTextures = new int[1];

    private int mModelViewUniform;

    private int mModelViewProjectionUniform;

    private int mPositionAttribute;

    private int mNormalAttribute;

    private int mTexCoordAttribute;

    private int mTextureUniform;

    private int mLightingParametersUniform;

    // Shader location: object color property (to change the primary color of the object).
    private int mColorUniform;

    private float[] mModelMatrixs = new float[MATRIX_SIZE];

    private float[] mModelViewMatrixs = new float[MATRIX_SIZE];

    private float[] mModelViewProjectionMatrixs = new float[MATRIX_SIZE];

    // Bounding box size is 6 [minX, minY, minZ, maxX, maxY, maxZ].
    private float[] mBoundingBoxs = new float[6];

    private float mWidth;

    private float mHeight;

    private Bitmap resultBitmap;
    private Bitmap textureBitmap;
    private boolean changeTex = false;
    private boolean textureBinded = true;

    /**
     * Set the size of the screen display area.
     *
     * @param width Width of display.
     * @param height Height of display.
     */
    void setSize(float width, float height) {
        mWidth = width;
        mHeight = height;
    }

    /**
     * Create shader program, read virtual object data, and pass in object data in OpenGLES
     * This method will be called when {@link RenderUtil#onSurfaceCreated}.
     *
     * @param context Context.
     */
    void init(Context context) {
        createProgram();

        // Get two buffer IDS, coordinate and index.
        int[] buffers = new int[2];
        GLES20.glGenBuffers(2, buffers, 0);
        mVertexBufferId = buffers[0];
        mIndexBufferId = buffers[1];
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(mTextures.length, mTextures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        initGlTextureData(context);
        initializeGlObjectData(context);
    }

    private void createProgram() {
        mProgram = WorldShaderUtil.getObjectProgram();
        ShaderUtil.checkGlError(TAG, "program creation");
        mModelViewUniform = GLES20.glGetUniformLocation(mProgram, "inViewMatrix");
        mModelViewProjectionUniform = GLES20.glGetUniformLocation(mProgram, "inMVPMatrix");
        mPositionAttribute = GLES20.glGetAttribLocation(mProgram, "inObjectPosition");
        mNormalAttribute = GLES20.glGetAttribLocation(mProgram, "inObjectNormalVector");
        mTexCoordAttribute = GLES20.glGetAttribLocation(mProgram, "inTexCoordinate");
        mTextureUniform = GLES20.glGetUniformLocation(mProgram, "inObjectTexture");
        mLightingParametersUniform = GLES20.glGetUniformLocation(mProgram, "inLight");
        mColorUniform = GLES20.glGetUniformLocation(mProgram, "inObjectColor");
        ShaderUtil.checkGlError(TAG, "Program parameters");
        Matrix.setIdentityM(mModelMatrixs, 0);
    }

    private void initGlTextureData(Context context) {
        try (InputStream inputStream = context.getAssets().open("chair1/wenli.jpg")) {
            textureBitmap = BitmapFactory.decodeStream(inputStream);
            resultBitmap = Bitmap.createBitmap(textureBitmap.getWidth(), textureBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        } catch (IllegalArgumentException | IOException e) {
            Log.e(TAG, "Get data failed!");
            return;
        }

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
//        textureBitmap.recycle();
        ShaderUtil.checkGlError(TAG, "load texture");
    }

    private void initializeGlObjectData(Context context) {
        ObjectData objectData = readObject(context);
        if (objectData == null) {
            return;
        }
        mTexCoordsBaseAddress = FLOAT_BYTE_SIZE * objectData.objectIndices.limit();
        mNormalsBaseAddress = mTexCoordsBaseAddress + FLOAT_BYTE_SIZE * objectData.texCoords.limit();
        final int totalBytes = mNormalsBaseAddress + FLOAT_BYTE_SIZE * objectData.normals.limit();
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, totalBytes, null, GLES20.GL_STATIC_DRAW);
        GLES20.glBufferSubData(
            GLES20.GL_ARRAY_BUFFER, 0, FLOAT_BYTE_SIZE * objectData.objectVertices.limit(), objectData.objectVertices);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, mTexCoordsBaseAddress,
            FLOAT_BYTE_SIZE * objectData.texCoords.limit(), objectData.texCoords);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, mNormalsBaseAddress,
            FLOAT_BYTE_SIZE * objectData.normals.limit(), objectData.normals);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferId);
        mIndexCount = objectData.indices.limit();
        GLES20.glBufferData(
            GLES20.GL_ELEMENT_ARRAY_BUFFER, INDEX_COUNT_RATIO * mIndexCount, objectData.indices, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        ShaderUtil.checkGlError(TAG, "obj buffer load");
    }

    public void updateTextureImg(Bitmap colorBitmap){
        ColorTransferUtil.transfer_color_same_size(colorBitmap,textureBitmap,resultBitmap);
        textureBinded = false;
    }

    public void showTransferedColor(boolean show){
        this.changeTex = show;
        textureBinded = false;
    }

    private ObjectData readObject(Context context) {
        Obj obj;
//        try (InputStream objInputStream = context.getAssets().open("AR_logo.obj")) {
//        try (InputStream objInputStream = context.getAssets().open("hourse/19446_horse_in_profile_v1_NEW.obj")) {
//        try (InputStream objInputStream = context.getAssets().open("BMW850/BMW850.obj")) {
        try (InputStream objInputStream = context.getAssets().open("chair1/chair1.obj")) {
//        try (InputStream objInputStream = context.getAssets().open("car/camaro.obj")) {
            obj = ObjReader.read(objInputStream);
            obj = ObjUtils.convertToRenderable(obj);
        } catch (IllegalArgumentException | IOException e) {
            Log.e(TAG, "Get data failed!");
            return null;
        }

        // Every surface of an object has three vertices.
        IntBuffer objectIndices = ObjData.getFaceVertexIndices(obj, 3);
        FloatBuffer objectVertices = ObjData.getVertices(obj);

        calculateBoundingBox(objectVertices);

        // Prevent memory shortage and double it.
        ShortBuffer indices = ByteBuffer.allocateDirect(2 * objectIndices.limit())
            .order(ByteOrder.nativeOrder()).asShortBuffer();
        while (objectIndices.hasRemaining()) {
            indices.put((short) objectIndices.get());
        }
        indices.rewind();

        // The dimension of texture coordinate is 2
        FloatBuffer texCoordinates = ObjData.getTexCoords(obj, 2);
        FloatBuffer normals = ObjData.getNormals(obj);
        return new ObjectData(objectIndices, objectVertices, indices, texCoordinates, normals);
    }

    /**
     * Object data.
     *
     * @author HW
     * @since 2020-04-11
     */
    private static class ObjectData {
        IntBuffer objectIndices;
        FloatBuffer objectVertices;
        ShortBuffer indices;
        FloatBuffer texCoords;
        FloatBuffer normals;

        ObjectData(IntBuffer objectIndices,
                   FloatBuffer objectVertices,
                   ShortBuffer indices,
                   FloatBuffer texCoords,
                   FloatBuffer normals) {
            this.objectIndices = objectIndices;
            this.objectVertices = objectVertices;
            this.indices = indices;
            this.texCoords = texCoords;
            this.normals = normals;
        }
    }

    /**
     * Draw a virtual object at the specified position on the specified surface.
     *
     * @param cameraView A 4x4 view matrix, in column-major order.
     * @param cameraProjection A 4x4 projection matrix, in column-major order.
     * @param lightIntensity Light intensity.
     * @param obj The colour of an object.
     */
    public void onDrawFrame(float[] cameraView, float[] cameraProjection, float lightIntensity, VirtualObject obj) {
        ShaderUtil.checkGlError(TAG, "before draw");
        mModelMatrixs = obj.getModelAnchorMatrix();
        Matrix.multiplyMM(mModelViewMatrixs, 0, cameraView, 0, mModelMatrixs, 0);
        Matrix.multiplyMM(mModelViewProjectionMatrixs, 0, cameraProjection, 0, mModelViewMatrixs, 0);
        GLES20.glUseProgram(mProgram);
        Matrix.multiplyMV(mViewLightDirections, 0, mModelViewMatrixs, 0, LIGHT_DIRECTIONS, 0);
        MatrixUtil.normalizeVec3(mViewLightDirections);

        // Lighting direction data has three dimensions(0,1,2).
        GLES20.glUniform4f(mLightingParametersUniform,
            mViewLightDirections[0], mViewLightDirections[1], mViewLightDirections[2], lightIntensity);
//        float[] objColors = obj.getColor();

        // Set the object color property.
//        GLES20.glUniform4fv(mColorUniform, 1, objColors, 0);
        //需要在opengl的Render线程中更新纹理才能生效
        if(!textureBinded){
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
            if(changeTex){
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, resultBitmap, 0);
            }else {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0);
            }
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
            textureBinded = true;
        }
        // 这里面还是需要每一帧都激活绑定的，不然会产生第一个对象绘制时跳帧
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
//        GLES20.glUniform1i(mTextureUniform, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId);

        // The coordinate dimension of the read virtual 3D object is 3
        GLES20.glVertexAttribPointer(
            mPositionAttribute, 3, GLES20.GL_FLOAT, false, 0, 0);

        // The dimension of normal vector is 3.
        GLES20.glVertexAttribPointer(
            mNormalAttribute, 3, GLES20.GL_FLOAT, false, 0, mNormalsBaseAddress);

        // The dimension of texture coordinate is 2.
        GLES20.glVertexAttribPointer(
            mTexCoordAttribute, 2, GLES20.GL_FLOAT, false, 0, mTexCoordsBaseAddress);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glUniformMatrix4fv(
            mModelViewUniform, 1, false, mModelViewMatrixs, 0);
        GLES20.glUniformMatrix4fv(
            mModelViewProjectionUniform, 1, false, mModelViewProjectionMatrixs, 0);
        GLES20.glEnableVertexAttribArray(mPositionAttribute);
        GLES20.glEnableVertexAttribArray(mNormalAttribute);
        GLES20.glEnableVertexAttribArray(mTexCoordAttribute);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferId);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mIndexCount, GLES20.GL_UNSIGNED_SHORT, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLES20.glDisableVertexAttribArray(mPositionAttribute);
        GLES20.glDisableVertexAttribArray(mNormalAttribute);
        GLES20.glDisableVertexAttribArray(mTexCoordAttribute);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        ShaderUtil.checkGlError(TAG, "after draw");
    }

    /**
     * Determine whether an object has been hit.
     *
     * @param cameraView Camera view data.
     * @param cameraPerspective Camera perspective data.
     * @param obj Virtual object.
     * @param event Motion event.
     * @return Returns the result of a click to determine whether or not it is clicked.
     */
    boolean hitTest(float[] cameraView, float[] cameraPerspective, VirtualObject obj, MotionEvent event) {
        mModelMatrixs = obj.getModelAnchorMatrix();
        Matrix.multiplyMM(mModelViewMatrixs, 0, cameraView, 0, mModelMatrixs, 0);
        Matrix.multiplyMM(mModelViewProjectionMatrixs, 0, cameraPerspective, 0, mModelViewMatrixs, 0);

        // Calculate screen position by boundingbox's [minX, minY, minZ].
        float[] screenPos = calculateScreenPos(mBoundingBoxs[0], mBoundingBoxs[1], mBoundingBoxs[2]);

        // Record the largest rectangle containing the object(minX/minY/maxX/maxY)
        float[] boundarys = new float[4];
        boundarys[0] = screenPos[0];
        boundarys[1] = screenPos[0];
        boundarys[2] = screenPos[1];
        boundarys[3] = screenPos[1];

        // Calculate screen position by boundingbox's [maxX, maxY, maxZ].
        boundarys = findMaximum(boundarys, new int[]{3, 4, 5});

        // hit test motion pos.
        if (((event.getX() > boundarys[0]) && (event.getX() < boundarys[1]))
                && ((event.getY() > boundarys[2]) && (event.getY() < boundarys[3]))) {
            return true;
        }

        // Calculate screen position by boundingbox's [minX, minY, maxZ].
        boundarys = findMaximum(boundarys, new int[]{0, 1, 5});

        // Hittest motion pos.
        if (((event.getX() > boundarys[0]) && (event.getX() < boundarys[1]))
                && ((event.getY() > boundarys[2]) && (event.getY() < boundarys[3]))) {
            return true;
        }

        // Calculate screen position by boundingbox's [minX, maxY, minZ].
        boundarys = findMaximum(boundarys, new int[]{0, 4, 2});

        // Hit test motion pos.
        if (((event.getX() > boundarys[0]) && (event.getX() < boundarys[1]))
                && ((event.getY() > boundarys[2]) && (event.getY() < boundarys[3]))) {
            return true;
        }

        // Calculate screen position by boundingbox's [minX, maxY, maxZ].
        boundarys = findMaximum(boundarys, new int[]{0, 4, 5});

        // Hit test motion pos.
        if (((event.getX() > boundarys[0]) && (event.getX() < boundarys[1]))
                && ((event.getY() > boundarys[2]) && (event.getY() < boundarys[3]))) {
            return true;
        }

        // Calculate screen position by boundingbox's [maxX, minY, minZ].
        boundarys = findMaximum(boundarys, new int[]{3, 1, 2});

        // Hit test motion pos.
        if (((event.getX() > boundarys[0]) && (event.getX() < boundarys[1]))
                && ((event.getY() > boundarys[2]) && (event.getY() < boundarys[3]))) {
            return true;
        }

        // Calculate screen position by boundingbox's [maxX, minY, maxZ].
        boundarys = findMaximum(boundarys, new int[]{3, 1, 5});

        // Hit test motion pos.
        if (((event.getX() > boundarys[0]) && (event.getX() < boundarys[1]))
                && ((event.getY() > boundarys[2]) && (event.getY() < boundarys[3]))) {
            return true;
        }

        // Calculate screen position by boundingbox's [maxX, maxY, minZ].
        boundarys = findMaximum(boundarys, new int[]{3, 4, 2});

        // Hit test motion pos.
        if (((event.getX() > boundarys[0]) && (event.getX() < boundarys[1]))
                && ((event.getY() > boundarys[2]) && (event.getY() < boundarys[3]))) {
            return true;
        }
        return false;
    }

    // minXmaxXminYmaxY size is 4
    // index size is 3
    private float[] findMaximum(float[] minXmaxXminYmaxY, int[] index) {
        float[] screenPos = calculateScreenPos(mBoundingBoxs[index[0]],
                mBoundingBoxs[index[1]], mBoundingBoxs[index[2]]);
        if (screenPos[0] < minXmaxXminYmaxY[0]) {
            minXmaxXminYmaxY[0] = screenPos[0];
        }
        if (screenPos[0] > minXmaxXminYmaxY[1]) {
            minXmaxXminYmaxY[1] = screenPos[0];
        }
        if (screenPos[1] < minXmaxXminYmaxY[2]) {
            minXmaxXminYmaxY[2] = screenPos[1];
        }
        if (screenPos[1] > minXmaxXminYmaxY[3]) {
            minXmaxXminYmaxY[3] = screenPos[1];
        }
        return minXmaxXminYmaxY;
    }

    /**
     * Convert the secondary coordinate system with the screen center as the left origin to the screen
     * pixel coordinate system with the top left corner of the screen as the origin.
     *
     * @param coordinateX Coordinate x component of point in secondary coordinate system.
     * @param coordinateY Coordinate y component of point in secondary coordinate system.
     * @param coordinateZ Coordinate z component of point in secondary coordinate system.
     * @return Coordinates of points. Screen pixel coordinate system,
     * The origin on the left is the upper left corner of the screen.
     */
    private float[] calculateScreenPos(float coordinateX, float coordinateY, float coordinateZ) {
        // The dimension of the point in OpenGL is 4.
        float[] vecs = new float[4];

        // Converting object coordinates to screen coordinates.
        vecs[0] = coordinateX;
        vecs[1] = coordinateY;
        vecs[2] = coordinateZ;
        vecs[3] = 1.0f;

        // Store clip space coordinate values of OpenGLES.
        float[] rets = new float[4];
        Matrix.multiplyMV(rets, 0, mModelViewProjectionMatrixs, 0, vecs, 0);

        // Divide by coordinate W component, convert to secondary coordinate system.
        rets[0] /= rets[3];
        rets[1] /= rets[3];
        rets[2] /= rets[3];

        // In the current coordinate system, left is negative, right is positive,
        // down is positive, and up is negative.
        // Adding 1 to the left of coordinate X is equivalent to moving the left of the left system,
        // and the operation of coordinate y is equivalent to moving up the coordinate system.
        rets[0] += 1.0f;
        rets[1] = 1.0f - rets[1];

        // Convert to pixel coordinates.
        rets[0] *= mWidth;
        rets[1] *= mHeight;

        // When w component is set to 1, the XY component caused by coordinate
        // system movement is eliminated to increase by 2 times.
        rets[3] = 1.0f;
        rets[0] /= 2.0f;
        rets[1] /= 2.0f;
        return rets;
    }

    // bounding box [minX, minY, minZ, maxX, maxY, maxZ]
    private void calculateBoundingBox(FloatBuffer vertices) {
        if (vertices.limit() < 3) {
            mBoundingBoxs[0] = 0.0f;
            mBoundingBoxs[1] = 0.0f;
            mBoundingBoxs[2] = 0.0f;
            mBoundingBoxs[3] = 0.0f;
            mBoundingBoxs[4] = 0.0f;
            mBoundingBoxs[5] = 0.0f;
            return;
        } else {
            mBoundingBoxs[0] = vertices.get(0);
            mBoundingBoxs[1] = vertices.get(1);
            mBoundingBoxs[2] = vertices.get(2);
            mBoundingBoxs[3] = vertices.get(0);
            mBoundingBoxs[4] = vertices.get(1);
            mBoundingBoxs[5] = vertices.get(2);
        }

        // The first three pairs are taken out as initial variables,
        // and then three maximum and three minimum values are found.
        int index = 3;
        while (index < vertices.limit() - 2) {
            if (vertices.get(index) < mBoundingBoxs[0]) {
                mBoundingBoxs[0] = vertices.get(index);
            }
            if (vertices.get(index) > mBoundingBoxs[3]) {
                mBoundingBoxs[3] = vertices.get(index);
            }
            index++;

            if (vertices.get(index) < mBoundingBoxs[1]) {
                mBoundingBoxs[1] = vertices.get(index);
            }
            if (vertices.get(index) > mBoundingBoxs[4]) {
                mBoundingBoxs[4] = vertices.get(index);
            }
            index++;

            if (vertices.get(index) < mBoundingBoxs[2]) {
                mBoundingBoxs[2] = vertices.get(index);
            }
            if (vertices.get(index) > mBoundingBoxs[5]) {
                mBoundingBoxs[5] = vertices.get(index);
            }
            index++;
        }
    }
}