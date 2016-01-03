package com.android.common.CommonHttpClient;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by cl on 12/25/15.
 */
public class RequestQueue {
    public static interface RequestFinishedListener<T> {
        public void onRequestFinished(Request<T> request);
    }

    private AtomicInteger mSequenceGenerator = new AtomicInteger();

    private final Map<String , Queue<Request<?>>> mWaitingRequests =  new HashMap<String, Queue<Request<?>>>();
    private final Set<Request<?>> mCurrentRequests = new HashSet<Request<?>>();
    private final PriorityBlockingQueue<Request<?>> mCacheQueue = new PriorityBlockingQueue<Request<?>>();
    private final PriorityBlockingQueue<Request<?>> mNetworkQueue = new PriorityBlockingQueue<Request<?>>();

    private static final int DEFAULT_NETWORK_THREAD_POOL_SIZE = 4;

    private final Cache mCache;
    private final Network network;
    private ResponseDelivery mDelivery;
    private NetworkDispatcher[] mDispatchers ;
    private CacheDispatcher mCacheDispatcher;

    private List<RequestFinishedListener> mFinishedListeners = new ArrayList<RequestFinishedListener>();

    public RequestQueue(Cache mCache, Network network, ResponseDelivery mDelivery,int threadPoolSize) {
        this.mCache = mCache;
        this.network = network;
        this.mDelivery = mDelivery;
        mDispatchers = new NetworkDispatcher[threadPoolSize];
    }

    public RequestQueue(Cache mCache, Network network , int threadPoolSize) {
        this(mCache , network , new ExecutorDelivery(new Handler(Looper.getMainLooper())) , threadPoolSize);
    }
    public RequestQueue(Cache mCache,Network network){
        this(mCache,network,DEFAULT_NETWORK_THREAD_POOL_SIZE);
    }
    public void start(){
        stop();

        mCacheDispatcher = new CacheDispatcher(mCacheQueue,mNetworkQueue , mCache,mDelivery);
        mCacheDispatcher.start();

        for(int i = 0; i < mDispatchers.length; i ++){
            mDispatchers[i] = new NetworkDispatcher(mNetworkQueue,network,mCache,mDelivery);
            mDispatchers[i].start();
        }
    }

    public void stop(){
        if(mCacheDispatcher != null){
            mCacheDispatcher.quit();
        }

        for(int i = 0 ; i < mDispatchers.length ; i ++){
            if(mDispatchers[i] != null){
                mDispatchers[i].quit();
            }
        }
    }

    public int getSequenceNumber(){
        return mSequenceGenerator.incrementAndGet();
    }

    public Cache getCache(){
        return mCache;
    }

    public <T>Request<T> add(Request<T> request){
        request.setRequestQueue(this);
        synchronized (mCurrentRequests){
            mCurrentRequests.add(request);
        }
        request.setSequenceNum(getSequenceNumber());
        if(!request.shouldCache()){
            mNetworkQueue.add(request);
            return request;
        }
        synchronized (mWaitingRequests){
            String cacheKey = request.getCacheKey();
            if(mWaitingRequests.containsKey(cacheKey)){
                Queue<Request<?>> stagedRequests = mWaitingRequests.get(cacheKey);
                if(stagedRequests == null){
                    stagedRequests = new LinkedList<Request<?>>();
                }
                stagedRequests.add(request);
                mWaitingRequests.put(cacheKey,stagedRequests);
            } else {
                mWaitingRequests.put(cacheKey,null);
                mCacheQueue.add(request);
            }
        }
        return request;
    }

    <T> void finish(Request<T> request){
        synchronized (mCurrentRequests){
            mCurrentRequests.remove(request);
        }
        synchronized (mFinishedListeners){
            for(RequestFinishedListener listener : mFinishedListeners){
                listener.onRequestFinished(request);
            }
        }
        if(request.shouldCache()){
            synchronized (mWaitingRequests){
                String cacheKey = request.getCacheKey();
                Queue<Request<?>> waitingRequests = mWaitingRequests.remove(cacheKey);
                if(waitingRequests != null){
                    mCacheQueue.addAll(waitingRequests);
                }
            }
        }
    }

    public interface RequestFilter {
        public boolean apply(Request<?> request);
    }

    public void cancelAll(final Object tag){
        if(tag == null){
            throw  new IllegalArgumentException("cancel tag null is not allowed");
        }
        cancelAll(new RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return tag == request.getTag();
            }
        });
    }

    public void cancelAll(RequestFilter filter){
        synchronized (mCurrentRequests){
            for(Request<?> request : mCurrentRequests){
                if(filter.apply(request)){
                    request.cancel();
                }
            }
        }
    }

    public <T> void addRequestFinishedListener(RequestFinishedListener<?> listener){
        synchronized (mFinishedListeners){
            mFinishedListeners.add(listener);
        }
    }

    public <T> void removeRequestFinishedListener(RequestFinishedListener<?> listener){
        synchronized (mFinishedListeners){
            mFinishedListeners.remove(listener);
        }
    }

}
