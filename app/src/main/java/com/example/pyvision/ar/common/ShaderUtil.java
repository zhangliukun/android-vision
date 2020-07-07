/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.example.pyvision.ar.common;

import android.opengl.GLES20;
import android.util.Log;

/**
 * Read the shader program and compile links.
 *
 * @author HW
 * @since 2020-04-05
 */
public class ShaderUtil {
    private ShaderUtil() {
    }

    /**
     * Check openGL runtime error.
     *
     * @param tag Log information.
     * @param label Programe label.
     */
    public static void checkGlError(String tag, String label) {
        int lastError = GLES20.GL_NO_ERROR;
        int error = GLES20.glGetError();
        while (error != GLES20.GL_NO_ERROR) {
            Log.e(tag, label + ": glError " + error);
            lastError = error;
            error = GLES20.glGetError();
        }
        if (lastError != GLES20.GL_NO_ERROR) {
            throw new ArDemoRuntimeException(label + ": glError " + error);
        }
    }
}