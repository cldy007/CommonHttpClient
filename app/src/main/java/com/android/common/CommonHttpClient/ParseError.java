package com.android.common.CommonHttpClient;

/**
 * Created by cl on 12/29/15.
 */
public class ParseError extends ErrorException {
    public ParseError() {
    }

    public ParseError(NetworkResponse response) {
        super(response);
    }

    public ParseError(Throwable throwable) {
        super(throwable);
    }
}
