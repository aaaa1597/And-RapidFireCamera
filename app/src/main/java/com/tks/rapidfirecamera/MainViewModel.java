package com.tks.rapidfirecamera;

import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Pair;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.util.Arrays;

public class MainViewModel extends ViewModel {
    /*******************
     * SharedPreferences
     * *****************/
    private SharedPreferences mSharedPref;
    public void setSharedPreferences(SharedPreferences sharedPref) {
        mSharedPref = sharedPref;
    }

    /***************
     * 画像保存場所
     * ************/
    private static String RAPIDFIRE = "/Rapidfie";
    private static String RELATIVEPATH = "Pictures" + RAPIDFIRE + "/";
    private String mSavePath = "";
    public String getSaveFullPath() {
        if(mSavePath.equals("")) {
            /* 保存場所 読込み */
            mSavePath = mSharedPref.getString(ConfigFragment.PREF_KEY_SAVEPATH, "");
            if(mSavePath.equals("")) {
                /* 外部保存先を取得する(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)配下しか対象にしない) */
                mSavePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + RAPIDFIRE;
                /* SharedPreferencesに保存 */
                SharedPreferences.Editor editor = mSharedPref.edit();
                editor.putString(ConfigFragment.PREF_KEY_SAVEPATH, mSavePath);
                editor.apply();
            }
            /* 外部保存先をmkdirs */
            File defalutsavefile = new File(mSavePath);
            // noinspection ResultOfMethodCallIgnored <- mkdirs()の戻り値無視傾向対応
            defalutsavefile.mkdirs();
        }
        return mSavePath;
    }

    public String getSaveRelativePath() {
        return RELATIVEPATH;
    }

    /***************
     * カメラId
     * ************/
    private String mCameraId;
    public void setCameraId(String cameraId) {
        mCameraId = cameraId;
    }
    public String getCameraId() {
        return mCameraId;
    }

    /***************
     * Flashサポート
     * ************/
    private boolean mFlashSupported;
    public void setFlashSupported(boolean flashSupported) {
        mFlashSupported = flashSupported;
    }
    public boolean getFlashSupported() {
        return mFlashSupported;
    }

    /*****************************************
     * Cameraデバイスがサポートする撮像サイズのリスト
     * **************************************/
    private Size[] mSupportedCameraSizes;
    public Size[] getSupportedCameraSizes() {
        return mSupportedCameraSizes;
    }
    public void setSupportedCameraSizes(Size[] supportedResolutionSizes) {
        mSupportedCameraSizes = supportedResolutionSizes;
    }

    /***************
     * 撮像サイズ
     * ************/
    private final MutableLiveData<Size> mTakePictureSize = new MutableLiveData<>();
    public Size getTakePictureSize() {
        Size retsize = mTakePictureSize.getValue();
        if(retsize == null || retsize.getWidth() == -1 || retsize.getHeight() == -1) {
            /* 撮像解像度の読込み */
            int w = mSharedPref.getInt(ConfigFragment.PREF_KEY_RESOLUTION_W, -1);
            int h = mSharedPref.getInt(ConfigFragment.PREF_KEY_RESOLUTION_H, -1);
            if(w == -1 || h == -1) {
                /* 撮像解像度を決定する */
                Size size = getSuitablePictureSize(1920, 1080);
                /* 撮像解像度をSharedPreferencesに保存 */
                SharedPreferences.Editor editor = mSharedPref.edit();
                editor.putInt(ConfigFragment.PREF_KEY_RESOLUTION_W, size.getWidth());
                editor.putInt(ConfigFragment.PREF_KEY_RESOLUTION_H, size.getHeight());
                editor.apply();
                w = size.getWidth();
                h = size.getHeight();
            }
            retsize = new Size(w, h);
            setTakePictureSize(retsize);
        }
        return retsize;
    }

    private Size getSuitablePictureSize(int w, int h) {
        int idx = Arrays.asList(mSupportedCameraSizes).indexOf(new Size(w,h));
        /* 見つかった場合は、指定Sizeを返却する */
        if(idx != -1)
            return mSupportedCameraSizes[idx];

        /* 見つからなかった場合は、指定Sizeと同一アスペクト比の直近の大きめサイズを返却 */
        Size retSameAspectSize = null;
        Size retDiffAspectSize = null;
        /* アスペクト比を求めるため、先に最大公約数を求める */
        int gcd = getGreatestCommonDivisor(w, h);
        /* ベースのアスペクト比算出 */
        Size baseAspect = new Size(w/gcd, h/gcd);
        for(Size lsize : mSupportedCameraSizes) {
            int lgcd = getGreatestCommonDivisor(lsize.getWidth(), lsize.getHeight());
            Size laspect = new Size(lsize.getWidth()/lgcd, lsize.getHeight()/lgcd);
            if( !baseAspect.equals(laspect)) {
                if(retDiffAspectSize == null) {
                    retDiffAspectSize = lsize;
                }
                /* 引数 < lsize < retDiffAspectSizeの時、最適サイズなので、戻り値に設定 */
                else if(lsize.getWidth()*lsize.getHeight() >= (w*h) &&
                        lsize.getWidth()*lsize.getHeight() < retDiffAspectSize.getWidth()*retDiffAspectSize.getWidth() ) {
                    retDiffAspectSize = lsize;
                }
            }
            else {
                if(retSameAspectSize == null) {
                    retSameAspectSize = lsize;
                }
                /* 引数 < lsize < retSameAspectSize、最適サイズなので、戻り値に設定 */
                else if(lsize.getWidth()*lsize.getHeight() >= (w*h) &&
                        lsize.getWidth()*lsize.getHeight() < retSameAspectSize.getWidth()*retSameAspectSize.getWidth() ) {
                    retSameAspectSize = lsize;
                }
            }
        }

        /* 同一アスペクト比を優先返却する */
        return (retSameAspectSize!=null) ? retSameAspectSize : retDiffAspectSize;
    }

    public void setTakePictureSize(Size resolutionSize) {
        mTakePictureSize.postValue(resolutionSize);
    }

    public MutableLiveData<Size> setOnChageTakePictureSizeListner() {
        return mTakePictureSize;
    }

    /***************
     * 回転発生
     * ************/
    private final MutableLiveData<Pair<Integer, Integer>> mOnChageRotation = new MutableLiveData<>();
    public MutableLiveData<Pair<Integer, Integer>> setOnChageRotationListner() {
        return mOnChageRotation;
    }

    /***************
     * スマホ向き
     * ************/
    public static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private int mOrientation = 0;
    public void setOrientation(int newOrientation) {
        if(mOrientation != newOrientation)
            mOnChageRotation.postValue(new Pair<>(mOrientation, newOrientation));
        mOrientation = newOrientation;
    }
    public int getOrientation() {
        return mOrientation;
    }

    /***********************
     * Cameraデバイスが持つ向き
     * ********************/
    int mSensorOrientation = 0;
    public void setSensorOrientation(int sensorOrientation) {
        mSensorOrientation = sensorOrientation;
    }
    public int getSensorOrientation() {
        return mSensorOrientation;
    }

    /***************
     * Utils
     * ************/
    /* アスペクト比を求めるための最大公約数を求める関数 */
    static public int getGreatestCommonDivisor(int aaa, int bbb) {
        int wk = -1;
        while(wk != 0) {
            wk = aaa % bbb;
            aaa = bbb;
            bbb = wk;
        }
        return aaa;
    };
}
