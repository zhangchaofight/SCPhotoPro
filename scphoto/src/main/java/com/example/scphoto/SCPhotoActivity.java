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
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP;
import static android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW;

public class SCPhotoActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;

    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    private SCSurfaceView scSurfaceView;
    private View btnRetry;
    private View btnConfirm;

    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private Handler mainHandler;

    private CameraDevice mCameraDevice;
    private CameraCaptureSession mSession;

    private ImageReader mImageReader;
    private Image latestImage;

    private CameraCharacteristics bCharacter;
//    private CameraCharacteristics fCharacter;
//    private Size fPreSize;
    private Size bPreSize;
//    private Size fCapSize;
    private Size bCapSize;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

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
    private MediaRecorder mMediaRecorder;
    private String mNextVideoAbsolutePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scphoto);
        scSurfaceView = findViewById(R.id.surface);
        SCControlView btnCap = findViewById(R.id.btn_cap);
        btnCap.setScControlViewTouchListener(new SCControlView.SCControlViewTouchListener() {
            @Override
            public void click() {
                googleCapture();
                Log.d("mediaRecorder", "click: ");
            }

            @Override
            public void startLongPress() {
                startVideo();
                Log.d("mediaRecorder", "startLongPress: " + System.currentTimeMillis());
            }

            @Override
            public void cancelLongPress() {
                stopVideo();
                Log.d("mediaRecorder", "cancelLongPress: " + System.currentTimeMillis());
            }
        });
        findViewById(R.id.btn_back).setOnClickListener(this);
        findViewById(R.id.btn_light_on).setOnClickListener(this);
        findViewById(R.id.btn_changeCam).setOnClickListener(this);
        btnRetry = findViewById(R.id.btn_recap);
        btnRetry.setOnClickListener(this);
        btnRetry.setVisibility(View.GONE);
        btnConfirm = findViewById(R.id.btn_confirm);
        btnConfirm.setOnClickListener(this);
        btnConfirm.setVisibility(View.GONE);
        init();
    }

    private void init() {
        cameraThread = new HandlerThread("camera");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
        mainHandler = new Handler(getMainLooper());
    }

    private Surface mSurface;

    private String getVideoPath() {
        mNextVideoAbsolutePath = getExternalFilesDir(null) + File.separator
                + System.currentTimeMillis() +  ".mp4";
        return mNextVideoAbsolutePath;
    }

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
        if (manager == null) {
            return;
        }
        try {
            manager.openCamera("0", mStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        try {
            bCharacter = manager.getCameraCharacteristics("0");
//            fCharacter = manager.getCameraCharacteristics("1");
            StreamConfigurationMap map = bCharacter.get(SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                return;
            }
            Size[] previewSizes = map.getOutputSizes(SurfaceTexture.class);
            Size[] capSizes = map.getOutputSizes(ImageFormat.JPEG);

            choosePreSize(previewSizes);
            chooseCapSize(capSizes);
            initImageReader();
            configureTransform(scSurfaceView.getWidth(), scSurfaceView.getHeight());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        btnRetry.setVisibility(View.GONE);
        btnConfirm.setVisibility(View.GONE);
        try {
            mPreBuilder = mCameraDevice.createCaptureRequest(TEMPLATE_PREVIEW);
            mPreBuilder.addTarget(mSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
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
                bPreSize = allSize;
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
                latestImage = reader.acquireLatestImage();
            }
        }, cameraHandler);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (cameraThread != null && cameraThread.isAlive()) {
            cameraThread.quitSafely();
            cameraThread = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    private void prepareVideo() {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(getVideoPath());
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(1080, 1920);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        switch (bCharacter.get(CameraCharacteristics.SENSOR_ORIENTATION)) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("mediaRecorder : ", "prepare");
        }
    }

    private void startVideo() {
        if (mSession != null) {
            mSession.close();
        }
        prepareVideo();
        try {
            final CaptureRequest.Builder builder = mCameraDevice
                    .createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

            List<Surface> surfaces = new ArrayList<>(2);

            Surface sur = mMediaRecorder.getSurface();
//            builder.addTarget(sur);

            SurfaceTexture surfaceTexture = scSurfaceView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(1080, 1920);
            Surface surface = new Surface(surfaceTexture);
            builder.addTarget(surface);

            surfaces.add(sur);
            surfaces.add(surface);

            mCameraDevice.createCaptureSession(surfaces,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mSession = session;
                            try {
                                mSession.setRepeatingRequest(builder.build(), null, cameraHandler);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mMediaRecorder.start();
                                        Log.d("mediaRecorder : ", "start");
                                    }
                                });
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                                Log.d("mediaRecorder : ", "CameraAccessException 0");
                            }
                        }
                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.d("mediaRecorder : ", "onConfigureFailed");
                        }
                    }, cameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.d("mediaRecorder : ", "CameraAccessException 1");
        }
    }

    private void stopVideo() {
        try {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void googleCapture() {
        try {
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            Surface irSurface = mImageReader.getSurface();
            captureBuilder.addTarget(irSurface);

            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            btnRetry.setVisibility(View.VISIBLE);
                            btnConfirm.setVisibility(View.VISIBLE);
                        }
                    });
                }
            };

            mSession.stopRepeating();
            mSession.abortCaptures();
            mSession.capture(captureBuilder.build(), CaptureCallback, cameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private int getOrientation(int rotation) {
        Integer orientation = bCharacter.get(CameraCharacteristics.SENSOR_ORIENTATION);
        int ori = 0;
        if (orientation != null) {
            ori = orientation;
        }
        return (ORIENTATIONS.get(rotation) + ori + 270) % 360;
    }

    private void clickChangeCam() {

    }

    private void clickLightOn() {

    }

    private void clickRetry() {
        startPreview();
    }

    private void clickBack() {
        finish();
    }

    private void clickConfirm() {
        File f = getExternalFilesDir(null);
        if (latestImage != null && f != null) {
            cameraHandler.post(new ImageSaver(latestImage,
                    new File(f.getAbsolutePath() + File.separator + System.currentTimeMillis() + ".jpg")));
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_back) {
            clickBack();
        } else if (v.getId() == R.id.btn_light_on) {
            clickLightOn();
        } else if (v.getId() == R.id.btn_changeCam) {
            clickChangeCam();
        } else if (v.getId() == R.id.btn_recap) {
            clickRetry();
        } else if (v.getId() == R.id.btn_confirm) {
            clickConfirm();
        }
    }
}
