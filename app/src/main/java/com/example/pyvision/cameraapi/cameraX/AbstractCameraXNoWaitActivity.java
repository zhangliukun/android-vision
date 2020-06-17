package com.example.pyvision.cameraapi.cameraX;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;

import com.example.pyvision.common.BaseModuleActivity;
import com.example.pyvision.common.StatusBarUtils;

/**
 * Created by zale.zhang on 2020/5/16.
 *
 * @author zale.zhang
 */
public abstract class AbstractCameraXNoWaitActivity<R> extends BaseModuleActivity {
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 200;
    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA};

    protected abstract int getContentViewLayoutId();

    protected abstract TextureView getCameraPreviewTextureView();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtils.setStatusBarOverlay(getWindow(), true);
        setContentView(getContentViewLayoutId());

        startBackgroundThread();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS,
                    REQUEST_CODE_CAMERA_PERMISSION);
        } else {
            setupCameraX();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(
                        this,
                        "You can't use image classification example without granting CAMERA permission",
                        Toast.LENGTH_LONG)
                        .show();
                finish();
            } else {
                setupCameraX();
            }
        }
    }

    private void setupCameraX(){

        final TextureView textureView = getCameraPreviewTextureView();

        final PreviewConfig previewConfig = new PreviewConfig.Builder().setTargetRotation(Surface.ROTATION_180).build();
        final Preview preview = new Preview(previewConfig);
        //output: Preview.PreviewOutput
        preview.setOnPreviewOutputUpdateListener(output -> {
            textureView.setSurfaceTexture(output.getSurfaceTexture());
        });

        final ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setTargetResolution(new Size(720,1080))
                .setTargetRotation(Surface.ROTATION_90)
                .setCallbackHandler(mBackgroundHandler)
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .build();

        final ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        imageAnalysis.setAnalyzer((image, rotationDegrees) -> {

            final R result = analyzeImage(image,rotationDegrees);
            if (result != null){
                runOnUiThread(() -> {
                    applyToUiAnalyzeImageResult(result);
                });
            }
        });
        CameraX.bindToLifecycle(this,preview,imageAnalysis);

    }

    @WorkerThread
    @Nullable
    protected abstract R analyzeImage(ImageProxy image, int rotationDegrees);

    @UiThread
    protected abstract void applyToUiAnalyzeImageResult(R result);

}
