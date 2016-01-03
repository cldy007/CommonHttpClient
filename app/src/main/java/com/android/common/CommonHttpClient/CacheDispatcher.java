package com.android.common.CommonHttpClient;

import java.util.concurrent.BlockingQueue;

/**
 * Created by cl on 12/28/15.
 */
public class CacheDispatcher extends Thread {

    private final BlockingQueue<Request<?>> mCacheQueue;
    private final BlockingQueue<Request<?>> mNetworkQueue;
    private final Cache mCache;
    private final ResponseDelivery mDelivery;

    private volatile boolean mQuit = false;

    public CacheDispatcher(BlockingQueue<Request<?>> cacheQueue,
                           BlockingQueue<Request<?>> networkQueue,
                           Cache cache, ResponseDelivery delivery){
        this.mCacheQueue = cacheQueue;
        this.mNetworkQueue = networkQueue;
        this.mCache = cache;
        this.mDelivery = delivery;
    }

    public void quit(){
        mQuit = true;
        interrupt();
    }

    @Override
    public void run() {
        mCache.initialize();
        while (true){
            try {
                final Request<?> request = mCacheQueue.take();
                if(request.isCanceled()){
                    request.finish("cache-discard-canceled");
                    continue;
                }
                Cache.Entry entry = mCache.get(request.getCacheKey());
                if(entry == null){
                    mNetworkQueue.put(request);
                    continue;
                }

                if(entry.isExpired()){
                    request.setCacheEntry(entry);
                    mNetworkQueue.put(request);
                    continue;
                }

                Response<?> response = request.parseNetworkResponse(new NetworkResponse(entry.data,entry.responseHeaders));
                if(!entry.refreshNeeded()){
                    mDelivery.postResponse(request,response);
                } else {
                    request.setCacheEntry(entry);
                    response.intermediate = true; // ??
                    mDelivery.postResponse(request, response, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mNetworkQueue.put(request);
                            } catch (InterruptedException e) {
                            }
                        }
                    });
                }
            } catch (InterruptedException e) {
                if(mQuit)
                    return;
                continue;
            }
        }
    }
}
