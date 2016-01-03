package com.android.common.CommonHttpClient;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;

/**
 * Created by cl on 12/30/15.
 */
public class JsonArrayRequest extends JsonRequest<JSONArray> {

    public JsonArrayRequest(int mMethod, String url, Response.Listener<JSONArray> listener, Response.ErrorListener errorListener, JSONArray requestBody) {
        super(mMethod, url, listener, errorListener, requestBody == null ? null : requestBody.toString());
    }

    public JsonArrayRequest(String url, Response.Listener<JSONArray> listener, Response.ErrorListener errorListener, JSONArray requestBody) {
        this(Method.DEPRECATED_GET_OR_POST , url,listener,errorListener,requestBody);
    }

    @Override
    protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data , HttpHeaderParser.parseCharset(response.headers,PROTOCOL_CHARSET));
            return Response.success(new JSONArray(jsonString),HttpHeaderParser.parseCacheHeader(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException e){
            return Response.error(new ParseError(e));
        }
    }
}
