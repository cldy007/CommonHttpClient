package com.android.common.CommonHttpClient;

/**
 * Created by cl on 12/28/15.
 */
public interface ResponseDelivery {
    public void postResponse(Request<?> request , Response<?> response);
    public void postResponse(Request<?> request , Response<?> response , Runnable runnable);
    public void postError(Request<?> request , ErrorException error);
}
