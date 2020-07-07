/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.example.pyvision.ar.common;

import android.opengl.Matrix;

/**
 * Matrix tools.
 *
 * @author HW
 * @since 2020-03-29
 */
public class MatrixUtil {
    private static final int MATRIX_SIZE = 16;

    private MatrixUtil() {
    }

    /**
     * Get matrix of the specified type.
     *
     * @param matrix Results of matrix obtained
     * @param width width
     * @param height height
     */
    public static void getProjectionMatrix(float[] matrix, int width, int height) {
        if (height > 0 && width > 0) {
            float[] projection = new float[MATRIX_SIZE];
            float[] camera = new float[MATRIX_SIZE];

            // Computes an orthographic projection matrix.
            Matrix.orthoM(projection, 0, -1, 1, -1, 1, 1, 3);
            Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
            Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0);
        }
    }

    /**
     * Three-dimensional data standardization method, dividing each number by the root of all square sums.
     *
     * @param vector Three-dimensional vector.
     */
    public static void normalizeVec3(float[] vector) {
        // This data has three dimensions(0,1,2)
        float length = 1.0f / (float) Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]);
        vector[0] *= length;
        vector[1] *= length;
        vector[2] *= length;
    }

    /**
     * Provide unit matrix(4 * 4).
     *
     * @return Returns matrix as an array.
     */
    public static float[] getOriginalMatrix() {
        return new float[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    }
}