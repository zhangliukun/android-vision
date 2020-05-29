package com.example.pyvision.helloworld;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pyvision.R;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by zale.zhang on 2020/5/15.
 *
 * @author zale.zhang
 */
public class HelloworldActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_helloworld);

        Bitmap bitmap = null;
        Module module = null;
        try {
            // 读取图片
            bitmap = BitmapFactory.decodeStream(getAssets().open("image.jpg"));
            // 加载序列化的torchscript模块
            module = Module.load(assetFilePath(this,"model.pt"));
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("PytorchHelloWorld", "Error reading assets", e);
            finish();
        }

        ImageView imageView = findViewById(R.id.image);
        imageView.setImageBitmap(bitmap);

        // 准备输入tensor
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,TensorImageUtils.TORCHVISION_NORM_STD_RGB);

        // 运行模型
        IValue input = IValue.from(inputTensor);
        final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();

        // 将tensor数据类型转为java数组
        final float[] scores = outputTensor.getDataAsFloatArray();

        // 查找最大分数的索引
        float maxScore = -Float.MAX_VALUE;
        int maxScoreIdx = -1;
        for (int i =0;i < scores.length; i++){
            if (scores[i] > maxScore){
                maxScore = scores[i];
                maxScoreIdx = i;
            }
        }

        String className = ImageNetClasses.IMAGENET_CLASSES[maxScoreIdx];

        // showing className on UI
        TextView textView = findViewById(R.id.text);
        textView.setText(className);

    }

    public static String assetFilePath(Context context,String assetName) throws IOException {
        File file = new File(context.getFilesDir(),assetName);
        if (file.exists() && file.length() > 0){
            return file.getAbsolutePath();
        }
        try (InputStream is = context.getAssets().open(assetName)) {
            try(OutputStream os = new FileOutputStream(file)){
               byte[] buffer = new byte[4 * 1024];
               int read;
               while((read = is.read(buffer)) != -1){
                   os.write(buffer,0,read);
               }
               os.flush();
            }
            return file.getAbsolutePath();
        }
    }
}
