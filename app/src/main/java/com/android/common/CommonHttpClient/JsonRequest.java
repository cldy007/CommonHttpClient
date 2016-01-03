package com.android.common.CommonHttpClient;

import java.io.UnsupportedEncodingException;

/**
 * Created by cl on 12/29/15.
 */
public abstract class JsonRequest<T> extends Request<T> {

    protected static final String PROTOCOL_CHARSET = "utf-8";
    private static final String PROTOCOL_CONTENT_TYPE =
            String.format("application/json;charset=%s" , PROTOCOL_CHARSET);

    private Response.Listener<T> mListener;
    private String mRequestBody = null;


    public JsonRequest(int mMethod ,String url, Response.Listener<T> listener , Response.ErrorListener errorListener , String requestBody){
        super(mMethod , url,errorListener);
        mListener = listener;
        mRequestBody = requestBody;
    }

    public JsonRequest(String url , Response.Listener<T> listener , Response.ErrorListener errorListener , String requestBody){
        this(Method.DEPRECATED_GET_OR_POST,url,listener,errorListener,requestBody);
    }

    @Override
    protected abstract Response<T> parseNetworkResponse(NetworkResponse response) ;

    @Override
    protected void deliverResponse(T response) {
        mListener.onResponse(response);
    }

    @Override
    protected void deliverError(ErrorException error) {
        super.deliverError(error);
    }

    @Override
    public String getBodyContentType() {
        return PROTOCOL_CONTENT_TYPE;
    }

    @Override
    public byte[] getBody() {
        try {
            return mRequestBody==null ? null : mRequestBody.getBytes(PROTOCOL_CHARSET);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
