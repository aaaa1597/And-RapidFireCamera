package com.tks.rapidfirecamera;

import android.util.Size;

import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {
    private String mSavePath;
    private Size mResolutionSize;
    String mCameraId;
    private Size[] mSupportedResolutionSizes;

    public void setSavePath(String savepath) {
        mSavePath = savepath;
    }
    public void setResolutionSize(Size resolutionSize) {
        mResolutionSize = resolutionSize;
    }

    public String getSavePath() {
        return mSavePath;
    }

    public Size getResolutionSize() {
        return mResolutionSize;
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