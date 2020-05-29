package com.example.pyvision.ui.main;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.pyvision.R;
import com.example.pyvision.cameraapi.cameraX.CameraXActivity;
import com.example.pyvision.cameraapi.cameramix.DectorActivity;
import com.example.pyvision.helloworld.HelloworldActivity;
import com.example.pyvision.mnist.MnistClassificationActivity;
import com.example.pyvision.vision.VisionListActivity;

import java.io.IOException;

public class MainFragment extends Fragment {

    static {
        System.loadLibrary("native-lib");
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    public native int[] grayJNI(int[] buf,int w, int h);

    Button helloDemo;
    Button pytorchDemo;
    Button mnistDemo;
    Button jniTest;
    Button colorTransDemo;

    ImageView IV_bg;

    public static float[] TORCHVISION_NORM_MEAN_MNIST = new float[]{0.1037f,0.1037f,0.1037f};
    public static float[] TORCHVISION_NORM_STD_MNIST = new float[]{0.3081f,0.3081f,0.3081f};

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View fragment_view = inflater.inflate(R.layout.main_fragment, container, false);
        helloDemo = fragment_view.findViewById(R.id.button);
        pytorchDemo = fragment_view.findViewById(R.id.button2);
        mnistDemo = fragment_view.findViewById(R.id.button3);
        jniTest = fragment_view.findViewById(R.id.button4);
        colorTransDemo = fragment_view.findViewById(R.id.color_transfer_btn);
        IV_bg = fragment_view.findViewById(R.id.imageview_bg);

        helloDemo.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(),HelloworldActivity.class);
            startActivity(intent);
        });

        pytorchDemo.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), VisionListActivity.class);
            startActivity(intent);
        });

        mnistDemo.setOnClickListener(view -> {
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    test_mnist();
//                }
//            }).start();
            Intent intent = new Intent(getActivity(), MnistClassificationActivity.class);
            startActivity(intent);
        });
        jniTest.setOnClickListener(view -> {
            Toast.makeText(getActivity(),stringFromJNI(),Toast.LENGTH_LONG).show();
//            try {
//                testGray();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            Intent intent = new Intent(getActivity(), CameraXActivity.class);
            startActivity(intent);
        });
        colorTransDemo.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), DectorActivity.class);
            startActivity(intent);
        });

        return fragment_view;
    }

    public void testGray() throws IOException {
        Bitmap bitmap = BitmapFactory.decodeStream(getActivity().getAssets().open("one.jpg"));
        int w = bitmap.getWidth(),h = bitmap.getHeight();
        int[] pix = new int[w * h];
        bitmap.getPixels(pix,0,w,0,0,w,h);
        //调用jni方法
        int[]resultPixes = grayJNI(pix,w,h);
        Bitmap result = Bitmap.createBitmap(w,h, Bitmap.Config.RGB_565);
        result.setPixels(resultPixes, 0, w, 0, 0,w, h);
        IV_bg.setVisibility(View.VISIBLE);
        IV_bg.setImageBitmap(result);
    }

}
