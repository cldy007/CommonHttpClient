package com.android.common.CommonHttpClient;

/**
 * Created by cl on 12/29/15.
 */
public class ServerError extends ErrorException {
    public ServerError() {
    }

    public ServerError(NetworkResponse response) {
        super(response);
    }
}
