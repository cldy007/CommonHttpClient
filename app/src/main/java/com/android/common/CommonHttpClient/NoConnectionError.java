package com.android.common.CommonHttpClient;

/**
 * Created by cl on 12/29/15.
 */
public class NoConnectionError extends NetworkError {
    public NoConnectionError() {
    }

    public NoConnectionError(Throwable throwable) {
        super(throwable);
    }
}
