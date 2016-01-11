package com.android.common.CommonHttpClient;

import android.util.Log;

/**
 * Created by cl on 1/11/16.
 */
public class DebugLog {
    public static final String REQUEST_TAG= "REQUESTS";
    public static void log(String tag , String log){
        if(isDebug()){
            Log.d(tag, log);
        }
    }
    public static boolean isDebug(){
        return Log.isLoggable(REQUEST_TAG , Log.VERBOSE);
    }
}
