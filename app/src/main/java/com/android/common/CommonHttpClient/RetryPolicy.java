package com.android.common.CommonHttpClient;

/**
 * Created by cl on 12/25/15.
 */
public interface RetryPolicy {

    public int getCurrentTimeout();

    public int getCurrentRetryCount();

    public void retry(ErrorException error) throws ErrorException;
}
