/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2019-2020. All rights reserved.
 */

package com.example.pyvision.ar.world;

import android.view.MotionEvent;

/**
 * Gesture event class, including postural categories and creating postural events.
 *
 * @author HW
 * @since 2019-06-13
 */
public class GestureEvent {
    /**
     * define gesture event type, default value is 0.
     */
    public static final int GESTURE_EVENT_TYPE_UNKNOW = 0;

    /**
     * define gesture event type DOWN(1).
     */
    public static final int GESTURE_EVENT_TYPE_DOWN = 1;

    /**
     * define gesture event type SINGLETAPUP(2).
     */
    public static final int GESTURE_EVENT_TYPE_SINGLETAPUP = 2;

    /**
     * define gesture event type SCROLL(3).
     */
    public static final int GESTURE_EVENT_TYPE_SCROLL = 3;

    private int type;

    private MotionEvent e1;

    private MotionEvent e2;

    private float distanceX;

    private float distanceY;

    private float velocityX;

    private float velocityY;

    private GestureEvent() {
    }

    public int getType() {
        return type;
    }

    public MotionEvent getE1() {
        return e1;
    }

    public MotionEvent getE2() {
        return e2;
    }

    public float getDistanceX() {
        return distanceX;
    }

    public float getDistanceY() {
        return distanceY;
    }

    public float getVelocityX() {
        return velocityX;
    }

    public float getVelocityY() {
        return velocityY;
    }

    /**
     * This method will create a down event.
     *
     * @param motionEvent The down motion event.
     * @return Gesture event(DOWN).
     */
    static GestureEvent createDownEvent(MotionEvent motionEvent) {
        GestureEvent ret = new GestureEvent();
        ret.type = GESTURE_EVENT_TYPE_DOWN;
        ret.e1 = motionEvent;
        return ret;
    }

    /**
     * This method will create a SINGLETAPUP event.
     *
     * @param motionEvent MotionEvent
     * @return Gesture event(SINGLETAPUP)
     */
    static GestureEvent createSingleTapUpEvent(MotionEvent motionEvent) {
        GestureEvent ret = new GestureEvent();
        ret.type = GESTURE_EVENT_TYPE_SINGLETAPUP;
        ret.e1 = motionEvent;
        return ret;
    }

    /**
     * This method will create a scroll event.
     *
     * @param e1 The first down motion event that started the scrolling.
     * @param e2 The first down motion event that started the scrolling.
     * @param distanceX The distance along the X axis that has been scrolled since the last call to onScroll.
     * @param distanceY The distance along the Y axis that has been scrolled since the last call to onScroll.
     * @return Gesture event(SCROLL).
     */
    static GestureEvent createScrollEvent(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        GestureEvent ret = new GestureEvent();
        ret.type = GESTURE_EVENT_TYPE_SCROLL;
        ret.e1 = e1;
        ret.e2 = e2;
        ret.distanceX = distanceX;
        ret.distanceY = distanceY;
        return ret;
    }
}