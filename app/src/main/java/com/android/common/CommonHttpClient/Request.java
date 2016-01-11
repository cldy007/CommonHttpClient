package com.android.common.CommonHttpClient;

import android.net.Uri;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;

/**
 * Created by cl on 12/25/15.
 */
public abstract class Request<T> implements Comparable<Request<T>> {

    private final int mMethod;
    private final String mUrl;
    private final int mDefaultTrafficStatsTag;
    private final Response.ErrorListener mErrorListener;
    private  Integer mSequence;
    private RequestQueue mRequestQueue;
    private boolean mShouldCache = true;
    private boolean mCanceled = false;
    private boolean mResponseDelivered = false;
    private RetryPolicy mRetryPolicy;

    private Cache.Entry mCacheEntry = null;
    private Object mTag;

    public Request(int mMethod, String mUrl, Response.ErrorListener mErrorListener) {
        this.mMethod = mMethod;
        this.mUrl = mUrl;
        this.mErrorListener = mErrorListener;
        setRetryPolicy(new DefaultRetryPolicy());
        mDefaultTrafficStatsTag = findDefaultTrafficStatsTag(mUrl);
    }

    public Request(int mMethod , String mUrl , Map<String ,String > params ,Response.ErrorListener mErrorListener ){
        this.mMethod = mMethod;
        this.mUrl = getFullUrl(mUrl , params);
        this.mErrorListener = mErrorListener;
        setRetryPolicy(new DefaultRetryPolicy());
        mDefaultTrafficStatsTag = findDefaultTrafficStatsTag(mUrl);
    }

    private static final String DEFAULT_PARAMS_ENCODING = "UTF-8";

    public interface Method {
        int DEPRECATED_GET_OR_POST = -1;
        int GET = 0;
        int POST = 1;
        int PUT = 2;
        int DELETE = 3;
        int HEAD = 4;
        int OPTIONS = 5;
        int TRACE = 6;
        int PATCH = 7;
    }

    public Request<?> setRetryPolicy(RetryPolicy retryPolicy) {
        mRetryPolicy = retryPolicy;
        return this;
    }
    public RetryPolicy getRetryPolicy(){
        return mRetryPolicy;
    }

    public int getTimeoutMs(){
        return mRetryPolicy.getCurrentTimeout();
    }

    public Request<?> setRequestQueue(RequestQueue queue){
        this.mRequestQueue = queue;
        return this;
    }

    public Request<?> setSequenceNum(int sequenceNum){
        this.mSequence = sequenceNum;
        return this;
    }

    public int getSequenceNum(){
        if(mSequence == null){
            throw new IllegalStateException("Try to get sequence num before set is not allowed");
        }
        return mSequence;
    }

    public int getMethod(){
        return mMethod;
    }

    public Request<?> setTag(Object tag){
        this.mTag = tag;
        return this;
    }

    public Object getTag(){
        return mTag;
    }

    public Response.ErrorListener getErrorListener() {
        return mErrorListener;
    }

    public int getTrafficStatsTag() {
        return mDefaultTrafficStatsTag;
    }

    public boolean isCanceled(){
        return mCanceled;
    }
    public void cancel(){
        mCanceled = true;
    }

    public boolean hasHadResponseDelivered(){
        return mResponseDelivered;
    }

    public void markDelivered(){
        mResponseDelivered = true;
    }

    public boolean shouldCache(){
        return mShouldCache;
    }

    public Request<?> setShouldCache(boolean shouldCache){
        mShouldCache = shouldCache;
        return this;
    }

    public String getUrl(){
        return mUrl;
    }

    public String getCacheKey(){
        return getUrl();
    }

    public void setCacheEntry(Cache.Entry entry){
        this.mCacheEntry = entry;
    }
    public Cache.Entry getCacheEntry(){
        return this.mCacheEntry;
    }

    public Map<String ,String > getHeaders() {
        return Collections.emptyMap();
    }

    protected Map<String ,String > getParams(){
        return null;
    }

    protected String getParamsEncoding(){
        return DEFAULT_PARAMS_ENCODING;
    }


    public String getBodyContentType(){
        return "application/x-www-form-urlencoded; charset=" + getParamsEncoding();
    }

    public byte[] getBody(){
        Map<String ,String > params = getParams();
        if(params != null && params.size() > 0){
            return encodeParameters(params,getParamsEncoding());
        }
        return null;
    }

    private byte[] encodeParameters(Map<String, String> params, String paramsEncoding) {
        StringBuilder encodedParams = new StringBuilder();

        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                encodedParams.append(URLEncoder.encode(entry.getKey(), paramsEncoding));
                encodedParams.append("=");
                encodedParams.append(URLEncoder.encode(entry.getValue(), paramsEncoding));
                encodedParams.append("&");
            }
            return encodedParams.toString().getBytes(paramsEncoding);

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding not supported" + paramsEncoding , e);
        }
    }

    void finish(String tag){
        if(mRequestQueue != null){
            mRequestQueue.finish(this);
        }
    }


    protected abstract Response<T> parseNetworkResponse(NetworkResponse response);

    // override this , return more specific errors
    protected ErrorException parseNetworkError(ErrorException e){
        return e;
    }

    protected abstract void deliverResponse(T response);

    protected void deliverError(ErrorException error){
        if(mErrorListener != null){
            mErrorListener.onErrorResponse(error);
        }
    }

    private static int findDefaultTrafficStatsTag(String url){
        if(!TextUtils.isEmpty(url)){
            Uri uri = Uri.parse(url);
            if(uri != null){
                String host = uri.getHost();
                if(host != null){
                    return host.hashCode();
                }
            }
        }
        return 0;
    }

    public enum Priority{
        LOW,
        NORMAL,
        HIGH,
        IMMEDIATE
    }

    public Priority getPriority(){
        return Priority.NORMAL;
    }

    public String getFullUrl(String url , Map<String ,String > params ){
        StringBuilder builder = new StringBuilder();
        builder.append(url);
        if(!url.contains("?")){
            builder.append("?");
        }
        int i = 0;
        if(params != null){
            for(String key : params.keySet()){
                String encodeValue = Uri.encode(params.get(key) , getParamsEncoding());
                if(i != 0){
                    builder.append("&");
                }
                builder.append(key).append("=").append(encodeValue);
                i++;
            }
        }
        String fullUrl = builder.toString();
        return fullUrl;
    }

    @Override
    public int compareTo(Request<T> another) {
        Priority left = this.getPriority();
        Priority right = another.getPriority();
        return left == right ? this.mSequence - another.mSequence : right.ordinal() - left.ordinal();
    }

}
