package com.android.common.CommonHttpClient;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by cl on 1/3/16.
 */
public class NetworkImageView extends ImageView {

    private ImageLoader.ImageContainer mImageContainer ;
    private String mUrl;



    private int mDefaultImageResId ;
    private int mErrorImageId;

    private ImageLoader mImageLoader;

    public NetworkImageView(Context context) {
        super(context);
    }

    public NetworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NetworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setmDefaultImageResId(int mDefaultImageResId) {
        this.mDefaultImageResId = mDefaultImageResId;
    }

    public void setmErrorImageId(int mErrorImageId) {
        this.mErrorImageId = mErrorImageId;
    }

    public void setImageUrl(String url , ImageLoader imageLoader){
        mUrl = url;
        mImageLoader = imageLoader;
        loadImageIfNecessary(false);
    }

    void loadImageIfNecessary(final boolean isInLayoutPass){
        int width = getWidth();
        int height = getHeight();

        ScaleType scaleType = getScaleType();

        boolean wrapWidth = false;
        boolean wrapHeight = false;

        if(getLayoutParams() != null){
            wrapWidth = getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT;
            wrapHeight = getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        boolean isFullWrapContent = wrapWidth && wrapHeight;
        if(width == 0 && height == 0 && !isFullWrapContent ) {
            return;
        }

        if(TextUtils.isEmpty(mUrl)){
            if(mImageContainer != null){
                mImageContainer.cancelRequest();
                mImageContainer = null;
            }
            setDefaultImageOrNull();
            return;
        }

        if(mImageContainer != null && mImageContainer.getRequestUrl() != null){
            if(mImageContainer.getRequestUrl().equals(mUrl)){
                return;
            } else {
                mImageContainer.cancelRequest();
                setDefaultImageOrNull();
            }
        }

        int maxWidth = wrapWidth ? 0 : width;
        int maxHeight = wrapHeight ? 0 : height;
        ImageLoader.ImageContainer newContainer = mImageLoader.get(mUrl, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(final ImageLoader.ImageContainer container, boolean isImmediate) {
                if(isImmediate && isInLayoutPass){
                    post(new Runnable() {
                        @Override
                        public void run() {
                            onResponse(container , false);
                        }
                    });
                    return;
                }
                if(container.getBitmap() != null){
                    setImageBitmap(container.getBitmap());
                } else if(mDefaultImageResId != 0){
                    setImageResource(mDefaultImageResId);
                }
            }

            @Override
            public void onErrorResponse(ErrorException error) {
                if(mErrorImageId != 0){
                    setImageResource(mErrorImageId);
                }
            }
        } , maxWidth , maxHeight , scaleType);

        mImageContainer = newContainer;
    }

    private void setDefaultImageOrNull(){
        if(mDefaultImageResId != 0){
            setImageResource(mDefaultImageResId);
        } else {
            setImageBitmap(null);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        loadImageIfNecessary(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        if(mImageContainer != null){
            mImageContainer.cancelRequest();
            setImageBitmap(null);
            mImageContainer = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }
}
