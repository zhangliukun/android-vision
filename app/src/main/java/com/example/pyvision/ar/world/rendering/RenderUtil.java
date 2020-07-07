package com.example.pyvision.ar.world.rendering;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.example.pyvision.R;
import com.example.pyvision.ar.common.DisplayRotationUtil;
import com.example.pyvision.ar.common.ImageUtils;
import com.example.pyvision.ar.common.TextDisplayUtil;
import com.example.pyvision.ar.common.TextureRenderUtil;
import com.example.pyvision.ar.world.GestureEvent;
import com.example.pyvision.ar.world.VirtualObject;
import com.example.pyvision.common.opencv.ColorTransferUtil;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARHitResult;
import com.huawei.hiar.ARLightEstimate;
import com.huawei.hiar.ARPlane;
import com.huawei.hiar.ARPoint;
import com.huawei.hiar.ARPose;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARTrackable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by zale.zhang on 2020/6/29.
 *
 * @author zale.zhang
 */

public class RenderUtil implements GLSurfaceView.Renderer{

    private static final String TAG = RenderUtil.class.getSimpleName();

    private static final int PROJ_MATRIX_OFFSET = 0;

    private static final float PROJ_MATRIX_NEAR = 0.1f;

    private static final float PROJ_MATRIX_FAR = 100.0f;

    private static final float MATERIAL_AMBIENT = 0.0f;

    private static final float MATERIAL_DIFFUSE = 3.5f;

    private static final float MATERIAL_SPECULAR = 1.0f;

    private static final float MATRIX_SCALE_SX = -1.0f;

    private static final float MATRIX_SCALE_SY = -1.0f;

    private static final float MATERIAL_SPECULAI_POWER = 6.0f;

    private static final float[] BLUE_COLORS = new float[] {66.0f, 133.0f, 244.0f, 255.0f};

    private static final float[] GREEN_COLORS = new float[] {66.0f, 133.0f, 244.0f, 255.0f};

    private ARSession mSession;

    private Activity mActivity;

    private Context mContext;

    private TextView mTextView;

    private TextView mSearchingTextView;

    private int frames = 0;

    private long lastInterval;

    private float fps;

    private TextureRenderUtil mTextureRenderUtil = new TextureRenderUtil();

    private SceneRenderUtil mSceneRenderUtil = new SceneRenderUtil();

    private TextDisplayUtil mTextDisplayUtil = new TextDisplayUtil();

    private LabelDisplay mLabelDisplay = new LabelDisplay();

    private ObjectDisplay mObjectDisplay = new ObjectDisplay();

    private DisplayRotationUtil mDisplayRotationUtil;

    private ArrayBlockingQueue<GestureEvent> mQueuedSingleTaps;

    private ArrayList<VirtualObject> mVirtualObjects = new ArrayList<>();

    private VirtualObject mSelectedObj = null;

    int i =0;

    private int[] rgbBytes = null;
    private byte[][] yuvBytes = new byte[3][];
    private Bitmap rgbBitmap;
    private Bitmap colorBitmap;
    private Bitmap resultBitmap;
    private boolean isTransfer = false;
    private boolean isTracking = false;

    /**
     * Constructor, passing in context and activity.
     * This method will be called by {@link Activity#onCreate}
     *
     * @param activity Activity
     * @param context Context
     */
    public RenderUtil(Activity activity, Context context) {
        mActivity = activity;
        mContext = context;
        mTextView = activity.findViewById(R.id.wordTextView);
        mSearchingTextView = activity.findViewById(R.id.searchingTextView);
    }

    public void setColorBitmap(Bitmap bitmap){
        colorBitmap = bitmap;
    }

    public void setTransfer(boolean transfer){
        isTransfer = transfer;
        if(isTransfer){
            mObjectDisplay.updateTextureImg(mContext,colorBitmap);
        }
    }

    public boolean getTransfer(){
        return isTransfer;
    }

    /**
     * Set AR session for updating in onDrawFrame to get the latest data.
     *
     * @param arSession ARSession.
     */
    public void setArSession(ARSession arSession) {
        if (arSession == null) {
            Log.e(TAG, "setSession error, arSession is null!");
            return;
        }
        mSession = arSession;
    }

    /**
     * Set queued single taps.
     *
     * @param queuedSingleTaps queuedSingleTaps
     */
    public void setQueuedSingleTaps(ArrayBlockingQueue<GestureEvent> queuedSingleTaps) {
        if (queuedSingleTaps == null) {
            Log.e(TAG, "setSession error, arSession is null!");
            return;
        }
        mQueuedSingleTaps = queuedSingleTaps;
    }

    /**
     * Set displayRotationUtil, this object will be used in onSurfaceChanged and onDrawFrame.
     *
     * @param displayRotationUtil DisplayRotationUtil.
     */
    public void setDisplayRotationUtil(DisplayRotationUtil displayRotationUtil) {
        if (displayRotationUtil == null) {
            Log.e(TAG, "setDisplayRotationUtil error, displayRotationUtil is null!");
            return;
        }
        mDisplayRotationUtil = displayRotationUtil;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Clear color, set window color.
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        mTextureRenderUtil.init();
        mSceneRenderUtil.init();
        mTextDisplayUtil.setListener(new TextDisplayUtil.OnTextInfoChangeListener() {
            @Override
            public boolean textInfoChanged(String text, float positionX, float positionY) {
                showWorldTypeTextView(text, positionX, positionY);
                return true;
            }
        });

        mLabelDisplay.init(getPlaneBitmaps());

        mObjectDisplay.init(mContext);
    }

    /**
     * Create a thread for UI display text, which will be used by gesture gesture rendering callback.
     *
     * @param text Gesture information for display on screen
     * @param positionX The left padding in pixels.
     * @param positionY The left padding in pixels
     */
    private void showWorldTypeTextView(final String text, final float positionX, final float positionY) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextView.setTextColor(Color.WHITE);

                // Set the size of the font used for display.
                mTextView.setTextSize(10f);
                if (text != null) {
                    mTextView.setText(text);
                    mTextView.setPadding((int) positionX, (int) positionY, 0, 0);
                } else {
                    mTextView.setText("");
                }
            }
        });
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        mTextureRenderUtil.onSurfaceChanged(width, height);
        mSceneRenderUtil.onSurfaceChanged(width,height);
        GLES20.glViewport(0, 0, width, height);
        mDisplayRotationUtil.updateViewportRotation(width, height);
        mObjectDisplay.setSize(width, height);
    }

    private void convertToBitmap(Image image){

        final Image.Plane[] planes = image.getPlanes();
        int previewWidth = image.getWidth();//640
        int previewHeight = image.getHeight();//480

        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        if( rgbBitmap == null){
            rgbBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        }

        if (resultBitmap == null){
            resultBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        }

        fillBytes(planes, yuvBytes);
        int yRowStride = planes[0].getRowStride();
        final int uvRowStride = planes[1].getRowStride();
        final int uvPixelStride = planes[1].getPixelStride();
        ImageUtils.convertYUV420ToARGB8888(
                yuvBytes[0],
                yuvBytes[1],
                yuvBytes[2],
                image.getWidth(),
                image.getHeight(),
                yRowStride,
                uvRowStride,
                uvPixelStride,
                rgbBytes);
        rgbBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);
    }

    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        // 因为变量的行stride，所以不能提前知道yuv planes的真实的需要的dimensions
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        Log.d("TEST QU", "mQueuedSingleTaps length: " + mQueuedSingleTaps.size());

        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mSession == null) {
            return;
        }
        if (mDisplayRotationUtil.getDeviceRotation()) {
            mDisplayRotationUtil.updateArSessionDisplayGeometry(mSession);
        }

        try {
//            mSession.setCameraTextureName(mTextureRenderUtil.getExternalTextureId());
            mSession.setCameraTextureName(mSceneRenderUtil.getTextureId());
            ARFrame arFrame = mSession.update();
            ARCamera arCamera = arFrame.getCamera();

            if(arCamera.getTrackingState() == ARTrackable.TrackingState.TRACKING){
                Image arimage = arFrame.acquireCameraImage();
                if (arimage != null){
                    Log.i("zale", String.valueOf(arimage.getWidth()));
                    convertToBitmap(arimage);
                    if(colorBitmap != null && isTransfer){
                        ColorTransferUtil.transfer_color(colorBitmap,rgbBitmap,resultBitmap);
                    }
                }

            }

//            mTextureRenderUtil.onDrawFrame(arFrame);
            if(colorBitmap != null && isTransfer){
                mSceneRenderUtil.onDrawFrame(arFrame,resultBitmap);
            }else if(rgbBitmap != null){
                mSceneRenderUtil.onDrawFrame(arFrame,rgbBitmap);
            }

            // The size of projection matrix is 4 * 4.
            float[] projectionMatrix = new float[16];

            // Obtain the projection matrix of AR camera.
            arCamera.getProjectionMatrix(projectionMatrix, PROJ_MATRIX_OFFSET, PROJ_MATRIX_NEAR, PROJ_MATRIX_FAR);

            StringBuilder sb = new StringBuilder();
            updateMessageData(sb);
            mTextDisplayUtil.onDrawFrame(sb);

            // The size of view matrix is 4 * 4.
            float[] viewMatrix = new float[16];
            arCamera.getViewMatrix(viewMatrix, 0);
            for (ARPlane plane : mSession.getAllTrackables(ARPlane.class)) {
                if (plane.getType() != ARPlane.PlaneType.UNKNOWN_FACING
                        && plane.getTrackingState() == ARTrackable.TrackingState.TRACKING) {
                    hideLoadingMessage();
                    break;
                }
            }
            mLabelDisplay.onDrawFrame(
                    mSession.getAllTrackables(ARPlane.class), arCamera.getDisplayOrientedPose(), projectionMatrix);
            handleGestureEvent(arFrame, arCamera, projectionMatrix, viewMatrix);
            float lightPixelIntensity = 1;
            ARLightEstimate lightEstimate = arFrame.getLightEstimate();
            if (lightEstimate.getState() != ARLightEstimate.State.NOT_VALID) {
                lightPixelIntensity = lightEstimate.getPixelIntensity();
            }
            Iterator<VirtualObject> ite = mVirtualObjects.iterator();
            while (ite.hasNext()) {
                VirtualObject obj = ite.next();
                if (obj.getAnchor().getTrackingState() == ARTrackable.TrackingState.STOPPED) {
                    ite.remove();
                }
                if (obj.getAnchor().getTrackingState() == ARTrackable.TrackingState.TRACKING) {
                    mObjectDisplay.onDrawFrame(viewMatrix, projectionMatrix, lightPixelIntensity, obj);
                }
            }
        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    private ArrayList<Bitmap> getPlaneBitmaps() {
        ArrayList<Bitmap> bitmaps = new ArrayList<>();
        bitmaps.add(getPlaneBitmap(R.id.plane_other));
        bitmaps.add(getPlaneBitmap(R.id.plane_wall));
        bitmaps.add(getPlaneBitmap(R.id.plane_floor));
        bitmaps.add(getPlaneBitmap(R.id.plane_seat));
        bitmaps.add(getPlaneBitmap(R.id.plane_table));
        bitmaps.add(getPlaneBitmap(R.id.plane_ceiling));
        return bitmaps;
    }

    private Bitmap getPlaneBitmap(int id) {
        TextView view = mActivity.findViewById(id);
        view.setDrawingCacheEnabled(true);
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap bitmap = view.getDrawingCache();
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.setScale(MATRIX_SCALE_SX, MATRIX_SCALE_SY);
        if (bitmap == null) {
            return null;
        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return bitmap;
    }

    /**
     * Update gesture related data for display.
     *
     * @param sb string buffer.
     */
    private void updateMessageData(StringBuilder sb) {
        float fpsResult = doFpsCalculate();
        sb.append("FPS=" + fpsResult + System.lineSeparator());
    }

    private float doFpsCalculate() {
        ++frames;
        long timeNow = System.currentTimeMillis();

        // Convert milliseconds to seconds.
        if (((timeNow - lastInterval) / 1000.0f) > 0.5f) {
            fps = frames / ((timeNow - lastInterval) / 1000.0f);
            frames = 0;
            lastInterval = timeNow;
        }
        return fps;
    }

    private void hideLoadingMessage() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSearchingTextView != null) {
                    mSearchingTextView.setVisibility(View.GONE);
                    mSearchingTextView = null;
                }
            }
        });
    }

    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    private void handleGestureEvent(ARFrame arFrame, ARCamera arCamera, float[] projectionMatrix, float[] viewMatrix) {
        GestureEvent event = mQueuedSingleTaps.poll();
        if (event == null) {
            return;
        }

        // do nothing when no tracking
        if (arCamera.getTrackingState() != ARTrackable.TrackingState.TRACKING) {
            return;
        }

        int eventType = event.getType();
        switch (eventType) {
            case GestureEvent.GESTURE_EVENT_TYPE_DOWN: {
                doWhenEventTypeDown(viewMatrix, projectionMatrix, event);
                break;
            }
            case GestureEvent.GESTURE_EVENT_TYPE_SCROLL: {
                if (mSelectedObj == null) {
                    break;
                }
                ARHitResult hitResult = hitTest4Result(arFrame, arCamera, event.getE2());
                if (hitResult != null) {
                    mSelectedObj.setAnchor(hitResult.createAnchor());
                }
                break;
            }
            case GestureEvent.GESTURE_EVENT_TYPE_SINGLETAPUP: {
                // do nothing when selecting object.
                if (mSelectedObj != null) {
                    return;
                }

                MotionEvent tap = event.getE1();
                ARHitResult hitResult = null;

                hitResult = hitTest4Result(arFrame, arCamera, tap);

                // if hit both Plane and Point,take Plane at the first priority.
                if (hitResult == null) {
                    break;
                }
                doWhenEventTypeSingleTap(hitResult);
                break;
            }
            default: {
                Log.e(TAG, "unknown motion event type, and do nothing.");
            }
        }
    }

    private void doWhenEventTypeDown(float[] viewMatrix, float[] projectionMatrix, GestureEvent event) {
        if (mSelectedObj != null) {
            mSelectedObj.setIsSelected(false);
            mSelectedObj = null;
        }
        Iterator<VirtualObject> ite = mVirtualObjects.iterator();
        while (ite.hasNext()) {
            VirtualObject obj = ite.next();
            if (mObjectDisplay.hitTest(viewMatrix, projectionMatrix, obj, event.getE1())) {
                obj.setIsSelected(true);
                mSelectedObj = obj;
                break;
            }
        }
    }

    private void doWhenEventTypeSingleTap(ARHitResult hitResult) {
        // Hits are sorted by depth. Consider only closest hit on a plane or oriented point.
        // Limit the number of objects created to 10. This avoids overloading both the
        // rendering system and AREngine.
        if (mVirtualObjects.size() >= 10) {
            mVirtualObjects.get(0).getAnchor().detach();
            mVirtualObjects.remove(0);
        }

        ARTrackable currentTrackable = hitResult.getTrackable();
        if (currentTrackable instanceof ARPoint) {
            mVirtualObjects.add(new VirtualObject(hitResult.createAnchor(), BLUE_COLORS));
        } else if (currentTrackable instanceof ARPlane) {
            mVirtualObjects.add(new VirtualObject(hitResult.createAnchor(), GREEN_COLORS));
        } else {
            Log.i(TAG, "Hit result is not plane or point");
        }
    }

    private ARHitResult hitTest4Result(ARFrame frame, ARCamera camera, MotionEvent event) {
        ARHitResult hitResult = null;
        List<ARHitResult> hitTestResults = frame.hitTest(event);

        for (int i = 0; i < hitTestResults.size(); i++) {
            // Check if any plane was hit, and if it was hit inside the plane polygon
            ARHitResult hitResultTemp = hitTestResults.get(i);
            ARTrackable trackable = hitResultTemp.getTrackable();

            // Judge whether to click to the plane.
            boolean isPlanHitJudge = trackable instanceof ARPlane
                    && ((ARPlane) trackable).isPoseInPolygon(hitResultTemp.getHitPose())
                    && (calculateDistanceToPlane(hitResultTemp.getHitPose(), camera.getPose()) > 0);

            // Determine whether to click the midpoint cloud, and whether the clicked point is facing the camera.
            boolean isPointHitJudge = trackable instanceof ARPoint
                    && ((ARPoint) trackable).getOrientationMode() == ARPoint.OrientationMode.ESTIMATED_SURFACE_NORMAL;

            // Preferred plane point.
            if (isPlanHitJudge || isPointHitJudge) {
                hitResult = hitResultTemp;
                if (trackable instanceof ARPlane) {
                    break;
                }
            }
        }
        return hitResult;
    }

    /**
     * Calculate the normals distance to plane from cameraPose, the given planePose should have y axis
     * parallel to plane's normals, for example plane's center pose or hit test pose.
     *
     * @param planePose Plane pose.
     * @param cameraPose Camera pose.
     * @return Distance To plane.
     */
    private static float calculateDistanceToPlane(ARPose planePose, ARPose cameraPose) {
        // The dimension of the point is 3
        float[] normals = new float[3];

        // Get transformed Y axis of plane's coordinate system.
        planePose.getTransformedAxis(1, 1.0f, normals, 0);

        // Compute dot product of plane's normals with vector from camera to plane center.
        return (cameraPose.tx() - planePose.tx()) * normals[0] // 0:x
                + (cameraPose.ty() - planePose.ty()) * normals[1] // 1:y
                + (cameraPose.tz() - planePose.tz()) * normals[2]; // 2:z
    }

}
