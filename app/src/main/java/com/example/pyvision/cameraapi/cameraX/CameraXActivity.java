package com.example.pyvision.cameraapi.cameraX;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;

import com.example.pyvision.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * Created by zale.zhang on 2020/5/27.
 *
 * @author zale.zhang
 */
public class CameraXActivity extends AbstractCameraXNoWaitActivity{

    TextureView mTextureView;
    SurfaceView mSurfaceView;
    SurfaceHolder holder;
    Paint mPaint;
    static Bitmap colorBitmap;

    public native void process_bitmap(Bitmap bitmap);
    public native void transfer_color(Bitmap src_bitmap,Bitmap tar_bitmap);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (getSupportActionBar() != null){
//            getSupportActionBar().hide();
//        }

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setFilterBitmap(true);
        mPaint.setDither(true);

        mSurfaceView = findViewById(R.id.camera_image_view);

        try {
            colorBitmap = BitmapFactory.decodeStream(getAssets().open("autumn.jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        };
        holder = mSurfaceView.getHolder();
        holder.addCallback(mCallback);


    }

    @Override
    protected int getContentViewLayoutId() {
        return R.layout.activity_camerax_surface;
    }

    @Override
    protected TextureView getCameraPreviewTextureView() {
        mTextureView = findViewById(R.id.texture_view_test);
//        mTextureView = new TextureView(this);
        return mTextureView;
    }

    @Nullable
    @Override
    protected Object analyzeImage(ImageProxy image, int rotationDegrees) {

        long startTime= System.currentTimeMillis();
//        Bitmap bitmap = mTextureView.getBitmap();
//                Bitmap bitmap = toBitmap(image.getImage());
        Bitmap bitmap = imageToBitmap(image.getImage(),rotationDegrees);
        long getBitmapTime = System.currentTimeMillis();
        Log.i("getBitmap",String.format("方法使用时间 %d ms", getBitmapTime - startTime)); //打印使用时间

        if (bitmap != null){
//            process_bitmap(bitmap);
            transfer_color(colorBitmap,bitmap);
            long colorTransferTime = System.currentTimeMillis();
            Log.i("colorTransfer",String.format("方法使用时间 %d ms", colorTransferTime - getBitmapTime));

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            Rect srcRect = new Rect(0,0,width,height);
            Rect dstRect = new Rect(0,0,width,height);
            Canvas canvas = holder.lockCanvas();
            canvas.drawBitmap(bitmap,srcRect,dstRect,mPaint);
            holder.unlockCanvasAndPost(canvas);
            mSurfaceView.draw(canvas);
        }
        return "Object()";
    }

    @Override
    protected void applyToUiAnalyzeImageResult(Object result) {
//        mImageView.setImageBitmap(mTextureView.getBitmap());
    }

    private Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    public Bitmap imageToBitmap(Image image, float rotationDegrees) {

        assert (image.getFormat() == ImageFormat.NV21);

        // NV21 is a plane of 8 bit Y values followed by interleaved  Cb Cr
        ByteBuffer ib = ByteBuffer.allocate(image.getHeight() * image.getWidth() * 2);

        ByteBuffer y = image.getPlanes()[0].getBuffer();
        ByteBuffer cr = image.getPlanes()[1].getBuffer();
        ByteBuffer cb = image.getPlanes()[2].getBuffer();
        ib.put(y);
        ib.put(cb);
        ib.put(cr);

        YuvImage yuvImage = new YuvImage(ib.array(),
                ImageFormat.NV21, image.getWidth(), image.getHeight(), null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0,
                image.getWidth(), image.getHeight()), 50, out);
        byte[] imageBytes = out.toByteArray();
        Bitmap bm = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        Bitmap bitmap = bm;

        // On android the camera rotation and the screen rotation
        // are off by 90 degrees, so if you are capturing an image
        // in "portrait" orientation, you'll need to rotate the image.
        long rot_start = System.currentTimeMillis();
        if (rotationDegrees != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotationDegrees);
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bm,
                    bm.getWidth(), bm.getHeight(), true);
            bitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
        }
        long rot_end = System.currentTimeMillis();
        Log.i("rot_time:",String.format("方法使用时间 %d ms", rot_end-rot_start));
        return bitmap;
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        colorBitmap = null;
    }
}
