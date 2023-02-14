package com.tks.rapidfirecamera;

import android.util.Size;

import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {
    private String mSavePath;
    private Size ResolutionSize;

    public void setSavePath(String savepath) {
        mSavePath = savepath;
    }
    public void setResolutionSize(Size resolutionSize) {
        ResolutionSize = resolutionSize;
    }

    public String getSavePath() {
        return mSavePath;
    }

    public Size getResolutionSize() {
        return ResolutionSize;
    }
}