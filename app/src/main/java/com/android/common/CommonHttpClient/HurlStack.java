package com.android.common.CommonHttpClient;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by cl on 12/29/15.
 */
public class HurlStack implements HttpStack {
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    private final SSLSocketFactory mSslSocketFactory;

    public HurlStack(){
        this(null);
    }

    public HurlStack(SSLSocketFactory mSslSocketFactory) {
        this.mSslSocketFactory = mSslSocketFactory;
    }

    @Override
    public HttpResponse performRequest(Request<?> request, Map<String, String> headers) throws IOException {
        String url = request.getUrl();
        HashMap<String ,String > map = new HashMap<String, String>();
        map.putAll(request.getHeaders());
        map.putAll(headers);

        URL parseUrl = new URL(url);
        HttpURLConnection urlConnection = openConnection(parseUrl,request);
        for(Map.Entry<String ,String > entry : map.entrySet()){
            urlConnection.addRequestProperty(entry.getKey(),entry.getValue());
        }
        setConnectionParametersForRequest(urlConnection , request);
        ProtocolVersion version = new ProtocolVersion("HTTP",1,1);
        int responseCode = urlConnection.getResponseCode();
        if(responseCode == -1){
            throw new IOException("Could not retrieve response code from HttpUrlConnection");
        }

        StatusLine statusLine = new BasicStatusLine(version,responseCode,urlConnection.getResponseMessage());
        BasicHttpResponse response = new BasicHttpResponse(statusLine);
        if(hasResponseBody(request.getMethod(),statusLine.getStatusCode())){
            response.setEntity(entityFromConnection(urlConnection));
        }
        for(Map.Entry<String ,List<String>> header : urlConnection.getHeaderFields().entrySet()){
            Header h = new BasicHeader(header.getKey() , header.getValue().get(0));
            response.addHeader(h);
        }
        return response;
    }

    private static HttpEntity entityFromConnection(HttpURLConnection connection) {
        BasicHttpEntity entity = new BasicHttpEntity();
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException ioe) {
            inputStream = connection.getErrorStream();
        }
        entity.setContent(inputStream);
        entity.setContentLength(connection.getContentLength());
        entity.setContentEncoding(connection.getContentEncoding());
        entity.setContentType(connection.getContentType());
        return entity;
    }

    private HttpURLConnection openConnection(URL url , Request<?> request) throws IOException{
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setInstanceFollowRedirects(HttpURLConnection.getFollowRedirects());
        int timeoutMs = request.getTimeoutMs();
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        if("https".equals(url.getProtocol()) && mSslSocketFactory != null){
            ((HttpsURLConnection)connection).setSSLSocketFactory(mSslSocketFactory);
        }
        return connection;
    }

    private static void setConnectionParametersForRequest(HttpURLConnection connection , Request<?> request)throws IOException{
        switch (request.getMethod()){
            case Request.Method.DEPRECATED_GET_OR_POST:
                byte[] body = request.getBody();
                if(body != null){
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.addRequestProperty(HEADER_CONTENT_TYPE, request.getBodyContentType());
                    DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                    out.write(body);
                    out.close();
                }
                break;
            case Request.Method.GET:
                connection.setRequestMethod("GET");
                break;
            case Request.Method.DELETE:
                connection.setRequestMethod("DELETE");
                break;
            case Request.Method.POST:
                connection.setRequestMethod("POST");
                addBodyIfExists(connection, request);
                break;
            case Request.Method.PUT:
                connection.setRequestMethod("PUT");
                addBodyIfExists(connection, request);
                break;
            case Request.Method.HEAD:
                connection.setRequestMethod("HEAD");
                break;
            case Request.Method.OPTIONS:
                connection.setRequestMethod("OPTIONS");
                break;
            case Request.Method.TRACE:
                connection.setRequestMethod("TRACE");
                break;
            case Request.Method.PATCH:
                connection.setRequestMethod("PATCH");
                addBodyIfExists(connection, request);
                break;
            default:
                throw new IllegalStateException("Unknown method type.");

        }
    }
    private static void addBodyIfExists(HttpURLConnection connection , Request<?> request)throws IOException{
        byte[] body = request.getBody();
        if(body != null){
            connection.setDoOutput(true);
            connection.addRequestProperty(HEADER_CONTENT_TYPE, request.getBodyContentType());
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.write(body);
            out.close();
        }
    }
    private static boolean hasResponseBody(int requestMethod, int responseCode) {
        return requestMethod != Request.Method.HEAD
                && !(HttpStatus.SC_CONTINUE <= responseCode && responseCode < HttpStatus.SC_OK)
                && responseCode != HttpStatus.SC_NO_CONTENT
                && responseCode != HttpStatus.SC_NOT_MODIFIED;
    }
}
