package com.example.pyvision.cameraapi.cameramix;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.pyvision.R;
import com.example.pyvision.common.customview.AutoFitTextureView;
import com.example.pyvision.utils.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by zale.zhang on 2020/5/29.
 *
 * @author zale.zhang
 */

@SuppressLint("ValidFragment")
public class CameraConnectionFragment extends Fragment {

    private static final Logger LOGGER = new Logger();

    /**
     * 这个camera preview size将会被选择到最小像素的帧，能够容纳DESIRED_SIZE x DESIRED_SIZE
     */
    private static final int MINIMUM_PREVIEW_SIZE = 320;

    // 将屏幕旋转转换到JPEG的旋转
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final String FRAGMENT_DIALOG = "dialog";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    // 在关掉camera之前防止app退出
    private final Semaphore cameraOpenCloseLock = new Semaphore(1);

    // 当帧到来时接受
    private final ImageReader.OnImageAvailableListener imageListener;;

    // Tensorflow需要的像素的输入大小
    private final Size inputSize;

    /** The layout identifier to inflate for this Fragment. */
    private final int layout;

    private final ConnectionCallback cameraConnectionCallback;
    private final CameraCaptureSession.CaptureCallback captureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureProgressed(
                        final CameraCaptureSession session,
                        final CaptureRequest request,
                        final CaptureResult partialResult) {}

                @Override
                public void onCaptureCompleted(
                        final CameraCaptureSession session,
                        final CaptureRequest request,
                        final TotalCaptureResult result) {}
            };

    // 当前摄像机设备的ID
    private String cameraId;
    // 摄像头预览view
    private AutoFitTextureView textureView;
    // 摄像头预览captureSession
    private CameraCaptureSession captureSession;
    // 已打开摄像头设备的引用，同Camera
    private CameraDevice cameraDevice;
    // 摄像头传感器显示的旋转角度
    private Integer sensorOrientation;
    // 摄像机预览的大小
    private Size previewSize;
    // 后台线程不堵塞UI
    private HandlerThread backgroundThread;
    // 后台运行的任务的Handler
    private Handler backgroundHandler;
    // 处理捕获的预览帧ImageReader
    private ImageReader previewReader;
    // 摄像头预览的CaptureRequest.Builder
    private CaptureRequest.Builder previewRequestBuilder;
    // 由previewRequestBuilder生成的CaptureRequest
    private CaptureRequest previewRequest;

    // 当CameraDevice改变状态时候的回调函数
    private final CameraDevice.StateCallback stateCallback =
            new CameraDevice.StateCallback() {
                @Override
                public void onOpened(final CameraDevice cd) {
                    // 当摄像头打开时调用的方法，在这里开启摄像头预览
                    cameraOpenCloseLock.release();
                    cameraDevice = cd;
                    createCameraPreviewSession();
                }

                @Override
                public void onDisconnected(final CameraDevice cd) {
                    cameraOpenCloseLock.release();
                    cd.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(final CameraDevice cd, final int error) {
                    cameraOpenCloseLock.release();
                    cd.close();
                    cameraDevice = null;
                    final Activity activity = getActivity();
                    if (null != activity) {
                        activity.finish();
                    }
                }
            };

    // TextureView.SurfaceTextureListener用来处理几个lifecycle的事件
    private final TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(
                        final SurfaceTexture texture, final int width, final int height) {
                    openCamera(width, height);
                }

                @Override
                public void onSurfaceTextureSizeChanged(
                        final SurfaceTexture texture, final int width, final int height) {
                    configureTransform(width, height);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(final SurfaceTexture texture) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(final SurfaceTexture texture) {}
            };

    public static CameraConnectionFragment newInstance(
            final ConnectionCallback callback,
            final ImageReader.OnImageAvailableListener imageListener,
            final int layout,
            final Size inputSize) {
        return new CameraConnectionFragment(callback, imageListener, layout, inputSize);
    }

    private CameraConnectionFragment(
            final ConnectionCallback connectionCallback,
            final ImageReader.OnImageAvailableListener imageListener,
            final int layout,
            final Size inputSize) {
        this.cameraConnectionCallback = connectionCallback;
        this.imageListener = imageListener;
        this.layout = layout;
        this.inputSize = inputSize;
    }



    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the minimum of both, or an exact match if possible.
     * 给定几个摄像头支持的Size，选择高度与宽度至少与两者的最小值相同的最小值，或者如果可能，选择完全匹配的最小值
     * @param choices The list of sizes that the camera supports for the intended output class
     * @param width The minimum desired width
     * @param height The minimum desired height
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    protected static Size chooseOptimalSize(final Size[] choices, final int width, final int height) {
        final int minSize = Math.max(Math.min(width, height), MINIMUM_PREVIEW_SIZE);
        final Size desiredSize = new Size(width, height);

        // Collect the supported resolutions that are at least as big as the preview Surface
        // 选择至少与预览Surface一样大的支持的分辨率
        boolean exactSizeFound = false;
        final List<Size> bigEnough = new ArrayList<Size>();
        final List<Size> tooSmall = new ArrayList<Size>();
        for (final Size option : choices) {
            if (option.equals(desiredSize)) {
                // Set the size but don't return yet so that remaining sizes will still be logged.
                // 设置size但是不返回这样其余的sizes将会被logged
                exactSizeFound = true;
            }

            if (option.getHeight() >= minSize && option.getWidth() >= minSize) {
                bigEnough.add(option);
            } else {
                tooSmall.add(option);
            }
        }

        LOGGER.i("Desired size: " + desiredSize + ", min size: " + minSize + "x" + minSize);
        LOGGER.i("Valid preview sizes: [" + TextUtils.join(", ", bigEnough) + "]");
        LOGGER.i("Rejected preview sizes: [" + TextUtils.join(", ", tooSmall) + "]");

        if (exactSizeFound) {
            LOGGER.i("Exact size match found.");
            return desiredSize;
        }

        // Pick the smallest of those, assuming we found any
        // 选择最小的一个，假设找到了某个
        if (bigEnough.size() > 0) {
            final Size chosenSize = Collections.min(bigEnough, new CompareSizesByArea());
            LOGGER.i("Chosen size: " + chosenSize.getWidth() + "x" + chosenSize.getHeight());
            return chosenSize;
        } else {
            LOGGER.e("Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Override
    public View onCreateView(
            final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(layout, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        textureView = (AutoFitTextureView) view.findViewById(R.id.texture);
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        // 当屏幕被关闭了然后再开启，这个SurfaceTexture已经可用了，并且onSurfaceTextureAvailable不会被调用，
        // 这样，我们可以打开一个camera并且开启预览（否则，我们等待直到surface在SurfaceTextureListener回调中准备好了）
        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    public void setCamera(String cameraId) {
        this.cameraId = cameraId;
    }

    // 设置camera的成员变量
    private void setUpCameraOutputs() {
        final Activity activity = getActivity();
        final CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            final StreamConfigurationMap map =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
            // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
            // garbage capture data.
            // 危险，！！尝试使用太大的预览大小可能会超出摄像机的bus'带宽限制，导致奇怪的预览垃圾的空间捕获数据
            previewSize =
                    chooseOptimalSize(
                            map.getOutputSizes(SurfaceTexture.class),
                            inputSize.getWidth(),
                            inputSize.getHeight());

            // 我们将TextureView的比例设置为我们选择的预览比例
            final int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
            } else {
                textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
            }
        } catch (final CameraAccessException e) {
            LOGGER.e(e, "Exception!");
        } catch (final NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            // 目前当Camera2API被使用但是在代码运行的设备上面不支持的话跑出空指针。
            ErrorDialog.newInstance(getString(R.string.tfe_od_camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            throw new IllegalStateException(getString(R.string.tfe_od_camera_error));
        }

        cameraConnectionCallback.onPreviewSizeChosen(previewSize, sensorOrientation);
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`. This method should be
     * called after the camera preview size is determined in setUpCameraOutputs and also the size of
     * `mTextureView` is fixed.
     * 配置必要的变换矩阵给mTextureView，这个方法将会在摄像头预览大小已经在setUpCameraOutputs确定和mTextureView的大小
     * 已经固定以后才会被调用
     * @param viewWidth The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(final int viewWidth, final int viewHeight) {
        final Activity activity = getActivity();
        if (null == textureView || null == previewSize || null == activity) {
            return;
        }
        final int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        final Matrix matrix = new Matrix();
        final RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        final RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        final float centerX = viewRect.centerX();
        final float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            final float scale =
                    Math.max(
                            (float) viewHeight / previewSize.getHeight(),
                            (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }


    // 打开由cameraId指定的摄像机
    @SuppressLint("MissingPermission")
    private void openCamera(final int width, final int height) {
        setUpCameraOutputs();
        configureTransform(width, height);
        final Activity activity = getActivity();
        final CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (final CameraAccessException e) {
            LOGGER.e(e, "Exception!");
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    // 关闭当前的CameraDevice
    private void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            if (null != captureSession) {
                captureSession.close();
                captureSession = null;
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != previewReader) {
                previewReader.close();
                previewReader = null;
            }
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            cameraOpenCloseLock.release();
        }
    }

    // 开启后台线程和它的Handler
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("ImageListener");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    // 关闭后台线程和它的Handler
    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }
    }

    // 对摄像机预览创建一个CameraCaptureSession
    private void createCameraPreviewSession() {
        try {
            final SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;

            // 我们将默认的buffer的大小配置为我们想要的camera preview的大小
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            // 这是我们需要预览的Surface的输出
            final Surface surface = new Surface(texture);

            // 我们队输出的Surface设置一个CaptureRequest.Builder
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface); // 这边add了一个surface,应该是输出到Textureview预览

            LOGGER.i("Opening camera preview: " + previewSize.getWidth() + "x" + previewSize.getHeight());

            // 对预览的帧创建reader
            previewReader =
                    ImageReader.newInstance(
                            previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 2);

            previewReader.setOnImageAvailableListener(imageListener, backgroundHandler);
            previewRequestBuilder.addTarget(previewReader.getSurface());    // 这边也add了一个surface??，应该是输出到ImageReader用于后处理

            // 这里，我们创建CameraCaptureSession来进行摄像头预览
            // 第一个参数，outputs:输出的Surface集合，每个 CaptureRequest 的输出 Surface 都应该是 outputs 的一个子元素。
            // 例如上例中使用了一个 mPreviewSurface 用于预览的输出，一个 mImageReader.getSurface() 用于拍照的输出。
            cameraDevice.createCaptureSession(
                    Arrays.asList(surface, previewReader.getSurface()),     // 将这两个变为List<Surface>
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(final CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == cameraDevice) {
                                return;
                            }

                            // 当session准备好以后，我们开始显示预览
                            captureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                previewRequestBuilder.set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                previewRequestBuilder.set(
                                        CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                // Finally, we start displaying the camera preview.
                                previewRequest = previewRequestBuilder.build();
                                captureSession.setRepeatingRequest(
                                        previewRequest, captureCallback, backgroundHandler);
                            } catch (final CameraAccessException e) {
                                LOGGER.e(e, "Exception!");
                            }
                        }

                        @Override
                        public void onConfigureFailed(final CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    },
                    null);
        } catch (final CameraAccessException e) {
            LOGGER.e(e, "Exception!");
        }
    }

    // 给Activity的回调这样当选择的preivew大小被知道了
    public interface ConnectionCallback {
        void onPreviewSizeChosen(Size size, int cameraRotation);
    }

    // 基于面积来比较两个大小
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(final Size lhs, final Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum(
                    (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /** Shows an error message dialog. */
    public static class ErrorDialog extends DialogFragment {
        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(final String message) {
            final ErrorDialog dialog = new ErrorDialog();
            final Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(
                            android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialogInterface, final int i) {
                                    activity.finish();
                                }
                            })
                    .create();
        }
    }

}
