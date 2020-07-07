package com.example.pyvision.ar.world;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.pyvision.R;
import com.example.pyvision.ar.common.DisplayRotationUtil;
import com.example.pyvision.ar.world.rendering.RenderUtil;
import com.huawei.hiar.AREnginesApk;
import com.huawei.hiar.AREnginesSelector;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARWorldTrackingConfig;
import com.huawei.hiar.exceptions.ARCameraNotAvailableException;
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException;
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableDeviceNotCompatibleException;
import com.huawei.hiar.exceptions.ARUnavailableEmuiNotCompatibleException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException;
import com.huawei.hiar.exceptions.ARUnavailableUserDeclinedInstallationException;

import java.io.FileNotFoundException;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by zale.zhang on 2020/6/29.
 *
 * @author zale.zhang
 */

public class WorldActivity extends Activity {
    private static final String TAG = WorldActivity.class.getSimpleName();

    private static final int MOTIONEVENT_QUEUE_CAPACITY = 2;

    private static final int OPENGLES_VERSION = 2;

    private ARSession mArSession;

    private GLSurfaceView mSurfaceView;

    private boolean isRemindInstall = true;

    private RenderUtil mRenderUtil;

    private GestureDetector mGestureDetector;

    private DisplayRotationUtil mDisplayRotationUtil;

    private ArrayBlockingQueue<GestureEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(MOTIONEVENT_QUEUE_CAPACITY);

    private String message = null;

    private Button selectBtn;
    private Button previewBtn;

    private Uri imageUri;

    public final static int GALLERY_REQUEST_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.world_java_activity_main);

        mSurfaceView = findViewById(R.id.surfaceview);
        mDisplayRotationUtil = new DisplayRotationUtil(this);
        initGestureDetector();

        // Set up renderer.
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(OPENGLES_VERSION);

        // Set EGL config chooser, including byte size and short size.
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        mRenderUtil = new RenderUtil(this, this);
        mRenderUtil.setDisplayRotationUtil(mDisplayRotationUtil);
        mRenderUtil.setQueuedSingleTaps(mQueuedSingleTaps);

        mSurfaceView.setRenderer(mRenderUtil);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Judge whether the current device supports Huawei arengine. If not, end the application.
        if (!isSupportHuaweiArEngine()) {
            String showMessage = "This device does not support Huawei AREngine!";
            Toast.makeText(this, showMessage, Toast.LENGTH_LONG).show();
            finish();
        }

        selectBtn = findViewById(R.id.select_btn);
        previewBtn = findViewById(R.id.preview_btn);
        selectBtn.setOnClickListener(view ->{
            choosePhoto();
        });
        previewBtn.setOnClickListener(view->{
            mRenderUtil.setTransfer(!mRenderUtil.getTransfer());
        });
    }

    private void initGestureDetector() {
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                onGestureEvent(GestureEvent.createSingleTapUpEvent(e));
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                onGestureEvent(GestureEvent.createDownEvent(e));
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                onGestureEvent(GestureEvent.createScrollEvent(e1, e2, distanceX, distanceY));
                return true;
            }
        });

        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });
    }

    private void onGestureEvent(GestureEvent e) {
        mQueuedSingleTaps.offer(e);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        Exception exception = null;
        message = null;
        if (mArSession == null) {
            try {
                // Request to install arengine server. If it is already installed or
                // the user chooses to install it, it will work normally. Otherwise, set isRemindInstall to false.
                requestedInstall();

                // If the user rejects the installation, isRemindInstall is false.
                if (!isRemindInstall) {
                    return;
                }
                mArSession = new ARSession(this);
                ARWorldTrackingConfig config = new ARWorldTrackingConfig(mArSession);

                int supportedSemanticMode = mArSession.getSupportedSemanticMode();
                Log.d(TAG, "supportedSemanticMode:" + supportedSemanticMode);
                if (supportedSemanticMode != ARWorldTrackingConfig.SEMANTIC_NONE) {
                    Log.d(TAG, "supported mode:" + supportedSemanticMode);
                    config.setSemanticMode(supportedSemanticMode);
                }
                mArSession.configure(config);
                mRenderUtil.setArSession(mArSession);
            } catch (Exception capturedException) {
                exception = capturedException;
                setMessageWhenError(capturedException);
            }
            if (message != null) {
                stopArSession(exception);
                return;
            }
        }
        try {
            mArSession.resume();
        } catch (ARCameraNotAvailableException e) {
            Toast.makeText(this, "Camera open failed, please restart the app", Toast.LENGTH_LONG).show();
            mArSession = null;
            return;
        }
        mDisplayRotationUtil.registerDisplayListener();
        mSurfaceView.onResume();
    }

    /**
     * Judge whether the current application supports Huawei arengine on the modified equipment.
     *
     * @return Return inspection results.
     */
    private boolean isSupportHuaweiArEngine() {
        AREnginesSelector.AREnginesAvaliblity enginesAvaliblity =
                AREnginesSelector.checkAllAvailableEngines(this);
        return (enginesAvaliblity.getKeyValues()
                & AREnginesSelector.AREnginesAvaliblity.HWAR_ENGINE_SUPPORTED.getKeyValues()) != 0;
    }

    /**
     * At the time of onResume, request to install the AREngine service.
     * If the current device has been installed or the user agrees to
     * install it, it will work normally. If the device is not installed
     * and the user rejects the installation, setting isRemindInstall to
     * false means that the installation will not be prompted.
     */
    private void requestedInstall() {
        AREnginesApk.ARInstallStatus installStatus = AREnginesApk.requestInstall(this, isRemindInstall);
        switch(installStatus) {
            case INSTALL_REQUESTED:
                isRemindInstall = false;
                break;
            case INSTALLED:
                break;
        }
    }

    private void setMessageWhenError(Exception catchException) {
        if (catchException instanceof ARUnavailableServiceNotInstalledException) {
            message = "Please install HuaweiARService.apk";
        } else if (catchException instanceof ARUnavailableServiceApkTooOldException) {
            message = "Please update HuaweiARService.apk";
        } else if (catchException instanceof ARUnavailableClientSdkTooOldException) {
            message = "Please update this app";
        } else if (catchException instanceof ARUnavailableDeviceNotCompatibleException) {
            message = "This device does not support Huawei AR Engine ";
        } else if (catchException instanceof ARUnavailableEmuiNotCompatibleException) {
            message = "Please update EMUI version";
        } else if (catchException instanceof ARUnavailableUserDeclinedInstallationException) {
            message = "Please agree to install!";
        } else if (catchException instanceof ARUnSupportedConfigurationException) {
            message = "The configuration is not supported by the device!";
        } else {
            message = "exception throwed";
        }
    }

    /**
     * When some exceptions occur, the application needs to stop the session.
     *
     * @param exception Exception occurred
     */
    private void stopArSession(Exception exception) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Creating session error", exception);
        if (mArSession != null) {
            mArSession.stop();
            mArSession = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mArSession != null) {
            mDisplayRotationUtil.unregisterDisplayListener();
            mSurfaceView.onPause();
            mArSession.pause();
        }
    }

    @Override
    protected void onDestroy() {
        if (mArSession != null) {
            mArSession.stop();
            mArSession = null;
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean isHasFocus) {
        super.onWindowFocusChanged(isHasFocus);
        if (isHasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
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
                        try {
                            BitmapFactory.Options newOpts = new BitmapFactory.Options();
//                            newOpts.inJustDecodeBounds = true;//只读取bitmap的宽高信息
//                            newOpts.inJustDecodeBounds = false;
                            newOpts.inSampleSize = 16; //压缩率设置为3
                            mRenderUtil.setColorBitmap(BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri),null,newOpts));

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
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
}
