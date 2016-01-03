package com.android.common.CommonHttpClient;

import android.os.SystemClock;

import java.util.concurrent.BlockingQueue;

/**
 * Created by cl on 12/28/15.
 */
public class NetworkDispatcher extends Thread {

    private final BlockingQueue<Request<?>> mQueue;
    private final Network mNetwork;
    private final Cache mCache;

    private final ResponseDelivery mDelivery;

    private volatile boolean mQuit  = false;

    public NetworkDispatcher(BlockingQueue<Request<?>> queue , Network network,Cache cache ,ResponseDelivery delivery){
        this.mQueue = queue;
        this.mNetwork = network;
        this.mCache = cache;
        this.mDelivery = delivery;
    }

    public void quit(){
        this.mQuit = false;
        interrupt();
    }
    //TODO traffic statistics


    @Override
    public void run() {
        while (true){
            long startTimeMs = SystemClock.elapsedRealtime();
            Request<?> request;
            try {
                request = mQueue.take();
            } catch (InterruptedException e) {
                if(mQuit){
                    return;
                }
                continue;
            }

            try {
                if(request.isCanceled()){
                    request.finish("network-discard-cancelled");
                    continue;
                }
                NetworkResponse networkResponse = mNetwork.performRequest(request);
                if(networkResponse.notModified && request.hasHadResponseDelivered()){
                    request.finish("not-modified");
                    continue;
                }

                Response<?> response = request.parseNetworkResponse(networkResponse);
                if(request.shouldCache() && response.cacheEntry != null){
                    mCache.put(request.getCacheKey() , response.cacheEntry);
                }
                request.markDelivered();
                mDelivery.postResponse(request,response);
            } catch (ErrorException e) {
                e.setNetworkTimeMs(SystemClock.elapsedRealtime() - startTimeMs);
                mDelivery.postError(request,request.parseNetworkError(e));
            } catch (Exception e){
                ErrorException errorException = new ErrorException(e);
                errorException.setNetworkTimeMs(SystemClock.elapsedRealtime() - startTimeMs);
                mDelivery.postError(request,errorException);
            }
        }
    }
}
