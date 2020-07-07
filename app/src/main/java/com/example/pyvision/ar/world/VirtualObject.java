/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2019-2020. All rights reserved.
 */

package com.example.pyvision.ar.world;

import android.opengl.Matrix;

import com.huawei.hiar.ARAnchor;

/**
 * This class provides information about the attributes of virtual objects and how to set the attributes of objects.
 *
 * @author HW
 * @since 2019-06-13
 */
public class VirtualObject {
//    private static final float ROTATION_ANGLE = 315.0f;
    private static final float ROTATION_ANGLE = 150.0f;

    private static final int MATRIX_SIZE = 16;
//    private static final int MATRIX_SIZE = 5;

//    private static final float SCALE_FACTOR = 0.15f;
    private static final float SCALE_FACTOR = 0.35f; //越小物体越小

    private ARAnchor mArAnchor;

    private float[] mObjectColors;

    private float[] mModelMatrix = new float[MATRIX_SIZE];

    private boolean mIsSelectedFlag = false;

    /**
     * Instantiate this class according to the anchor class and the original color class provided by HW.
     *
     * @param arAnchor Anchors provided by HW
     * @param color4f Object color data
     */
    public VirtualObject(ARAnchor arAnchor, float[] color4f) {
        mArAnchor = arAnchor;
        mObjectColors = color4f;
        init();
    }

    @Override
    protected void finalize() throws Throwable {
        // If the anchor object is destructed, alert arengine to stop tracking the anchor
        if (mArAnchor != null) {
            mArAnchor.detach();
            mArAnchor = null;
        }
        super.finalize();
    }

    private void init() {
        // Set the first column of the right-hand-side Matrix matrix to the scaling coefficient.
        Matrix.setIdentityM(mModelMatrix, 0);
        mModelMatrix[0] = SCALE_FACTOR;
        mModelMatrix[5] = SCALE_FACTOR;
        mModelMatrix[10] = SCALE_FACTOR;

        // Rotate a certain angle along the Y axis.
        Matrix.rotateM(mModelMatrix, 0, ROTATION_ANGLE, 0f, 1f, 0f);
    }

    /**
     * Change anchor properties of instantiated objects by external assignment.
     *
     * @param arAnchor Anchors provided by HW
     */
    public void setAnchor(ARAnchor arAnchor) {
        if (mArAnchor != null) {
            mArAnchor.detach();
        }
        mArAnchor = arAnchor;
    }

    /**
     * Get anchor properties of instantiated objects.
     *
     * @return ARAnchor(Anchors provided by HW)
     */
    public ARAnchor getAnchor() {
        return mArAnchor;
    }

    /**
     * Obtain object color data for rendering.
     *
     * @return Color data
     */
    public float[] getColor() {
        if (mIsSelectedFlag) {
            // If the object is the selected object, return the reverse color of the current object color
            float[] rets = new float[4];
            rets[0] = 255.0f - mObjectColors[0];
            rets[1] = 255.0f - mObjectColors[1];
            rets[2] = 255.0f - mObjectColors[2];
            rets[3] = mObjectColors[3];
            return rets;
        } else {
            return mObjectColors;
        }
    }

    /**
     * Set the color of the object.
     *
     * @param color Color assigned to an object.
     */
    public void setColor(float[] color) {
        mObjectColors = color;
    }

    /**
     * Obtaining matrix data of objects.
     *
     * @return Matrix data of objects
     */
    public float[] getModelMatrix() {
        return mModelMatrix;
    }

    /**
     * Obtaining Anchor Matrix Data of Model
     *
     * @return Anchor Matrix Data of the Model
     */
    public float[] getModelAnchorMatrix() {
        float[] modelMatrix = new float[MATRIX_SIZE];
        if (mArAnchor != null) {
            mArAnchor.getPose().toMatrix(modelMatrix, 0);
        } else {
            Matrix.setIdentityM(modelMatrix, 0);
        }
        float[] rets = new float[MATRIX_SIZE];
        Matrix.multiplyMM(rets, 0, modelMatrix, 0, mModelMatrix, 0);
        return rets;
    }

    public boolean getIsSelectedFlag() {
        return mIsSelectedFlag;
    }

    /**
     * Set whether an object is selected
     *
     * @param isSelected A flag indicating whether an object is selected or not.
     */
    public void setIsSelected(boolean isSelected) {
        mIsSelectedFlag = isSelected;
    }
}