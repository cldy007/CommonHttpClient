package com.android.common.CommonHttpClient;

import android.os.SystemClock;

import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.protocol.HTTP;

import java.util.Map;

/**
 * Created by cl on 12/29/15.
 */
public class HttpHeaderParser {
    public static Cache.Entry parseCacheHeader(NetworkResponse response){
        long now = SystemClock.elapsedRealtime();
        Map<String ,String > headers = response.headers;

        long serverDate = 0;
        long lastModified = 0;
        long serverExpires = 0;
        long softExpire = 0;
        long finalExpire = 0;
        long maxAge = 0;
        boolean hasCacheControl = false;
        boolean mustRevalidate = false;

        String serverEtag = null;
        String headerValue;

        headerValue = headers.get("Date");
        if(headerValue != null){
            serverDate = parseDateAsEpoch(headerValue);
        }

        headerValue = headers.get("Cache-Control");
        if(headerValue != null){
            hasCacheControl = true;
            String[] tokens = headerValue.split(",");
            for(int i = 0 ; i < tokens.length ; i ++){
                String token = tokens[i].trim();
                if(token.equals("no-cache") || token.equals("no-store")){
                    return null;
                } else if(token.startsWith("max-age=")) {
                    maxAge = Long.parseLong(token.substring(8));
                } else if(token.equals("must-revalidate") || token.equals("proxy-revalidate")){
                    mustRevalidate = true;
                }
            }
        }

        headerValue = headers.get("Expires");
        if(headerValue != null){
            serverExpires = parseDateAsEpoch(headerValue);
        }

        headerValue = headers.get("Last-Modified");
        if(headerValue != null){
            lastModified = parseDateAsEpoch(headerValue);
        }

        serverEtag = headers.get("Etag");

        if(hasCacheControl){
            softExpire = now + maxAge*1000;
            finalExpire = softExpire;
        } else if(serverDate > 0 && serverExpires >= serverDate){
            softExpire = now + (serverExpires - serverDate);
            finalExpire = softExpire;
        }

        Cache.Entry entry = new Cache.Entry();
        entry.data = response.data;
        entry.etag = serverEtag;
        entry.softTtl = softExpire;
        entry.ttl = finalExpire;
        entry.serverDate = serverDate;
        entry.lastModified = lastModified;
        entry.responseHeaders = headers;
        return entry;
    }
    public static long parseDateAsEpoch(String dateStr){
        try {
            return DateUtils.parseDate(dateStr).getTime();
        } catch (DateParseException e) {
            return 0;
        }
    }

    public static String parseCharset(Map<String ,String > headers , String defaultCharset){
        String contentType = headers.get(HTTP.CONTENT_TYPE);
        if(contentType != null){
            String[] params = contentType.split(";");
            for(int i = 0; i < params.length; i++){
                String[] pair = params[i].trim().split("=");
                if(pair.length ==2){
                    if(pair[0].equals("charset")){
                        return pair[1];
                    }
                }
            }
        }
        return defaultCharset;
    }

    public static String parseCharset(Map<String,String> headers){
        return parseCharset(headers , "UTF-8");
    }
}
