package com.mingbikes.acquisition;

import android.os.Build;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URLConnection;

import utils.HttpConnectionUtils;

/**
 *
 */
public class HttpConnectionProcessor implements Runnable {

    private String mServerURL;
    private AcquisitionStore mAcquisitionStore;

    public HttpConnectionProcessor(String serverUrl, AcquisitionStore acquisitionStore){

        mServerURL = serverUrl;
        mAcquisitionStore = acquisitionStore;

        // HTTP connection reuse which was buggy pre-froyo
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    @Override
    public void run() {

        while (true) {

            final String[] storedEvents = mAcquisitionStore.connections();
            if (storedEvents == null || storedEvents.length == 0) {
                // currently no data to send, we are done for now
                break;
            }

            //continue with sending the request to the server
            URLConnection conn = null;
            String eventData = null;
            try {
                // initialize and open connection
                conn = HttpConnectionUtils.urlConnectionForEventData(mServerURL, eventData);
                conn.connect();

                // response code has to be 2xx to be considered a success
                boolean success = true;
                final int responseCode;
                if (conn instanceof HttpURLConnection) {
                    final HttpURLConnection httpConn = (HttpURLConnection) conn;
                    responseCode = httpConn.getResponseCode();
                    success = responseCode >= 200 && responseCode < 300;
                    if (!success && Acquisition.sharedInstance().isLoggingEnabled()) {
                        Log.w(Acquisition.TAG, "HTTP error response code was " + responseCode + " from submitting event data: " + eventData);
                    }
                } else {
                    responseCode = 0;
                }

                // HTTP response code was ok, check response JSON contains {"result":"Success"}
                if (success) {
                    if (Acquisition.sharedInstance().isLoggingEnabled()) {
                        Log.d(Acquisition.TAG, "ok ->" + eventData);
                    }

                    // successfully submitted event data to Count.ly server, so remove
                    // this one from the stored events collection
                    mAcquisitionStore.removeConnection(storedEvents[0]);
                } else if (responseCode >= 400 && responseCode < 500) {
                    if (Acquisition.sharedInstance().isLoggingEnabled()) {
                        Log.d(Acquisition.TAG, "fail " + responseCode + " ->" + eventData);
                    }
                    mAcquisitionStore.removeConnection(storedEvents[0]);
                } else {
                    // warning was logged above, stop processing, let next tick take care of retrying
                    break;
                }
            } catch (Exception e) {
                if (Acquisition.sharedInstance().isLoggingEnabled()) {
                    Log.w(Acquisition.TAG, "Got exception while trying to submit event data: " + eventData, e);
                }
                // if exception occurred, stop processing, let next tick take care of retrying
                break;
            } finally {
                // free connection resources
                if (conn != null && conn instanceof HttpURLConnection) {
                    ((HttpURLConnection) conn).disconnect();
                }
            }
        }
    }
}
