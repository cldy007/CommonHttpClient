package com.android.common.CommonHttpClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.LruCache;

/**
 * Created by cl on 12/30/15.
 */
public class BitmapLruCache extends LruCache<String , Bitmap> implements ImageLoader.ImageCache {

    public BitmapLruCache(int maxSize) {
        super(maxSize);
    }

    public BitmapLruCache(Context context){
        this(getCacheSize(context));
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return value.getRowBytes() * value.getHeight();
    }

    @Override
    public Bitmap getBitmap(String url) {
        return get(url);
    }

    @Override
    public void putBitmap(String url, Bitmap bitmap) {
        put(url , bitmap);
    }

    public static int getCacheSize(Context context){
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        int screenBytes = screenHeight * screenWidth * 4;
        return screenBytes * 3;
    }
}
