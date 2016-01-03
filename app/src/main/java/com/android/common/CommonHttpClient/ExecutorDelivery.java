package com.android.common.CommonHttpClient;

import android.os.Handler;

import java.util.concurrent.Executor;

/**
 * Created by cl on 12/28/15.
 */
public class ExecutorDelivery implements ResponseDelivery {
    private final Executor mResponsePoster;

    public ExecutorDelivery(final Handler handler){
        mResponsePoster = new Executor() {
            @Override
            public void execute(Runnable command) {
                handler.post(command);
            }
        };
    }

    public ExecutorDelivery(Executor executor){
        mResponsePoster = executor;
    }
    @Override
    public void postResponse(Request<?> request, Response<?> response) {
        postResponse(request,response,null);
    }

    @Override
    public void postResponse(Request<?> request, Response<?> response, Runnable runnable) {
        request.markDelivered();
        mResponsePoster.execute(new ResponseDeliveryRunnable(request,response,runnable));
    }

    @Override
    public void postError(Request<?> request, ErrorException error) {
        Response<?> response = Response.error(error);
        mResponsePoster.execute(new ResponseDeliveryRunnable(request,response,null));
    }
    private class ResponseDeliveryRunnable implements Runnable{
        private final Request mRequest;
        private final Response mResponse;
        private final Runnable runnable;

        public ResponseDeliveryRunnable(Request mRequest, Response mResponse, Runnable runnable) {
            this.mRequest = mRequest;
            this.mResponse = mResponse;
            this.runnable = runnable;
        }

        @Override
        public void run() {
            if(mRequest.isCanceled()){
                mRequest.finish("cancel-at-delivery");
                return;
            }

            if(mResponse.isSuccess()){
                mRequest.deliverResponse(mResponse.result);
            } else {
                mRequest.deliverError(mResponse.error);
            }

            if(!mResponse.intermediate){
                mRequest.finish("done");
            }
            if(runnable != null){
                runnable.run();
            }
        }
    }
}
