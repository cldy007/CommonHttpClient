package com.android.common.CommonHttpClient;

import org.apache.http.HttpResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Created by cl on 12/29/15.
 */
public interface HttpStack {
    public HttpResponse performRequest(Request<?> request , Map<String ,String > headers) throws IOException;
}
