package com.tks.rapidfirecamera;

import android.util.Size;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {
    private String mSavePath;
    private final MutableLiveData<Size> mCurrentResolutionSize = new MutableLiveData<>();
    public Size getCurrentResolutionSize() {
        return mCurrentResolutionSize.getValue();
    }
    public void setCurrentResolutionSize(Size resolutionSize) {
        mCurrentResolutionSize.postValue(resolutionSize);
    }
    public MutableLiveData<Size> onChageCurrentResolutionSize() {
        return mCurrentResolutionSize;
    }

    String mCameraId;
    private Size[] mSupportedResolutionSizes;

    public void setSavePath(String savepath) {
        mSavePath = savepath;
    }

    public String getSavePath() {
        return mSavePath;
    }

    public Size[] getSupportedResolutionSizes() {
        return mSupportedResolutionSizes;

    }

    public void setSupportedResolutionSizes(Size[] supportedResolutionSizes) {
        mSupportedResolutionSizes = supportedResolutionSizes;
    }

    public void setCameraId(String cameraId) {
        mCameraId = cameraId;
    }
}