package com.android.common.CommonHttpClient;

/**
 * Created by cl on 12/29/15.
 */
public class NetworkError extends ErrorException {
    public NetworkError() {
    }

    public NetworkError(Throwable throwable) {
        super(throwable);
    }

    public NetworkError(NetworkResponse response) {
        super(response);
    }
}
