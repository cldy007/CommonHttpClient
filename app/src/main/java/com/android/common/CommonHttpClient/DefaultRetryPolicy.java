package com.android.common.CommonHttpClient;

/**
 * Created by cl on 12/25/15.
 */
public class DefaultRetryPolicy implements RetryPolicy {

    private int mCurrentTimeoutMs;
    private int mCurrentRetryCount;
    private int mMaxNumRetries;
    private float mBackoffMultiplier;
    private static final int DEFAULT_TIMEOUT_MS = 2500;
    private static final int  DEFAULT_MAX_RETRIES = 1;
    private static final float DEFULT_BACKOFF_MULT = 1f;

    public DefaultRetryPolicy() {
        this(DEFAULT_TIMEOUT_MS,DEFAULT_MAX_RETRIES,DEFULT_BACKOFF_MULT);
    }

    public DefaultRetryPolicy(int mCurrentTimeoutMs, int mMaxNumRetries, float mBackoffMultiplier) {
        this.mCurrentTimeoutMs = mCurrentTimeoutMs;
        this.mMaxNumRetries = mMaxNumRetries;
        this.mBackoffMultiplier = mBackoffMultiplier;
    }

    @Override
    public int getCurrentTimeout() {
        return mCurrentTimeoutMs;
    }

    @Override
    public int getCurrentRetryCount() {
        return mCurrentRetryCount;
    }

    @Override
    public void retry(ErrorException error) throws ErrorException {
        mCurrentRetryCount++;
        mCurrentTimeoutMs += (mCurrentTimeoutMs * mBackoffMultiplier);
        if(mCurrentRetryCount > mMaxNumRetries){
            throw error;
        }
    }
}
