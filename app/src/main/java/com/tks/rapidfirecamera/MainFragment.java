package com.tks.rapidfirecamera;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.view.animation.RotateAnimation;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class MainFragment extends Fragment {
    private MainViewModel mViewModel;
//    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
//
//    static {
//        ORIENTATIONS.append(Surface.ROTATION_0, 90);
//        ORIENTATIONS.append(Surface.ROTATION_90, 0);
//        ORIENTATIONS.append(Surface.ROTATION_180, 270);
//        ORIENTATIONS.append(Surface.ROTATION_270, 180);
//    }

    private final Semaphore mCameraOpenCloseSemaphore = new Semaphore(1);

    private CameraCaptureSession mCaptureSession;

    /* 絞りを開ける=f値小, 光がたくさん, 被写界深度-浅, 背景がボケる。被写体を浮き立たせる効果がある。シャッター速度が速くなるため手ブレしにくくなる。 */
    /* 絞りを絞る　=f値大, 光が少し　　, 被写界深度-深, 画面全部にピントが合う。 */

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    /*******************************
     * MainFragment::onViewCreated()
     *******************************/
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTextureView = view.findViewById(R.id.tvw_preview);
        mViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        final FragmentActivity activity = getActivity();
        if(activity == null)
            throw new RuntimeException("Error occurred!! illigal state in this app. activity is null!!");

        /* カメラデバイスIDの確定と、そのCameraがサポートしている解像度リストを取得 */
        CameraManager manager = (CameraManager)activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                /* フロントカメラは対象外 */
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if(facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT)
                    continue;

                /* streamConfig mapが取れなければ対象外 */
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if(map == null)
                    continue;

                Boolean flashavailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);

                mViewModel.setCameraId(cameraId);
                mViewModel.setSupportedResolutionSizes(map.getOutputSizes(SurfaceTexture.class));
                mViewModel.setFlashSupported((flashavailable == null) ? false : flashavailable);
                break;
            }
        }
        catch(CameraAccessException e) {
            Log.d("aaaaa", e.toString());
            throw new RuntimeException("Error!! Camera is illigal state!!");
        }

        /* 画面回転イベントの設定 */
        mViewModel.setOnChageRotationListner().observe(getViewLifecycleOwner(), rotfromto -> {
            /* Config系ボタン */
            RotateAnimation rotanim_s = new RotateAnimation(rotfromto.first, rotfromto.second, view.findViewById(R.id.btn_setting).getPivotX(), view.findViewById(R.id.btn_setting).getPivotY());
            rotanim_s.setDuration(500);
            rotanim_s.setFillAfter(true);
            view.findViewById(R.id.btn_setting).startAnimation(rotanim_s);
            view.findViewById(R.id.btn_settingaaa).startAnimation(rotanim_s);
            /* シャッターボタン */
            RotateAnimation rotanim_l = new RotateAnimation(rotfromto.first, rotfromto.second, view.findViewById(R.id.btn_shutter).getPivotX(), view.findViewById(R.id.btn_shutter).getPivotY());
            rotanim_l.setDuration(500);
            rotanim_l.setFillAfter(true);
            view.findViewById(R.id.btn_shutter).startAnimation(rotanim_l);
        });

        /* 設定ボタンの再配置 */
        WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
        Insets insets = windowMetrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
        view.findViewById(R.id.ll_config).setTranslationY(insets.top + 1);

        /* 設定ボタン押下イベント生成 */
        view.findViewById(R.id.btn_setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.getSupportFragmentManager().beginTransaction().replace(R.id.container, ConfigFragment.newInstance()).commit();
            }
        });

        /* TextureViewのサイズ設定 */
        mTopLayoutListner = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                /* topのRelativeLayoutのサイズが確定したら、mTextureViewのサイズを確定できる */
                Size rltop = new Size(view.findViewById(R.id.rl_main_top).getWidth(), view.findViewById(R.id.rl_main_top).getHeight());
                Size picture = new Size(mViewModel.getCurrentResolutionSize().getHeight(), mViewModel.getCurrentResolutionSize().getWidth());   /* 縦画面(固定)なので逆にする */
                double factor = Math.min(((double)rltop.getWidth()) / picture.getWidth(), ((double)rltop.getHeight()) / picture.getHeight());
                /* TextureViewサイズ確定 */
                Size tetureview = new Size((int)(picture.getWidth() * factor), (int)(picture.getHeight() * factor));
                /* TextureViewサイズ設定 */
                mTextureView.setAspectRatio(tetureview.getWidth(), tetureview.getHeight());
                /* TextureViewサイズとプレビューサイズの拡縮を設定 */
                float scale = ((float)tetureview.getHeight())/*縦画面(固定)だからあえての横/縦にしている */ / mViewModel.getCurrentResolutionSize().getWidth();
                Matrix matrix = new Matrix();
                matrix.postScale(scale, scale, tetureview.getWidth() / ((float)2), tetureview.getHeight() / ((float)2));
                mTextureView.setTransform(matrix);
                /* リスナー解除(でないと無限Loopになる) */
                view.findViewById(R.id.rl_main_top).getViewTreeObserver().removeOnGlobalLayoutListener(mTopLayoutListner);
            }
        };
        view.findViewById(R.id.rl_main_top).getViewTreeObserver().addOnGlobalLayoutListener(mTopLayoutListner);
    }

    ViewTreeObserver.OnGlobalLayoutListener mTopLayoutListner;

    /*******************************************************************************************************
     * Handler初期化,TextureView初期化シーケンス
     * onResume() -> [onSurfaceTextureAvailable()] -> openCamera() CameraManager::openCamera() -> onOpened()
     *******************************************************************************************************/
    private AutoFitTextureView mTextureView;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private CameraDevice mCameraDevice;

    /* onResume() -> [onSurfaceTextureAvailable()] -> openCamera() CameraManager::openCamera() -> onOpened() */
    /* ↑ココ                                                                                                  */
    @Override
    public void onResume() {
        super.onResume();

        /* start Handler */
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        if(mTextureView.isAvailable()) {
            openCamera(mViewModel.getCameraId(), mTextureView.getWidth(), mTextureView.getHeight());
        }
        else {
            mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                    Size picturesize = mViewModel.getCurrentResolutionSize();
                    openCamera(mViewModel.getCameraId(), picturesize.getWidth(), picturesize.getHeight());
                }

                @Override public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {}
                @Override public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {}
                @Override public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                    return false;
                }
            });
        }
    }

     /* onResume() -> [onSurfaceTextureAvailable()] -> openCamera() -> CameraManager::openCamera() -> onOpened() */
     /*                                                ↑ ココ                                                     */
    private ImageReader mImageReader;
    static final SimpleDateFormat mSdf = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US);

    private void openCamera(String cameraid, int width, int height) {
        Log.d("aaaaa", String.format("aaaaa openCamera %d x %d", width, height));
        mSdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        /* 撮像サイズの設定 */
        mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), getActivity().getContentResolver(), mViewModel.getSaveUri()));
            }
        }, mBackgroundHandler);

        /* 撮像サイズの設定 */
        CameraManager manager = (CameraManager)getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            if(!mCameraOpenCloseSemaphore.tryAcquire(2500, TimeUnit.MILLISECONDS))
                throw new RuntimeException("Time out waiting to lock camera opening.");

            /* 権限チェック -> 権限なし時はアプリ終了!!(CameraManager::openCamera()をコールする前には必ず必要) */
            if(ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                new Throwable().printStackTrace();
                ErrorDialog.newInstance(getString(R.string.request_permission)).show(getChildFragmentManager(), "Error!!");
            }
            manager.openCamera(cameraid, mDeviceStateCallback, mBackgroundHandler);
        }
        catch(InterruptedException | CameraAccessException e) {
            /* 異常が発生したら、例外吐いて終了 */
            throw new RuntimeException(e);
        }
    }

    /***************************************************************************************************************************************************
     * Preview開始シーケンス
     * CameraDevice.StateCallback::onOpened() -> createCameraPreviewSession() -> StateCallback::onConfigured() -> CaptureCallback::onCaptureProgressed()
     ***************************************************************************************************************************************************/
    private final CameraDevice.StateCallback mDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            /* This method is called when the camera is opened.  We start camera preview here. */
            mCameraOpenCloseSemaphore.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseSemaphore.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseSemaphore.release();
            cameraDevice.close();
            mCameraDevice = null;
            throw new RuntimeException(String.format(java.util.Locale.US, "Error occurred!! CameraDevice.State errorcode=%x", error));
        }
    };

    private static class ImageSaver implements Runnable {
        private final Image mImage;
        private final ContentResolver mCr;
        private final Uri mSaveUri;
        ImageSaver(Image image, ContentResolver cr, Uri saveUri) {
            mImage  = image;
            mCr     = cr;
            mSaveUri= saveUri;
        }

        @Override
        public void run() {
            /* バイナリデータ取得 */
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            ContentValues contentvalues = new ContentValues();
            /* ファイル名 */
            contentvalues.put(MediaStore.Images.Media.DISPLAY_NAME, mSdf.format(new Date()));
            /* マイムの設定 */
            contentvalues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            /* 書込み時にメディア ファイルに排他的にアクセスする */
            contentvalues.put(MediaStore.Images.Media.IS_PENDING, 1);

            /* 保存領域確保 */
            Uri dstUri = mCr.insert(mSaveUri, contentvalues);

            /* 保存 */
            OutputStream outstream = null;
            try {
                outstream = mCr.openOutputStream(dstUri);
                outstream.write(bytes);
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            finally {
                mImage.close();
                contentvalues.clear();
                /*　排他的にアクセスの解除 */
                contentvalues.put(MediaStore.Images.Media.IS_PENDING, 0);
                mCr.update(dstUri, contentvalues, null, null);
                try {
                    if(outstream != null)
                        outstream.close();
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /* CameraDevice.StateCallback::onOpened() -> createCameraPreviewSession() -> StateCallback::onConfigured() -> CaptureCallback::onCaptureProgressed() */
    /*                                            ↑ココ                                                                                                   */
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequestforStartCameraPreview;
    private void createCameraPreviewSession() {
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        Log.d("aaaaa", String.format("aaaaa mTextureView %d x %d", mTextureView.getWidth(), mTextureView.getHeight()));
        /* デフォルトバッファのサイズに、カメラ街道度のサイズを設定。 */
        Size camerasize = mViewModel.getCurrentResolutionSize();
        Log.d("aaaaa", String.format("aaaaa mViewModel.getCurrentResolutionSize() %d x %d", camerasize.getWidth(), camerasize.getHeight()));
        texture.setDefaultBufferSize(camerasize.getWidth(), camerasize.getHeight());

        /* SurfaceTexture -> Surface */
        Surface surface = new Surface(texture);

        /* We set up a CaptureRequest.Builder with the output Surface. */
        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            /* 先にパラメータ生成 */
            SessionConfiguration sessionConfiguration = new SessionConfiguration(
                                            SessionConfiguration.SESSION_REGULAR,
                                            Arrays.asList(new OutputConfiguration(surface), new OutputConfiguration(mImageReader.getSurface())),
                                            Runnable::run,
                                            mCaptureSessionStateCallback);

            /* カメラプレビュー用 CameraCaptureSessionを開始 */
            mCameraDevice.createCaptureSession(sessionConfiguration);
        }
        catch(CameraAccessException e) {
            /* 異常が発生したら、例外吐いて終了 */
            throw new RuntimeException(e);
        }
    }

    /* CameraDevice.StateCallback::onOpened() -> createCameraPreviewSession() -> StateCallback::onConfigured() -> CaptureCallback::onCaptureProgressed() */
    /*                                                                           ↑ココ                                                                                                              */
    CameraCaptureSession.StateCallback mCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            /* The camera is already closed */
            if (mCameraDevice ==null) return;

            /* When the session is ready, we start displaying the preview. */
            mCaptureSession = session;
            try {
                /* Auto focus should be continuous for camera preview. */
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                /* Flash is automatically enabled when necessary. */
                if(mViewModel.getFlashSupported())
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                /* Finally, we start displaying the camera preview. */
                mPreviewRequestforStartCameraPreview = mPreviewRequestBuilder.build();
                mCaptureSession.setRepeatingRequest(mPreviewRequestforStartCameraPreview, mCaptureCallback, mBackgroundHandler);
            }
            catch (CameraAccessException e) {
                /* 異常が発生したら、例外吐いて終了 */
                throw new RuntimeException(e);
            }
        }

        @Override public void onConfigureFailed(@NonNull CameraCaptureSession session) { /* 異常が発生したら、例外吐いて終了 */ throw new RuntimeException(session.toString()); }
    };

    /***************************************************************************************************************************************************
     * Previewキャプチャ中シーケンス
     * CaptureCallback::onCaptureProgressed()
     **************************************************************************************************************************************************/
    private int mState = STATE_PREVIEW;
    private static final int STATE_PREVIEW                = 0;
    private static final int STATE_WAITING_LOCK           = 1;
    private static final int STATE_WAITING_PRECAPTURE     = 2;
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    private static final int STATE_PICTURE_TAKEN          = 4;
    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult result) {
            switch (mState) {
                // We have nothing to do when the camera preview is working normally.
                case STATE_PREVIEW: {
                    break;
                }
//                case STATE_WAITING_LOCK: {
//                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
//                    if (afState == null) {
//                        captureStillPicture();
//                    }
//                    // CONTROL_AE_STATE can be null on some devices
//                    else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
//                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
//                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
//                            mState = STATE_PICTURE_TAKEN;
//                            captureStillPicture();
//                        }
//                        else {
//                            runPrecaptureSequence();
//                        }
//                    }
//                    break;
//                }
//                // CONTROL_AE_STATE can be null on some devices
//                case STATE_WAITING_PRECAPTURE: {
//                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
//                    if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
//                        mState = STATE_WAITING_NON_PRECAPTURE;
//                    }
//                    break;
//                }
//                // CONTROL_AE_STATE can be null on some devices
//                case STATE_WAITING_NON_PRECAPTURE: {
//                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
//                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
//                        mState = STATE_PICTURE_TAKEN;
//                        captureStillPicture();
//                    }
//                    break;
//                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            process(result);
        }
    };

    /* CaptureCallback::onCaptureProgressed() -> */
    /*                                           ↑ココ                                  */

    /**********************************************************************
     * Handler終了,TextureView終了シーケンス
     **********************************************************************/
    @Override
    public void onPause() {
        super.onPause();

        /* stop Camera */
        closeCamera();

        /* stop Handler */
        mBackgroundThread.quitSafely();
        try {
            mBackgroundHandler.getLooper().getThread().join();
            mBackgroundHandler = null;
        }
        catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseSemaphore.acquire();
            if(mCaptureSession != null) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if(mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if(mImageReader != null) {
                mImageReader.close();
                mImageReader = null;
            }
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        }
        finally {
            mCameraOpenCloseSemaphore.release();
        }
    }

    /**************************************
     * Utils
     * ************************************/
    public static class ErrorDialog extends DialogFragment {
        private static final String ARG_MESSAGE = "message";
        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            final Activity activity = getActivity();
            if(activity == null) throw new RuntimeException("illegal state!! activity is null!!");
            android.os.Bundle bundle = getArguments();
            if(bundle == null) throw new RuntimeException("illegal state!! bundle is null!!");

            return new AlertDialog.Builder(activity)
                    .setMessage(bundle.getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }
    }
}