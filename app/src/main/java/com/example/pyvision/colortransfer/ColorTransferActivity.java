package com.example.pyvision.colortransfer;

import android.graphics.ImageFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.ViewStub;

import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;

import com.example.pyvision.R;
import com.example.pyvision.cameraapi.cameraX.AbstractCameraXActivity;

/**
 * Created by zale.zhang on 2020/5/26.
 *
 * @author zale.zhang
 */
public class ColorTransferActivity extends AbstractCameraXActivity<ColorTransferActivity.ColorTransferResult> {


    public native int[] colorTransfer(int[] source,int source_w, int source_h,
                                      int[] target,int target_w,int target_h);

    private boolean mAnalyzeImageErrorState;

    static class ColorTransferResult{
        private final long analysisDuration;

        public ColorTransferResult(long analysisDuration) {
            this.analysisDuration = analysisDuration;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("Tag","hello");
    }

    int format = ImageFormat.YUV_420_888;

    @Override
    protected int getContentViewLayoutId() {
        return R.layout.activity_colortransfer;
    }

    @Override
    protected TextureView getCameraPreviewTextureView() {
        return ((ViewStub) findViewById(R.id.color_transfer_texture_view_stub))
                .inflate()
                .findViewById(R.id.image_classification_texture_view);
    }

    @Nullable
    @Override
    protected ColorTransferResult analyzeImage(ImageProxy image, int rotationDegrees) {
        if (mAnalyzeImageErrorState){
            finish();
            return null;
        }


        return null;
    }

    @Override
    protected void applyToUiAnalyzeImageResult(ColorTransferResult result) {

    }
}
