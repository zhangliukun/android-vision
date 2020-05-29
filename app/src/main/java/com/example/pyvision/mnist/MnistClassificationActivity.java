package com.example.pyvision.mnist;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.TextureView;
import android.view.ViewStub;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;

import com.example.pyvision.R;
import com.example.pyvision.cameraapi.cameraX.AbstractCameraXActivity;
import com.example.pyvision.common.Constants;
import com.example.pyvision.common.Utils;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.Queue;

import static com.example.pyvision.ui.main.MainFragment.TORCHVISION_NORM_MEAN_MNIST;
import static com.example.pyvision.ui.main.MainFragment.TORCHVISION_NORM_STD_MNIST;

/**
 * Created by zale.zhang on 2020/5/19.
 *
 * @author zale.zhang
 */
public class MnistClassificationActivity extends AbstractCameraXActivity<MnistClassificationActivity.MnistResult> {


    private static final int INPUT_TENSOR_WIDTH = 28;
    private static final int INPUT_TENSOR_HEIGHT = 28;
    private static final int TOP_K = 3;
    private static final int MOVING_AVG_PERIOD = 10;
    private static final String FORMAT_MS = "%dms";
    private static final String FORMAT_AVG_MS = "avg:%.0fms";

    private static final String FORMAT_FPS = "%.1fFPS";
    public static final String SCORES_FORMAT = "%.2f";

    private boolean mAnalyzeImageErrorState;
    private TextView mFpsText;
    private TextView mMsText;
    private TextView mMsAvgText;
    private TextView mMaxIndexText;
    private Module mModule;
    private String mModuleAssetName;
    private FloatBuffer mInputTensorBuffer;
    private Tensor mInputTensor;
    private long mMovingAvgSum = 0;
    private Queue<Long> mMovingAvgQueue = new LinkedList<>();

    @Override
    protected int getContentViewLayoutId() {
        return R.layout.activity_mnist_classification;
    }

    @Override
    protected TextureView getCameraPreviewTextureView() {
        return ((ViewStub) findViewById(R.id.mnist_classification_texture_view_stub))
                .inflate()
                .findViewById(R.id.image_classification_texture_view);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMaxIndexText = findViewById(R.id.maxindex_textView);

    }

    @Nullable
    @Override
    protected MnistResult analyzeImage(ImageProxy image, int rotationDegrees) {
        if (mAnalyzeImageErrorState){
            return null;
        }
        try{
            if (mModule == null){
                mModule = Module.load(Utils.assetFilePath(this,"mnist_model.pt"));
                mInputTensorBuffer = Tensor.allocateFloatBuffer(3*INPUT_TENSOR_WIDTH*INPUT_TENSOR_HEIGHT);
                mInputTensor = Tensor.fromBlob(mInputTensorBuffer,new long[]{1,3,INPUT_TENSOR_HEIGHT,INPUT_TENSOR_WIDTH});

            }
            final long startTime = SystemClock.elapsedRealtime();
            TensorImageUtils.imageYUV420CenterCropToFloatBuffer(
                    image.getImage(),rotationDegrees,
                    INPUT_TENSOR_WIDTH,INPUT_TENSOR_HEIGHT,
                    TORCHVISION_NORM_MEAN_MNIST,
                    TORCHVISION_NORM_STD_MNIST,
                    mInputTensorBuffer, 0);

            final long moduleForwardStartTime = SystemClock.elapsedRealtime();
            final Tensor outputTensor = mModule.forward(IValue.from(mInputTensor)).toTensor();
            final long moduleForwardDuration = SystemClock.elapsedRealtime() - moduleForwardStartTime;

            final float[] scores = outputTensor.getDataAsFloatArray();
            int maxIndex = getMaxIndex(scores);

            final long analysisDuration = SystemClock.elapsedRealtime() - startTime;
            return new MnistResult(maxIndex, moduleForwardDuration, analysisDuration);
        }catch (Exception e){
            Log.e(Constants.TAG, "Error during image analysis", e);
            mAnalyzeImageErrorState = true;
            runOnUiThread(()->{
                if (!isFinishing()){
                    showErrorDialog(view -> MnistClassificationActivity.this.finish());
                }
            });
        }
        return null;
    }

    @Override
    protected void applyToUiAnalyzeImageResult(MnistResult result) {
        mMaxIndexText.setText(String.valueOf(result.index));
    }

    public static int getMaxIndex(float[] array){
        if(array == null || array.length == 0){
            return -1;
        }
        int maxIndex = 0;
//        int[] result = new int[2];
        for(int i=1;i<array.length;i++){
            if(array[maxIndex] < array[i]){
                maxIndex = i;
            }
        }
//        result[0] = (int) array[maxIndex];
//        result[1] = maxIndex;
        return maxIndex;

    }

    static class MnistResult{
        private final int  index;
        private final long analysisDuration;
        private final long moduleForwardDuration;

        public MnistResult(int index, long moduleForwardDuration, long analysisDuration) {
            this.index = index;
            this.moduleForwardDuration = moduleForwardDuration;
            this.analysisDuration = analysisDuration;
        }
    }

    public void test_mnist(){
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(getAssets().open("one.jpg"));
//            int[] pixels = new int[28*28];
//            bitmap.getPixels(pixels,0,28,0,0,28,28);

        } catch (IOException e) {
            e.printStackTrace();
        }

        Module module = Module.load(Utils.assetFilePath(MnistClassificationActivity.this,"mnist_model.pt"));
//        final float[] data = new float[784];
//        final long[] shape = new long[]{1,1,28,28};
//        final Tensor inputTensor = Tensor.fromBlob(data,shape);

        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                TORCHVISION_NORM_MEAN_MNIST,TORCHVISION_NORM_STD_MNIST);

        final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
        final float[] scores = outputTensor.getDataAsFloatArray();
        int maxIndex = getMaxIndex(scores);

        Log.i("scores",String.valueOf(maxIndex));
    }

}
