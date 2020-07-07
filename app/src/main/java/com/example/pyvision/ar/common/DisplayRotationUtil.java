/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.example.pyvision.ar.common;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.view.Display;
import android.view.WindowManager;

import com.huawei.hiar.ARSession;

/**
 * Helper to track the display rotations. In particular, the 180 degree rotations are not notified
 * by the onSurfaceChanged() callback, and thus they require listening to the android display
 * events.
 *
 * @author HW
 * @since 2020-03-20
 */
public class DisplayRotationUtil implements DisplayListener {
    private boolean mIsDeviceRotation;

    private int mViewportWidth;

    private int mViewportHeight;

    private final Context mContext;

    private final Display mDisplay;

    /**
     * Constructs the DisplayRotationHelper but does not register the listener yet.
     *
     * @param context the Android {@link Context}.
     */
    public DisplayRotationUtil(Context context) {
        mContext = context;
        mDisplay = context.getSystemService(WindowManager.class).getDefaultDisplay();
    }

    /**
     * Registers the display listener. This method should be called when activity is onResume.
     */
    public void registerDisplayListener() {
        mContext.getSystemService(DisplayManager.class).registerDisplayListener(this, null);
    }

    /**
     * Unregisters the display listener. This method should be called when activity is onPause.
     */
    public void unregisterDisplayListener() {
        mContext.getSystemService(DisplayManager.class).unregisterDisplayListener(this);
    }

    /**
     * When the device rotates, you need to update the viewport size and the sign of whether the
     * device color has rotated, so that the display geometry information of arsession can be updated correctly.
     * This method should be called when activity is onSurfaceChanged.
     *
     * @param width the updated width of the surface.
     * @param height the updated height of the surface.
     */
    public void updateViewportRotation(int width, int height) {
        mViewportWidth = width;
        mViewportHeight = height;
        mIsDeviceRotation = true;
    }

    /**
     * Judge whether the current equipment has been rotated。
     *
     * @return Equipment rotation judgment result。
     */
    public boolean getDeviceRotation() {
        return mIsDeviceRotation;
    }

    /**
     * If the device is rotated, update the device window of the current ARSession.
     * This method should be called when activity is onDrawFrame.
     *
     * @param session the {@link ARSession} object to set display geometry if display geometry changed.
     */
    public void updateArSessionDisplayGeometry(ARSession session) {
        int displayRotation = mDisplay.getRotation();
        session.setDisplayGeometry(displayRotation, mViewportWidth, mViewportHeight);
        mIsDeviceRotation = false;
    }

    @Override
    public void onDisplayAdded(int displayId) {
    }

    @Override
    public void onDisplayRemoved(int displayId) {
    }

    @Override
    public void onDisplayChanged(int displayId) {
        mIsDeviceRotation = true;
    }
}
