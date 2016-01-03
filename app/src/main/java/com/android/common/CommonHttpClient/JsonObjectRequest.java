package com.android.common.CommonHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by cl on 12/29/15.
 */
public class JsonObjectRequest extends JsonRequest<JSONObject> {

    public JsonObjectRequest(String url, JSONObject body ,  Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        this(body == null ? Method.GET : Method.POST , url,body,listener,errorListener);
    }

    public JsonObjectRequest(int mMethod , String url , JSONObject body , Response.Listener<JSONObject> listener , Response.ErrorListener errorListener){
        super(mMethod ,url,listener,errorListener , body == null ? null : body.toString());
    }

    @Override
    protected void deliverResponse(JSONObject response) {
        super.deliverResponse(response);
    }

    @Override
    protected void deliverError(ErrorException error) {
        super.deliverError(error);
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data , HttpHeaderParser.parseCharset(response.headers,PROTOCOL_CHARSET));
            return Response.success(new JSONObject(jsonString),HttpHeaderParser.parseCacheHeader(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException e){
            return Response.error(new ParseError(e));
        }
    }

}
