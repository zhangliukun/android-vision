package com.example.pyvision.colortransfer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.pyvision.R;
import com.example.pyvision.cameraapi.cameramix.CameraActivity;

import java.io.IOException;

/**
 * Created by zale.zhang on 2020/5/30.
 *
 * @author zale.zhang
 */
public class ColorTransferActivity2 extends CameraActivity {

    public native void transfer_color(Bitmap src_bitmap,Bitmap tar_bitmap,Bitmap result_bitmap);


//    private static int mTargetWidth = 1080,mTargetHeight=1920;
    private static int mTargetWidth = 1080,mTargetHeight=1440;

    // 176*144, 208*144,320*240,352*288,400*400,480*360,544*408...,640*480,960*540,960*720,1280*960,1920*1080
//    private static final Size DESIRED_PREVIEW_SIZE = new Size(240, 320);
    private static final Size DESIRED_PREVIEW_SIZE = new Size(360, 480);
//    private static final Size DESIRED_PREVIEW_SIZE = new Size(720, 960);
//    private static final Size DESIRED_PREVIEW_SIZE = new Size(1080, 1920);



    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    Bitmap rgbFrameBitmap;
    Bitmap restlt_Bitmap;
    static Bitmap colorBitmap;
    Paint mPaint;
    float rotateDegree;

    SurfaceHolder.Callback mCallback;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(null);

        try {
            colorBitmap = BitmapFactory.decodeStream(getAssets().open("autumn.jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                Log.i("Surfaceview","surfaceCreated");
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                Log.i("Surfaceview","surfaceChanged");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                Log.i("Surfaceview","surfaceDestroyed");

            }
        };

        surfaceView = findViewById(R.id.mix_surface);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(mCallback);


//        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint = new Paint();
//        mPaint.setFilterBitmap(true);
        mPaint.setAntiAlias(true);//抗锯齿
//        mPaint.setFilterBitmap(true);//对位图进行滤波处理
        mPaint.setDither(true);//放抖动
    }

    @Override
    protected void processImage() {

        long start_time = System.currentTimeMillis();

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
        readyForNextImage();

        if(!container_hidden){
            return;
        }

        if(rgbFrameBitmap != null){
            //调用jni方法
            transfer_color(colorBitmap,rgbFrameBitmap,restlt_Bitmap);

            int width = restlt_Bitmap.getWidth();
            int height = restlt_Bitmap.getHeight();
            Rect srcRect = new Rect(0,0,width,height);
            Rect dstRect = new Rect(0,0,width,height);
//            Rect dstRect = new Rect(0,0,1080,1920);
//            Rect dstRect = new Rect(0,0,width*2,height*2);
            Canvas canvas = surfaceHolder.lockCanvas();
            // 这里要防止surfaceview隐藏以后导致canvas为空的情况
            if(canvas == null){
                return;
            }
            canvas.drawBitmap(restlt_Bitmap,srcRect,dstRect,mPaint);
            surfaceHolder.unlockCanvasAndPost(canvas);
            surfaceView.draw(canvas);
        }

        long end_time = System.currentTimeMillis();
        Log.i("processImage",String.format("方法使用时间 %d ms", end_time - start_time)); //打印使用时间


    }

    @Override
    protected void onPreviewSizeChosen(Size size, int rotation) {
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        restlt_Bitmap = Bitmap.createBitmap(mTargetWidth, mTargetHeight, Bitmap.Config.ARGB_8888);
        Log.i("zale","onPreviewSizeChosen"); //打印使用时间
        rotateDegree = rotation;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_cameramix;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    boolean container_hidden = false;
    @Override
    protected void setNumThreads(int numThreads) {
        if (container_hidden){
            findViewById(R.id.container).setVisibility(View.VISIBLE);
            findViewById(R.id.mix_surface).setVisibility(View.GONE);
            container_hidden = false;
        }else {
            findViewById(R.id.container).setVisibility(View.GONE);
            findViewById(R.id.mix_surface).setVisibility(View.VISIBLE);
            container_hidden = true;
        }
    }

    @Override
    protected void setUseNNAPI(boolean isChecked) {

    }
}
