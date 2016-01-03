package com.android.common.CommonHttpClient;

/**
 * Created by cl on 12/25/15.
 */
public class ErrorException extends Exception {
    public final NetworkResponse networkResponse;
    private long networkTimeMs;

    public ErrorException(){
        networkResponse = null;
    }

    public ErrorException(NetworkResponse response){
        networkResponse = response;
    }

    public ErrorException(String exceptionMessage){
        super(exceptionMessage);
        networkResponse = null;
    }

    public ErrorException(String exceptionMessage , Throwable throwable){
        super(exceptionMessage,throwable);
        networkResponse = null;
    }

    public ErrorException(Throwable throwable){
        super(throwable);
        networkResponse = null;
    }

    void setNetworkTimeMs(long ms){
        this.networkTimeMs = ms;
    }

    public long getNetworkTimeMs(){
        return networkTimeMs;
    }
}
