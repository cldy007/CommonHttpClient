package com.android.common.CommonHttpClient;

import java.io.UnsupportedEncodingException;

/**
 * Created by cl on 12/29/15.
 */
public class StringRequest extends Request<String> {

    private Response.Listener<String> mListener;

    public StringRequest(int mMethod, String mUrl, Response.Listener<String> listener ,  Response.ErrorListener mErrorListener) {
        super(mMethod, mUrl, mErrorListener);
        mListener = listener;
    }
    public StringRequest(String url, Response.Listener<String> listener , Response.ErrorListener errorListener){
        this(Method.GET , url,listener,errorListener);
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String result;
        try {
            result = new String(response.data,HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            result = new String(response.data);
        }
        return Response.success(result,HttpHeaderParser.parseCacheHeader(response));
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }

    @Override
    protected void deliverError(ErrorException error) {
        super.deliverError(error);
    }
}
