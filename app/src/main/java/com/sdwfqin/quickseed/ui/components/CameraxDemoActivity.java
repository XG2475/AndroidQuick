package com.sdwfqin.quickseed.ui.components;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.blankj.utilcode.util.LogUtils;
import com.google.common.util.concurrent.ListenableFuture;
import com.sdwfqin.quicklib.base.BaseActivity;
import com.sdwfqin.quickseed.R;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * CameraxDemo
 * <p>
 *
 * @author 张钦
 * @date 2019-05-30
 */
public class CameraxDemoActivity extends BaseActivity implements CameraXConfig.Provider {

    @BindView(R.id.view_finder)
    PreviewView mViewFinder;
    @BindView(R.id.capture_button)
    ImageButton mCaptureButton;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture mImageCapture;
    private ImageAnalysis mImageAnalysis;
    private Executor executor;

    @Override
    protected int getLayout() {
        return R.layout.activity_camerax_demo;
    }

    @Override
    protected void initEventAndData() {

        mTopBar.setVisibility(View.GONE);

        initCamera();
        initImageAnalysis();
        initImageCapture();
    }

    private void initCamera() {

        executor = ContextCompat.getMainExecutor(this);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, executor);

    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        preview.setSurfaceProvider(mViewFinder.getPreviewSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, mImageCapture, mImageAnalysis, preview);

        // TODO 对焦
//        CameraControl cameraControl = camera.getCameraControl();
//        MeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(150, 150);
//        MeteringPoint point = factory.createPoint(50, 50);
//        FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
//                //.addPoint(point2, FocusMeteringAction.FLAG_AE) // could have many
//                // auto calling cancelFocusAndMetering in 5 seconds
//                .setAutoCancelDuration(5, TimeUnit.SECONDS)
//                .build();
//
//        ListenableFuture future = cameraControl.startFocusAndMetering(action);
//        future.addListener(() -> {
//            try {
//                FocusMeteringResult result = (FocusMeteringResult) future.get();
//                LogUtils.e(result);
//                // process the result
//            } catch (Exception e) {
//            }
//        }, executor);
    }

    /**
     * 图像分析
     */
    private void initImageAnalysis() {

        mImageAnalysis = new ImageAnalysis.Builder()
                // 分辨率
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        mImageAnalysis.setAnalyzer(executor, image -> {
            int rotationDegrees = image.getImageInfo().getRotationDegrees();
            LogUtils.e("Analysis#rotationDegrees", rotationDegrees);
        });
    }

    /**
     * 构建图像捕获用例
     */
    private void initImageCapture() {

        // 构建图像捕获用例
        mImageCapture = new ImageCapture.Builder()
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build();

        // 旋转监听
        OrientationEventListener orientationEventListener = new OrientationEventListener((Context) this) {
            @Override
            public void onOrientationChanged(int orientation) {
                int rotation;

                // Monitors orientation values to determine the target rotation value
                if (orientation >= 45 && orientation < 135) {
                    rotation = Surface.ROTATION_270;
                } else if (orientation >= 135 && orientation < 225) {
                    rotation = Surface.ROTATION_180;
                } else if (orientation >= 225 && orientation < 315) {
                    rotation = Surface.ROTATION_90;
                } else {
                    rotation = Surface.ROTATION_0;
                }

                mImageCapture.setTargetRotation(rotation);
            }
        };

        orientationEventListener.enable();
    }

    @NonNull
    @Override
    public CameraXConfig getCameraXConfig() {
        return Camera2Config.defaultConfig();
    }

    @OnClick(R.id.capture_button)
    public void onViewClicked() {
        File file = new File(getExternalMediaDirs()[0], System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(file).build();
        mImageCapture.takePicture(outputFileOptions, executor,
                new ImageCapture.OnImageSavedCallback() {

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        String msg = "图片保存成功: " + file.getAbsolutePath();
                        showMsg(msg);
                        LogUtils.d(msg);
                        Uri contentUri = Uri.fromFile(new File(file.getAbsolutePath()));
                        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
                        sendBroadcast(mediaScanIntent);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        String msg = "图片保存失败: " + exception.getMessage();
                        showMsg(msg);
                        LogUtils.e(msg);
                    }
                }
        );
    }
}
