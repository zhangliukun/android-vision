/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.example.pyvision.ar.common;

/**
 * This class shows how to get the information of the object in the tracking state.
 * Before using this function, you should set up listening. In listening, information
 * will be passed and displayed. In our example, we created a method to display information
 * in UI thread and set it to listen again.
 *
 * @author HW
 * @since 2020-03-16
 */
public class TextDisplayUtil {
    private OnTextInfoChangeListener mTextInfoListener;

    /**
     * Display the string information you given.
     * This method will be called by {@link android.opengl.GLSurfaceView.Renderer#onDrawFrame}.
     *
     * @param sb String builder.
     */
    public void onDrawFrame(StringBuilder sb) {
        if (sb == null) {
            showTextInfo();
            return;
        }
        showTextInfo(sb.toString());
    }

    /**
     * Set listener for show message in UI thread.
     * This method will be called by {@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated}.
     *
     * @param listener Text information change listener.
     */
    public void setListener(OnTextInfoChangeListener listener) {
        mTextInfoListener = listener;
    }

    /**
     * Perform this operation when text info changes.
     *
     * @author HW
     * @since 2020-03-16
     */
    public interface OnTextInfoChangeListener {
        /**
         * Display the given text message.
         *
         * @param text text
         * @param positionX X-coordinates of points
         * @param positionY Y-coordinates of points
         * @return changed or unchanged
         */
        boolean textInfoChanged(String text, float positionX, float positionY);
    }

    private void showTextInfo(String text) {
        if (mTextInfoListener != null) {
            mTextInfoListener.textInfoChanged(text, 0, 0);
        }
    }

    private void showTextInfo() {
        mTextInfoListener.textInfoChanged(null, 0, 0);
    }
}
