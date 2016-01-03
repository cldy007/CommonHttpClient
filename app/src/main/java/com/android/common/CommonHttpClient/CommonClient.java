package com.android.common.CommonHttpClient;

import android.content.Context;
import android.os.Build;

import java.io.File;

/**
 * Created by cl on 12/29/15.
 */
public class CommonClient {
    public static String DEFAULT_CACHE_DIR = "commonhttpclient";
    private static String mCacheDir = DEFAULT_CACHE_DIR;


    public static RequestQueue newRequestQueue(Context context , HttpStack httpStack){
        File cacheDir = new File(context.getCacheDir() , mCacheDir);

        if(httpStack == null){
            if(Build.VERSION.SDK_INT >= 9){
                httpStack = new HurlStack();
            } else {
                //TODO
            }
        }

        Network network = new BasicNetwork(httpStack);
        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir),network);
        queue.start();

        return queue;
    }

    public static RequestQueue newRequestQueue(Context context){
        return newRequestQueue(context,null);
    }
}
