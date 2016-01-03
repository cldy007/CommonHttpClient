package com.android.common.CommonHttpClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import org.json.JSONArray;

/**
 * Created by cl on 12/30/15.
 */
public class ImageRequest extends Request<Bitmap> {

    public static int DEFAULT_IMAGE_TIME_OUT = 1000;

    public static int DEFAULT_IMAGE_MAX_RETRY = 3;

    public static float DEFAULT_IMAGE_BACKOFF_MULT = 2f;

    private static final Object sLock = new Object();

    private Response.Listener<Bitmap> mListener = null;
    private Bitmap.Config mConfig;
    private int maxWidth ;
    private int maxHeight ;
    private ImageView.ScaleType mScaleType;

    public ImageRequest(String mUrl, Response.Listener<Bitmap> listener ,
                        Response.ErrorListener mErrorListener,
                        int maxWidth,
                        int maxHeight ) {
        this(mUrl,listener,mErrorListener ,maxWidth,maxHeight, Bitmap.Config.ARGB_8888, ImageView.ScaleType.CENTER_INSIDE);
    }

    public ImageRequest(String url, Response.Listener<Bitmap> listener , Response.ErrorListener errorListener ,
                        int maxWidth , int maxHeight , Bitmap.Config config , ImageView.ScaleType scaleType){
        super(Method.GET , url , errorListener);
        this.mListener = listener;
        this.maxHeight = maxHeight;
        this.maxWidth = maxWidth;
        this.mScaleType = scaleType;
        this.mConfig = config;
        setRetryPolicy(new DefaultRetryPolicy(DEFAULT_IMAGE_TIME_OUT, DEFAULT_IMAGE_MAX_RETRY, DEFAULT_IMAGE_BACKOFF_MULT));
    }

    @Override
    protected Response<Bitmap> parseNetworkResponse(NetworkResponse response) {
        synchronized (sLock){
            try {
                return doParse(response);
            } catch (OutOfMemoryError e) {
                return Response.error(new ParseError(e));
            }
        }
    }

    private Response<Bitmap> doParse(NetworkResponse response){
        byte[] data = response.data;
        BitmapFactory.Options decodeOption = new BitmapFactory.Options();
        Bitmap bitmap = null;
        if(maxWidth == 0 && maxHeight == 0){
            decodeOption.inPreferredConfig = mConfig;
            bitmap = BitmapFactory.decodeByteArray(data,0,data.length,decodeOption);
        } else {
            decodeOption.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data , 0 , data.length , decodeOption);
            int actualWidth = decodeOption.outWidth;
            int actualHeight = decodeOption.outHeight;

            int desiredWidth = getResizedDimension(maxWidth , maxHeight , actualWidth , actualHeight , mScaleType);
            int desiredHeight = getResizedDimension(maxHeight , maxWidth , actualHeight , actualWidth , mScaleType);

            decodeOption.inJustDecodeBounds = false;
            decodeOption.inSampleSize = findBestSampleSize(actualWidth,actualHeight,desiredWidth,desiredHeight);
            Bitmap tmp = BitmapFactory.decodeByteArray(data , 0 , data.length , decodeOption);

            if(tmp != null && (tmp.getWidth() > desiredWidth || tmp.getHeight() > desiredHeight)){
                bitmap = Bitmap.createScaledBitmap(tmp , desiredWidth , desiredHeight , true);
                tmp.recycle();
            } else {
                bitmap = tmp;
            }
        }

        if(bitmap == null){
            return  Response.error(new ParseError(response));
        } else {
            return Response.success(bitmap , HttpHeaderParser.parseCacheHeader(response));
        }
    }

    @Override
    protected void deliverResponse(Bitmap response) {
        mListener.onResponse(response);
    }

    @Override
    protected void deliverError(ErrorException error) {
        super.deliverError(error);
    }

    @Override
    public Priority getPriority() {
        return Priority.LOW;
    }

    private static int getResizedDimension(int maxPrimary , int maxSecondary , int actualPrimary , int actualSecondary ,ImageView.ScaleType scaleType){
        if(maxPrimary == 0 && maxSecondary == 0){
            return actualPrimary;
        }
        if(scaleType == ImageView.ScaleType.FIT_XY){
            if(maxPrimary == 0){
                return actualPrimary;
            }
            return maxPrimary;
        }
        if(maxPrimary == 0){
            double ratio = (double) maxSecondary / (double) actualSecondary;
            return (int) (actualPrimary * ratio);
        }

        if(maxSecondary == 0){
            return maxPrimary;
        }

        double ratio = (double)actualSecondary / (double)actualPrimary;
        int resized = maxPrimary;
        if(scaleType == ImageView.ScaleType.CENTER_CROP){
            if(resized * ratio < maxSecondary){
                resized = (int) (maxSecondary/ratio);
            }
            return resized;
        }
        if((resized * ratio) > maxSecondary){
            resized = (int) (maxSecondary / ratio);
        }

        return resized;
    }

    private static int findBestSampleSize(int actualWidth , int actualHeight , int desiredWidth , int desiredHeight){
        double w = (double) actualWidth/desiredWidth;
        double h = (double) actualHeight / desiredHeight;
        double ratio = Math.min(w , h);
        float n = 1.0f;
        while ((n * 2) <= ratio){
            n *= 2;
        }

        return (int) n ;
    }
}
