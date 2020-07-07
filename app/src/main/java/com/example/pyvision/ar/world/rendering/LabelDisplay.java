/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.example.pyvision.ar.world.rendering;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.util.Pair;

import com.example.pyvision.ar.common.ShaderUtil;
import com.huawei.hiar.ARPlane;
import com.huawei.hiar.ARPose;
import com.huawei.hiar.ARTrackable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

/**
 * This class demonstrates how to use the data ARPlane, including how to
 * get the center point of the plane post. On the premise that the plane
 * type can be recognized, the plane type will also be displayed at the
 * center point of the plane, otherwise it will be displayed as "other"
 *
 * @author HW
 * @since 2020-04-08
 */
public class LabelDisplay {
    private static final String TAG = LabelDisplay.class.getSimpleName();

    private static final String LS = System.lineSeparator();

    private static final int COORDS_PER_VERTEX = 3; // x, z, alpha

    private static final float LABEL_WIDTH = 0.3f;

    private static final float LABEL_HEIGHT = 0.3f;

    private static final int TEXTURES_SIZE = 12;

    private static final int MATRIX_SIZE = 16;

    private static final int PLANE_ANGLE_MATRIX_SIZE = 4;

    private final int[] textures = new int[TEXTURES_SIZE];

    // Temporary lists/matrices allocated here to reduce number of allocations for each frame.
    private final float[] modelMatrix = new float[MATRIX_SIZE];

    private final float[] modelViewMatrix = new float[MATRIX_SIZE];

    private final float[] modelViewProjectionMatrix = new float[MATRIX_SIZE];

    // 2x2 rotation matrix applied to uv coordinates.
    private final float[] planeAngleUvMatrix = new float[PLANE_ANGLE_MATRIX_SIZE];

    private int mProgram;

    private int glPositionParameter;

    private int glModelViewProjectionMatrix;

    private int glTexture;

    private int glPlaneUvMatrix;

    /**
     * Created and compiler label display shader on the OpenGL Thread.
     * This method will be called when {@link RenderUtil#onSurfaceCreated}.
     *
     * @param labelBitmaps View data for display plane type.
     */
    public void init(ArrayList<Bitmap> labelBitmaps) {
        if (labelBitmaps.size() == 0) {
            Log.e(TAG, "no bitmap");
        }
        createProgram();
        int idx = 0;
        GLES20.glGenTextures(textures.length, textures, 0);
        for (Bitmap labelBitmap : labelBitmaps) {
            // for semantic label plane
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + idx);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[idx]);

            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, labelBitmap, 0);
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            idx++;
            ShaderUtil.checkGlError(TAG, "Texture loading");
        }
        ShaderUtil.checkGlError(TAG, "Program parameters");
    }

    private void createProgram() {
        mProgram = WorldShaderUtil.getLabelProgram();
        ShaderUtil.checkGlError(TAG, "program");
        glPositionParameter = GLES20.glGetAttribLocation(mProgram, "inPosXZAlpha");
        glModelViewProjectionMatrix =
            GLES20.glGetUniformLocation(mProgram, "inMVPMatrix");
        glTexture = GLES20.glGetUniformLocation(mProgram, "inTexture");
        glPlaneUvMatrix = GLES20.glGetUniformLocation(mProgram, "inPlanUVMatrix");
        ShaderUtil.checkGlError(TAG, "program params");
    }

    /**
     * Render the type of plane at the center of the currently recognized plane
     * This method will be called when {@link RenderUtil#onDrawFrame}.
     *
     * @param allPlanes All currently recognized planes.
     * @param cameraPose Current camera position and attitude
     * @param cameraProjection Projection matrix of current camera
     */
    public void onDrawFrame(Collection<ARPlane> allPlanes, ARPose cameraPose, float[] cameraProjection) {
        ArrayList<ARPlane> sortedPlanes = getSortedPlanes(allPlanes, cameraPose);
        float[] cameraViewMatrix = new float[MATRIX_SIZE];
        cameraPose.inverse().toMatrix(cameraViewMatrix, 0);
        drawSortedPlans(sortedPlanes, cameraViewMatrix, cameraProjection);
    }

    private ArrayList<ARPlane> getSortedPlanes(Collection<ARPlane> allPlanes, ARPose cameraPose) {
        // Planes must be sorted by distance from camera so that we draw closer planes first, and
        // they occlude the farther planes.
        ArrayList<Pair<ARPlane, Float>> pairPlanes = new ArrayList<>();
        for (ARPlane plane : allPlanes) {
            if ((plane.getType() == ARPlane.PlaneType.UNKNOWN_FACING)
                || plane.getTrackingState() != ARTrackable.TrackingState.TRACKING
                || plane.getSubsumedBy() != null) {
                continue;
            }

            // store the current plane's normal vector.
            float[] planeNormalVector = new float[3];
            ARPose planeCenterPose = plane.getCenterPose();
            planeCenterPose.getTransformedAxis(1, 1.0f, planeNormalVector, 0);

            // Calculate the distance from the camera to the plane. If it is a negative number,
            // it means it is on the back of the plane (the normal vector distinguishes the front and the back)
            float distanceBetweenPlaneAndCamera = (cameraPose.tx() - planeCenterPose.tx()) * planeNormalVector[0]
                + (cameraPose.ty() - planeCenterPose.ty()) * planeNormalVector[1]
                + (cameraPose.tz() - planeCenterPose.tz()) * planeNormalVector[2];
            pairPlanes.add(new Pair<>(plane, distanceBetweenPlaneAndCamera));
        }

        pairPlanes.sort(new Comparator<Pair<ARPlane, Float>>() {
            @Override
            public int compare(Pair<ARPlane, Float> planA, Pair<ARPlane, Float> planB) {
                return planB.second.compareTo(planA.second);
            }
        });

        ArrayList<ARPlane> sortedPlanes = new ArrayList<>();
        for (Pair<ARPlane, Float> eachPlane: pairPlanes) {
            sortedPlanes.add(eachPlane.first);
        }
        return sortedPlanes;
    }

    private void drawSortedPlans(ArrayList<ARPlane> sortedPlanes, float[] cameraViews, float[] cameraProjection) {
        // Start by clearing the alpha channel of the color buffer to 1.0.
        GLES20.glClearColor(1, 1, 1, 1);
        GLES20.glColorMask(false, false, false, true);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glColorMask(true, true, true, true);

        // Disable depth write.
        GLES20.glDepthMask(false);

        // Additive blending, masked by alpha channel, clearing alpha channel.
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFuncSeparate(
            GLES20.GL_DST_ALPHA, GLES20.GL_ONE, GLES20.GL_ZERO, GLES20.GL_ONE_MINUS_SRC_ALPHA); // ALPHA (src, dest)

        // Set up the shader.
        GLES20.glUseProgram(mProgram);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(glPositionParameter);

        for (ARPlane plane: sortedPlanes) {
            float[] planeMatrix = new float[MATRIX_SIZE];
            plane.getCenterPose().toMatrix(planeMatrix, 0);

            System.arraycopy(planeMatrix, 0, modelMatrix, 0, MATRIX_SIZE);

            float scaleU = 1.0f / LABEL_WIDTH;

            // Set value for plane angle uv matrix.
            planeAngleUvMatrix[0] = scaleU;
            planeAngleUvMatrix[1] = 0.0f;
            planeAngleUvMatrix[2] = 0.0f;
            float scaleV = 1.0f / LABEL_HEIGHT;
            planeAngleUvMatrix[3] = scaleV;

            // Attach the texture.
            // Enumeration does not provide a method to get enumeration ordinal.
            int idx = plane.getLabel().ordinal();
            Log.d(TAG, "plane getLabel:" + idx);
            idx = Math.abs(idx);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + idx);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[idx]);
            GLES20.glUniform1i(glTexture, idx);
            GLES20.glUniformMatrix2fv(glPlaneUvMatrix, 1, false, planeAngleUvMatrix, 0);

            drawLabel(cameraViews, cameraProjection);
        }

        // Clean up the state we set
        GLES20.glDisableVertexAttribArray(glPositionParameter);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDepthMask(true);
        ShaderUtil.checkGlError(TAG, "Cleaning up after drawing planes");
    }

    private void drawLabel(float[] cameraViews, float[] cameraProjection) {
        // Build the ModelView and ModelViewProjection matrices
        // for calculating cube position and light.
        Matrix.multiplyMM(modelViewMatrix, 0, cameraViews, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraProjection, 0, modelViewMatrix, 0);

        // Take the general length and width.
        float halfWidth = LABEL_WIDTH / 2.0f;
        float halfHeight = LABEL_HEIGHT / 2.0f;
        float[] vertices = {
            -halfWidth, -halfHeight, 1,
            -halfWidth, halfHeight, 1,
            halfWidth, halfHeight, 1,
            halfWidth, -halfHeight, 1,
        };

        // The size of each float is 4 bits.
        FloatBuffer vetBuffer = ByteBuffer.allocateDirect(4 * vertices.length).
            order(ByteOrder.nativeOrder()).asFloatBuffer();
        vetBuffer.rewind();
        for (int i = 0; i < vertices.length; ++i) {
            vetBuffer.put(vertices[i]);
        }
        vetBuffer.rewind();

        // The size of each float is 4 bits.
        GLES20.glVertexAttribPointer(glPositionParameter, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
            false, 4 * COORDS_PER_VERTEX, vetBuffer);

        // Sets the order in which OpenGL draws points, resulting in two triangles that form a plane.
        short[] indices = {0, 1, 2, 0, 2, 3 };

        // The size of each float is 2 bits.
        ShortBuffer idxBuffer = ByteBuffer.allocateDirect(2 * indices.length).
            order(ByteOrder.nativeOrder()).asShortBuffer();
        idxBuffer.rewind();
        for (int i = 0; i < indices.length; ++i) {
            idxBuffer.put(indices[i]);
        }
        idxBuffer.rewind();

        GLES20.glUniformMatrix4fv(glModelViewProjectionMatrix, 1, false, modelViewProjectionMatrix, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, idxBuffer.limit(), GLES20.GL_UNSIGNED_SHORT, idxBuffer);
        ShaderUtil.checkGlError(TAG, "Drawing plane");
    }
}