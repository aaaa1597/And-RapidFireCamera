package com.tks.rapidfirecamera;

import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Pair;
import android.util.Size;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.util.Arrays;

import kotlin.NotImplementedError;
import kotlin.jvm.functions.Function2;

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
    private String mSavePath = "";
    public String getSavePath() {
        if(mSavePath.equals("")) {
            /* 保存場所 読込み */
            mSavePath = mSharedPref.getString(ConfigFragment.PREF_KEY_SAVEPATH, "");
            if(mSavePath.equals("")) {
                /* 外部保存先を取得する(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)配下しか対象にしない) */
                mSavePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/Rapidfie";
                /* 外部保存先をmkdirs */
                File defalutsavefile = new File(mSavePath);
                // noinspection ResultOfMethodCallIgnored <- mkdirs()の戻り値無視傾向対応
                defalutsavefile.mkdirs();
                /* SharedPreferencesに保存 */
                SharedPreferences.Editor editor = mSharedPref.edit();
                editor.putString(ConfigFragment.PREF_KEY_SAVEPATH, defalutsavefile.getAbsolutePath());
                editor.apply();
            }
        }
        return mSavePath;
    }

    /***************
     * カメラId
     * ************/
    String mCameraId;
    public void setCameraId(String cameraId) {
        mCameraId = cameraId;
    }

    /*****************************************
     * Cameraデバイスがサポートする撮像サイズのリスト
     * **************************************/
    private Size[] mSupportedResolutionSizes;
    public Size[] getSupportedResolutionSizes() {
        return mSupportedResolutionSizes;
    }
    public void setSupportedResolutionSizes(Size[] supportedResolutionSizes) {
        mSupportedResolutionSizes = supportedResolutionSizes;
    }

    /***************
     * 撮像サイズ
     * ************/
    private final MutableLiveData<Size> mCurrentResolutionSize = new MutableLiveData<>();
    public Size getCurrentResolutionSize() {
        Size cursize = mCurrentResolutionSize.getValue();
        if(cursize == null || cursize.getWidth() == -1 || cursize.getHeight() == -1) {
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
            cursize = new Size(w, h);
            setCurrentResolutionSize(cursize);
        }
        return cursize;
    }

    private Size getSuitablePictureSize(int w, int h) {
        int idx = Arrays.asList(mSupportedResolutionSizes).indexOf(new Size(w,h));
        /* 見つかった場合は、指定Sizeを返却する */
        if(idx != -1)
            return mSupportedResolutionSizes[idx];

        /* 見つからなかった場合は、指定Sizeと同一アスペクト比の直近の大きめサイズを返却 */
        Size retSameAspectSize = null;
        Size retDiffAspectSize = null;
        /* アスペクト比を求めるため、先に最大公約数を求める */
        int gcd = getGreatestCommonDivisor(w, h);
        /* ベースのアスペクト比算出 */
        Size baseAspect = new Size(w/gcd, h/gcd);
        for(Size lsize : mSupportedResolutionSizes) {
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

    public void setCurrentResolutionSize(Size resolutionSize) {
        mCurrentResolutionSize.postValue(resolutionSize);
    }
    public MutableLiveData<Size> setOnChageCurrentResolutionSizeListner() {
        return mCurrentResolutionSize;
    }

    /***************
     * 回転発生
     * ************/
    private final MutableLiveData<Pair<Integer, Integer>> mRotation = new MutableLiveData<>();
    public void setRotation(Pair<Integer, Integer> rotfromto) {
        mRotation.postValue(rotfromto);
    }
    public MutableLiveData<Pair<Integer, Integer>> setOnChageRotationListner() {
        return mRotation;
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
