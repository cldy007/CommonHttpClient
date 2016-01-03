package com.android.common.CommonHttpClient;

/**
 * Created by cl on 12/28/15.
 */
public interface Network {
    public NetworkResponse performRequest(Request<?> request) throws ErrorException;
}
