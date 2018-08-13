package com.example.scphoto;

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
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import java.util.Arrays;

import static android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP;
import static android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW;

public class SCPhotoActivity extends AppCompatActivity {

    private SCSurfaceView scSurfaceView;
    private View btn;

    private HandlerThread cameraThread;
    private Handler cameraHandler;

    private CameraDevice mCameraDevice;
    private CameraCaptureSession mSession;

    private ImageReader mImageReader;

    private CameraCharacteristics bCharacter;
    private CameraCharacteristics fCharacter;
    private Size fPreSize;
    private Size bPreSize;
    private Size fCapSize;
    private Size bCapSize;

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };
    private CaptureRequest.Builder mPreBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scphoto);
        scSurfaceView = findViewById(R.id.surface);
        btn = findViewById(R.id.btn);
        btn.setOnTouchListener(new View.OnTouchListener() {
            long time = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        time = System.currentTimeMillis();
                        break;
                    case MotionEvent.ACTION_UP:

                        break;
                }
                return false;
            }
        });
        init();
    }

    private void init() {
        cameraThread = new HandlerThread("camera");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
    }

    private Surface mSurface;

    @Override
    protected void onResume() {
        super.onResume();
        scSurfaceView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mSurface = new Surface(surface);
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                mSurface = new Surface(surface);
                openCamera();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    @SuppressWarnings("missingPermission")
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            manager.openCamera("0", mStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        try {
            bCharacter = manager.getCameraCharacteristics("0");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        StreamConfigurationMap map = bCharacter.get(SCALER_STREAM_CONFIGURATION_MAP);
        Size[] previewSizes = map.getOutputSizes(SurfaceTexture.class);
        Size[] capSizes = map.getOutputSizes(ImageFormat.JPEG);

        choosePreSize(previewSizes);
        chooseCapSize(capSizes);
        initImageReader();
        configureTransform(scSurfaceView.getWidth(), scSurfaceView.getHeight());

        try {
            mPreBuilder = mCameraDevice.createCaptureRequest(TEMPLATE_PREVIEW);
            mPreBuilder.addTarget(mSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mSession = session;
                    CaptureRequest request = mPreBuilder.build();
                    try {
                        session.setRepeatingRequest(request, null, cameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == scSurfaceView || null == bPreSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, bPreSize.getHeight(), bPreSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / bPreSize.getHeight(),
                    (float) viewWidth / bPreSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        scSurfaceView.setTransform(matrix);
    }


    private void choosePreSize(Size[] allSizes) {
        int maxSize = 0;
        for (Size allSize : allSizes) {
            int cur = allSize.getHeight() * allSize.getWidth();
            if (cur >= maxSize) {
                maxSize = cur;
                bCapSize = allSize;
            }
        }
    }

    private void chooseCapSize(Size[] allSizes) {
        int maxSize = 0;
        for (Size allSize : allSizes) {
            int cur = allSize.getHeight() * allSize.getWidth();
            if (cur >= maxSize) {
                maxSize = cur;
                bCapSize = allSize;
            }
        }
    }

    private void initImageReader() {
        mImageReader = ImageReader.newInstance(bCapSize.getWidth(), bCapSize.getHeight(),
                ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {

            }
        }, null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (cameraThread.isAlive()) {
            cameraThread.quitSafely();
            cameraThread = null;
        }
    }
}
