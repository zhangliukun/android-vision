package com.example.pyvision.colortransfer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.pyvision.R;
import com.example.pyvision.cameraapi.cameramix.CameraActivity;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by zale.zhang on 2020/5/30.
 *
 * @author zale.zhang
 */
public class ColorTransferActivity2 extends CameraActivity {

    public native void transfer_color(Bitmap src_bitmap,Bitmap tar_bitmap,Bitmap result_bitmap);

    // 相册选择回传吗
    public final static int GALLERY_REQUEST_CODE = 1;

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
    Button choosePicBtn;
    Button switchModeBtn;
    boolean container_hidden = false;

    SurfaceHolder.Callback mCallback;


    Uri imageUri;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(null);
        choosePicBtn = findViewById(R.id.choosePic);
        switchModeBtn = findViewById(R.id.switch_mode);
        choosePicBtn.setVisibility(View.VISIBLE);
        switchModeBtn.setVisibility(View.VISIBLE);
        findViewById(R.id.bottom_sheet_layout).setVisibility(View.GONE);
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

        choosePicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                choosePhotoCheckPermission();
            }
        });

        switchModeBtn.setOnClickListener((View view) ->{
            if (container_hidden){
                findViewById(R.id.container).setVisibility(View.VISIBLE);
                findViewById(R.id.mix_surface).setVisibility(View.GONE);
                container_hidden = false;
            }else {
                findViewById(R.id.container).setVisibility(View.GONE);
                findViewById(R.id.mix_surface).setVisibility(View.VISIBLE);
                container_hidden = true;
            }
        });


//        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint = new Paint();
//        mPaint.setFilterBitmap(true);
        mPaint.setAntiAlias(true);//抗锯齿
//        mPaint.setFilterBitmap(true);//对位图进行滤波处理
        mPaint.setDither(true);//放抖动
    }

    private void choosePhotoCheckPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    GALLERY_REQUEST_CODE);
        } else {
            choosePhoto();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == this.RESULT_OK){
            switch (requestCode){
                case GALLERY_REQUEST_CODE:
                    imageUri = data.getData();
                    if(imageUri!=null) {
                        Bitmap bit = null;
                        try {
                            BitmapFactory.Options newOpts = new BitmapFactory.Options();
//                            newOpts.inJustDecodeBounds = true;//只读取bitmap的宽高信息
//                            newOpts.inJustDecodeBounds = false;
                            newOpts.inSampleSize = 3; //压缩率设置为3
                            bit = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri),null,newOpts);

                            colorBitmap = bit;
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        Log.i("bit", String.valueOf(bit));
                    }
                    break;
            }
        }
    }

    private void choosePhoto(){
        Intent intentToPickPic = new Intent(Intent.ACTION_PICK, null);
        // 如果限制上传到服务器的图片类型时可以直接写如："image/jpeg 、 image/png等的类型" 所有类型则写 "image/*"
        intentToPickPic.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/jpeg");
        startActivityForResult(intentToPickPic, GALLERY_REQUEST_CODE);
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


    @Override
    protected void setNumThreads(int numThreads) {

    }

    @Override
    protected void setUseNNAPI(boolean isChecked) {

    }

    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        colorBitmap = null;
    }
}
