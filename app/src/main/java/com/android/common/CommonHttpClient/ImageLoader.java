package com.android.common.CommonHttpClient;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by cl on 12/30/15.
 * request must be issued on main thread
 * update UI must happens on main thread , immediate && isInLayoutPass(NetworkImageView) guarantee this
 */
public class ImageLoader {

    private RequestQueue mRequestQueue;
    private int mBatchResponseDelayMs = 100;
    private ImageCache mCache;

    private final HashMap<String , BatchedImageRequest> mInFlightRequests =
            new HashMap<String, BatchedImageRequest>();

    private final HashMap<String ,BatchedImageRequest> mBatchedResponses =
            new HashMap<String, BatchedImageRequest>();

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private Runnable mRunnable;

    public ImageLoader(RequestQueue queue , ImageCache cache){
        this.mRequestQueue = queue;
        this.mCache = cache;
    }

    public interface ImageCache {
        public Bitmap getBitmap(String url);
        public void putBitmap(String url , Bitmap bitmap);
    }

    public interface ImageListener extends Response.ErrorListener{
        public void onResponse(ImageContainer container , boolean isImmediate);
    }

    public static ImageListener getImageListener(final ImageView view , final int defaultImageResId , final int errorImageResId){
        return new ImageListener() {
            @Override
            public void onResponse(ImageContainer container, boolean isImmediate) {
                if(container.getBitmap() != null){
                    view.setImageBitmap(container.getBitmap());
                } else {
                    view.setImageResource(defaultImageResId);
                }
            }

            @Override
            public void onErrorResponse(ErrorException error) {
                if(errorImageResId != 0){
                    view.setImageResource(errorImageResId);
                }
            }
        };
    }

    public boolean isCached(String requestUrl, int maxWidth, int maxHeight) {
        return isCached(requestUrl, maxWidth, maxHeight, ImageView.ScaleType.CENTER_INSIDE);
    }

    public boolean isCached(String requestUrl, int maxWidth, int maxHeight, ImageView.ScaleType scaleType) {
        throwIfNotOnMainThread();

        String cacheKey = getCacheKey(requestUrl, maxWidth, maxHeight, scaleType);
        return mCache.getBitmap(cacheKey) != null;
    }

    public void setBatchedResponseDelay(int newBatchedResponseDelayMs) {
        mBatchResponseDelayMs = newBatchedResponseDelayMs;
    }

    public ImageContainer get(String url , ImageListener listener){
        return get(url , listener , 0 ,0);
    }

    public ImageContainer get(String url,ImageListener listener , int maxWidth , int maxHeight ){
        return get(url , listener , maxWidth , maxHeight , ImageView.ScaleType.CENTER_INSIDE);
    }

    public ImageContainer get(String url , ImageListener listener , int maxWidth , int maxHeight , ImageView.ScaleType scaleType){
        throwIfNotOnMainThread();
        String cacheKey = getCacheKey(url , maxWidth , maxHeight , scaleType);
        Bitmap cachedBimap =  mCache.getBitmap(cacheKey);
        if(cachedBimap != null){
            ImageContainer container = new ImageContainer(cachedBimap , url , null,null);
            listener.onResponse(container , true);
            return container;
        }

        ImageContainer imageContainer = new ImageContainer(null, url , cacheKey , listener);
        listener.onResponse(imageContainer , true);
        BatchedImageRequest request = mInFlightRequests.get(cacheKey);
        if(request != null){
            request.addContainer(imageContainer);
            return imageContainer;
        }

        Request<Bitmap> newRequest = makeImageRequest(url , maxWidth , maxHeight , scaleType, cacheKey);

        mRequestQueue.add(newRequest);
        mInFlightRequests.put(cacheKey , new BatchedImageRequest(newRequest , imageContainer));
        return imageContainer;

    }

    protected Request<Bitmap> makeImageRequest(String url , int maxWidth , int maxHeight , ImageView.ScaleType scaleType , final String cacheKey){
        return new ImageRequest(url, new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                onGetImageSuccess(cacheKey , response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(ErrorException error) {
                onGetImageError(cacheKey , error);
            }
        },maxWidth, maxHeight, Bitmap.Config.ARGB_8888, scaleType);
    }

    protected void onGetImageSuccess(String cacheKey , Bitmap response){
        mCache.putBitmap(cacheKey , response);
        BatchedImageRequest request = mInFlightRequests.remove(cacheKey);
        if(request != null){
            request.mResponseBitmap = response;
            batchResponse(cacheKey , request);
        }
    }

    protected void onGetImageError(String cacheKey , ErrorException e){
        BatchedImageRequest request = mInFlightRequests.remove(cacheKey);
        if(request != null){
            request.setError(e);
            batchResponse(cacheKey , request);
        }
    }

    private void batchResponse(String cacheKey , BatchedImageRequest request){
        mBatchedResponses.put(cacheKey , request);
        if(mRunnable == null){
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    for(BatchedImageRequest b : mBatchedResponses.values()){
                        for( ImageContainer container : b.mContainers){
                            if(container.mListener == null ){
                                continue;
                            }
                            if(b.getError() == null){
                                container.mBitmap = b.mResponseBitmap;
                                container.mListener.onResponse(container,false);
                            } else {
                                container.mListener.onErrorResponse(b.getError());
                            }
                        }
                    }
                    mBatchedResponses.clear();
                    mRunnable = null;
                }
            };
            mHandler.postDelayed(mRunnable , mBatchResponseDelayMs);
        }
    }

    public class ImageContainer{
        private Bitmap mBitmap;
        private String mRequestUrl;
        private String mCacheKey;
        private ImageListener mListener;

        public ImageContainer(Bitmap mBitmap, String mRequestUrl, String mCacheKey, ImageListener mListener) {
            this.mBitmap = mBitmap;
            this.mRequestUrl = mRequestUrl;
            this.mCacheKey = mCacheKey;
            this.mListener = mListener;
        }

        public void cancelRequest(){
            if(mListener == null){
                return;
            }
            BatchedImageRequest request = mInFlightRequests.get(mCacheKey);
            if(request != null){
                boolean canceled = request.removeContainerAndCancelRequestIfNecessary(this);
                if(canceled){
                    mInFlightRequests.remove(mCacheKey);
                }
            } else {
                request = mBatchedResponses.get(mCacheKey);
                if(request != null){
                    request.removeContainerAndCancelRequestIfNecessary(this);
                    if(request.mContainers.size() == 0){
                        mBatchedResponses.remove(mCacheKey);
                    }
                }
            }

        }

        public Bitmap getBitmap(){
            return mBitmap;
        }
        public String getRequestUrl(){
            return mRequestUrl;
        }
    }

    private class BatchedImageRequest{
        private Request<?> mRequest ;
        private Bitmap mResponseBitmap;
        private ErrorException mError;
        private LinkedList<ImageContainer> mContainers = new LinkedList<ImageContainer>();
        public BatchedImageRequest(Request<?> request ,
                                   ImageContainer container){
            mRequest = request;
            mContainers.add(container);
        }
        public void setError(ErrorException e){
            mError = e;
        }

        public ErrorException getError(){
            return mError;
        }
        public void addContainer(ImageContainer container){
            mContainers.add(container);
        }

        public boolean removeContainerAndCancelRequestIfNecessary(ImageContainer container){
            mContainers.remove(container);
            if(mContainers.size() == 0){
                mRequest.cancel();
                return true;
            }
            return false;
        }
    }

    private void throwIfNotOnMainThread(){
        if(Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("ImageLoader must be invoked from main thread");
        }
    }

    private static String getCacheKey(String url, int maxWidth , int maxHeight ,
                                      ImageView.ScaleType scaleType){
        return new StringBuilder(url.length() + 12).append("#W").append(maxWidth)
                .append("#H").append(maxHeight).append("#S").append(scaleType.ordinal())
                .append(url).toString();
    }

}
