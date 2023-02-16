package com.tks.rapidfirecamera;

import android.util.Pair;
import android.util.Size;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {
    /***************
     * 画像保存場所
     * ************/
    private String mSavePath;
    public void setSavePath(String savepath) {
        mSavePath = savepath;
    }
    public String getSavePath() {
        return mSavePath;
    }

    /***************
     * カメラId
     * ************/
    String mCameraId;
    public void setCameraId(String cameraId) {
        mCameraId = cameraId;
    }

    /***************
     * Cameraデバイスがサポートする撮像サイズのリスト
     * ************/
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
        return mCurrentResolutionSize.getValue();
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
}
