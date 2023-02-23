package com.tks.rapidfirecamera;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import java.time.LocalDate;

public class AutoFitTextureView extends TextureView {

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    public AutoFitTextureView(Context context) {
        this(context, null);
    }
    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        Log.d("aaaaa", String.format("AutoFitTextureView::setAspectRatio() (%d, %d)", mRatioWidth, mRatioHeight));
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.d("aaaaa", String.format("AutoFitTextureView::onMeasure(%d, %d)", widthMeasureSpec, heightMeasureSpec));

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        Log.d("aaaaa", String.format("AutoFitTextureView::onMeasure() MeasureSpec-Size (%d, %d), (%d, %d)", width, height, mRatioWidth, mRatioHeight));
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            Log.d("aaaaa", String.format("AutoFitTextureView::onMeasure() Set MeasuredDimension1 (%d, %d)", width, height));
            setMeasuredDimension(width, height);
        }
        else {
            if (width < height * mRatioWidth / mRatioHeight) {
                Log.d("aaaaa", String.format("AutoFitTextureView::onMeasure() Set MeasuredDimension2 (%d, %d(=%d*%d/%d))", width, (width*mRatioHeight/mRatioWidth), width,mRatioHeight,mRatioWidth));
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            }
            else {
                Log.d("aaaaa", String.format("AutoFitTextureView::onMeasure() Set MeasuredDimension3 (%d(=%d*%d/%d), %d)", (height*mRatioWidth/mRatioHeight), height, mRatioWidth,mRatioHeight,height));
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }
}
