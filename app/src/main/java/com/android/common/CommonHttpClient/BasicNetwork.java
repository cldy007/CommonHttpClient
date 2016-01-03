package com.android.common.CommonHttpClient;

import android.os.SystemClock;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.cookie.DateUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by cl on 12/29/15.
 */
public class BasicNetwork implements Network {
    private HttpStack mHttpStack = null;
    protected final ByteArrayPool mPool;
    private static int DEFAULT_POOL_SIZE = 4096;

    public BasicNetwork(HttpStack mHttpStack) {
        this(mHttpStack,new ByteArrayPool(DEFAULT_POOL_SIZE));
    }

    public BasicNetwork(HttpStack mHttpStack,ByteArrayPool mPool) {
        this.mPool = mPool;
        this.mHttpStack = mHttpStack;
    }

    @Override
    public NetworkResponse performRequest(Request<?> request) throws ErrorException {

        long requestStart = SystemClock.elapsedRealtime();
        while(true){
            HttpResponse response = null;
            byte[] responseContents = null;
            Map<String ,String > responseHeaders = null;
            try {
                Map<String ,String > header  = new HashMap<String, String>();
                addCacheHeaders(header,request.getCacheEntry());
                response = mHttpStack.performRequest(request,header);
                StatusLine status = response.getStatusLine();
                int statusCode = status.getStatusCode();
                responseHeaders = convertHeader(response.getAllHeaders());
                if(statusCode == HttpStatus.SC_NOT_MODIFIED){
                    Cache.Entry entry = request.getCacheEntry();
                    if(entry == null){
                        return new NetworkResponse(HttpStatus.SC_NOT_MODIFIED,
                                null , responseHeaders ,true,SystemClock.elapsedRealtime() - requestStart);
                    }
                    entry.responseHeaders.putAll(responseHeaders);
                    return new NetworkResponse(HttpStatus.SC_NOT_MODIFIED,
                            entry.data , entry.responseHeaders , true,SystemClock.elapsedRealtime() - requestStart);
                }
                if(response.getEntity() != null){
                    responseContents = entityToBytes(response.getEntity());
                } else {
                    responseContents = new byte[0];
                }
                long requestLifeTime = SystemClock.elapsedRealtime() - requestStart;
                logSlowRequests(requestLifeTime,request,responseContents,status);
                if(statusCode < 200 || statusCode > 299){
                    throw new IOException();
                }
                return new NetworkResponse(statusCode,responseContents,header,false,SystemClock.elapsedRealtime() - requestStart);
            } catch (SocketTimeoutException e){
                attemptRetryOnException("socket",request , new TimeoutException());
            } catch (ConnectTimeoutException e){
                attemptRetryOnException("connection" , request , new TimeoutException());
            } catch (MalformedURLException e){
                throw new RuntimeException("Bad Url" + request.getUrl(),e);
            } catch (IOException e) {
                int statusCode = 0;
                NetworkResponse networkResponse = null;
                if(response != null){
                    statusCode = response.getStatusLine().getStatusCode();
                } else {
                    throw new NoConnectionError(e);
                }
                if(responseContents != null){
                    networkResponse = new NetworkResponse(statusCode ,responseContents ,
                            responseHeaders , false, SystemClock.elapsedRealtime() - requestStart );
                    if(statusCode == HttpStatus.SC_UNAUTHORIZED || statusCode == HttpStatus.SC_FORBIDDEN){
                        attemptRetryOnException("auth",request , new ErrorException());
                    } else {
                        throw new ServerError(networkResponse);
                    }
                } else {
                    throw new NetworkError(networkResponse);
                }
            }

        }
    }

    private static void attemptRetryOnException(String logPrefix , Request<?> request , ErrorException error) throws ErrorException{
        RetryPolicy retryPolicy = request.getRetryPolicy();
        long timeout = retryPolicy.getCurrentTimeout();
        int count = retryPolicy.getCurrentRetryCount();
        try {
            retryPolicy.retry(error);
        } catch (ErrorException e) {
            throw e;
        }
    }

    private void addCacheHeaders(Map<String ,String > headers , Cache.Entry entry){
        if(entry == null){
            return;
        }

        if(entry.etag != null){
            headers.put("If-None-Match" , entry.etag);
        }

        if(entry.lastModified > 0){
            Date refTime = new Date(entry.lastModified);
            headers.put("If-Modified-Since", DateUtils.formatDate(refTime));
        }
    }

    protected static Map<String ,String > convertHeader(Header[] headers){
        Map<String , String > result = new TreeMap<String ,String >(String.CASE_INSENSITIVE_ORDER);
        for(int i = 0 ; i < headers.length ; i++){
            result.put(headers[i].getName() , headers[i].getValue());
        }
        return result;
    }

    private byte[] entityToBytes(HttpEntity entity) throws ErrorException , IOException{
        PoolingByteArrayOutputStream bytes = new PoolingByteArrayOutputStream(mPool , (int)entity.getContentLength());
        byte[] buffer = null;

        try {
            InputStream in = entity.getContent();
            if(in == null){
                throw new ErrorException();
            }
            buffer = mPool.getBuf(1024);
            int count ;
            while((count = in.read(buffer)) != -1){
                bytes.write(buffer , 0 , count);
            }
            return bytes.toByteArray();
        } finally {
            try {
                entity.consumeContent();
            } catch (IOException e) {
            }
            mPool.returnBuf(buffer);
            bytes.close();
        }
    }

    private static int SLOW_REQUEST_THRESHOLD_MS = 3000;

    private void logSlowRequests(long requestLifeTime , Request<?> request , byte[] responseContent,StatusLine statusLine){
        if(requestLifeTime > SLOW_REQUEST_THRESHOLD_MS){
            //TODO log
        }
    }
}
