package com.example.pyvision.opengl;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;

import com.example.pyvision.opengl.camera.Camera2Proxy;
import com.example.pyvision.opengl.display.ImageDisplay;
import com.example.pyvision.utils.ImageUtils;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by zale.zhang on 2020/6/9.
 *
 * @author zale.zhang
 */
public class Camera2GLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    public native void transfer_color(Bitmap src_bitmap,Bitmap tar_bitmap,Bitmap result_bitmap);

    private static final String TAG = "Camera2GLSurfaceView";
    private Camera2Proxy mCameraProxy;
    private SurfaceTexture mSurfaceTexture;
    private CameraDrawer mDrawer;
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private float mOldDistance;
    private int mTextureId = -1;

    private int count =0;
    private Activity mActivity;
    private ImageDisplay imageDisplay = new ImageDisplay();
    private int[] rgbBytes = null;
    private byte[][] yuvBytes = new byte[3][];
    private int yRowStride;
    private Bitmap rgbFrameBitmap;
    private Bitmap colorBitmap;
    private Bitmap resultBitmap;
    private boolean showPreview = true;

    public Camera2GLSurfaceView(Context context) {
        this(context, null);
    }

    public Camera2GLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mActivity = (Activity) context;
        mCameraProxy = new Camera2Proxy((Activity) context);
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
//        mTextureId = OpenGLUtils.getExternalOESTextureID();
//        mSurfaceTexture = new SurfaceTexture(mTextureId);
//        mSurfaceTexture.setOnFrameAvailableListener(this);
//        mCameraProxy.setPreviewSurface(mSurfaceTexture);
        mDrawer = new CameraDrawer();
        Log.d(TAG, "onSurfaceCreated. width: " + getWidth() + ", height: " + getHeight());
        mCameraProxy.openCamera(getWidth(), getHeight());
        Size size = mCameraProxy.getPreviewSize();

        rgbFrameBitmap = Bitmap.createBitmap(size.getWidth(), size.getHeight(), Bitmap.Config.ARGB_8888);
        resultBitmap = Bitmap.createBitmap(size.getWidth(), size.getHeight(), Bitmap.Config.ARGB_8888);
//        resultBitmap = Bitmap.createBitmap(size.getHeight(), size.getWidth(), Bitmap.Config.ARGB_8888);
        imageDisplay.init(rgbFrameBitmap);

        mCameraProxy.setPreviewImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {

                long start_time = System.currentTimeMillis();

                int previewWidth = size.getWidth();
                int previewHeight = size.getHeight();
                if (rgbBytes == null) {
                    rgbBytes = new int[previewWidth * previewHeight];
                }
                final Image image = reader.acquireLatestImage();

                long acquireLatestImage = System.currentTimeMillis();
                Log.i("zale","acquireLatestImage:"+(acquireLatestImage- start_time)+"ms");
//
                Trace.beginSection("imageAvailable");
                if(image == null){
                    Log.i("zale","image == null,continue");
                    return;
                }
                final Image.Plane[] planes = image.getPlanes();
                fillBytes(planes, yuvBytes);
                yRowStride = planes[0].getRowStride();
                final int uvRowStride = planes[1].getRowStride();
                final int uvPixelStride = planes[1].getPixelStride();

                long fill_bytes = System.currentTimeMillis();
                Log.i("zale","fill_bytes:"+(fill_bytes- acquireLatestImage)+"ms");

                ImageUtils.convertYUV420ToARGB8888(
                        yuvBytes[0],
                        yuvBytes[1],
                        yuvBytes[2],
                        previewWidth,
                        previewHeight,
                        yRowStride,
                        uvRowStride,
                        uvPixelStride,
                        rgbBytes);

                long convert = System.currentTimeMillis();
                Log.i("zale","convert:"+(convert- fill_bytes)+"ms");

                rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);
                image.close();

                long setPixels = System.currentTimeMillis();
                Log.i("zale","setPixels:"+(setPixels- convert)+"ms");

                if(colorBitmap != null){
                    transfer_color(colorBitmap,rgbFrameBitmap,resultBitmap);
                }

                long transcolor = System.currentTimeMillis();
                Log.i("zale","transcolor:"+(transcolor- setPixels)+"ms");

                requestRender();
            }
        });

    }

    public void setColorBitmap(Bitmap bitmap){
        colorBitmap = bitmap;
    }

    public void setShowPreview(boolean showPreview){
        this.showPreview = showPreview;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged. thread: " + Thread.currentThread().getName());
        Log.d(TAG, "onSurfaceChanged. width: " + width + ", height: " + height);
        int previewWidth = mCameraProxy.getPreviewSize().getWidth();
        int previewHeight = mCameraProxy.getPreviewSize().getHeight();
        if (width > height) {
            setAspectRatio(previewWidth, previewHeight);
        } else {
            setAspectRatio(previewHeight, previewWidth);
        }
        GLES20.glViewport(0, 0, width, height);
//        imageDisplay.adjustImageScaling(width,height);// 画普通图像的时候可能需要进行尺度的调整，但是摄像机预览的话因为已经设定了比例，所以不需要调整
    }

    private long global_timestamp = System.currentTimeMillis();

    @Override
    public void onDrawFrame(GL10 gl) {
        long start = System.currentTimeMillis();

        Log.i("zale","total_onDrawFrame_tims:"+(start- global_timestamp)+"ms");

        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
//        mSurfaceTexture.updateTexImage();
//        mDrawer.draw(mTextureId, mCameraProxy.isFrontCamera());


        if(this.showPreview){
            if(rgbFrameBitmap != null){
                imageDisplay.onDrawFrame(rgbFrameBitmap);
            }
        }else {
            if(resultBitmap != null){
                imageDisplay.onDrawFrame(resultBitmap);
            }
        }

        long onDrawFrame = System.currentTimeMillis();
        global_timestamp = onDrawFrame;
        Log.i("zale","onDrawFrame:"+(onDrawFrame- start)+"ms");

    }

    /**
     * 这个方法回调是使用新建的纹理SurfaceTexture绑定到onFrameAvailableListener得到的回调，调用requestRender
     * 使得opengl进行渲染，surfaceTexture需要调用updateTexImage更新纹理数据
     * @param surfaceTexture
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }

    private void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        post(new Runnable() {
            @Override
            public void run() {
                requestLayout(); // must run in UI thread
            }
        });
    }

    public Camera2Proxy getCameraProxy() {
        return mCameraProxy;
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1) {
            // 点击聚焦
            mCameraProxy.focusOnPoint((int) event.getX(), (int) event.getY(), getWidth(), getHeight());
            return true;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                mOldDistance = getFingerSpacing(event);
                break;
            case MotionEvent.ACTION_MOVE:
                float newDistance = getFingerSpacing(event);
                if (newDistance > mOldDistance) {
                    mCameraProxy.handleZoom(true);
                } else if (newDistance < mOldDistance) {
                    mCameraProxy.handleZoom(false);
                }
                mOldDistance = newDistance;
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
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

}