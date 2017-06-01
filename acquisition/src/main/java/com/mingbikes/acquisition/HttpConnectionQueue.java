package com.mingbikes.acquisition;

import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 */
public class HttpConnectionQueue {

    private ExecutorService mExecutorService;

    private Context mContext;
    private String mServerURL;
    private AcquisitionStore mAcquisitionStore;

    private Future<?> mConnectionProcessorFuture_;

    /**
     * Report a crash with device data to the server.
     */
    void sendCrashReport(String error, boolean nonfatal) {
        checkInternalState();

        tick();
    }

    /**
     * Record the specified events and sends them to the server.
     */
    void recordEvents(final String events) {
        checkInternalState();

        tick();
    }

    void checkInternalState() {
        if (mContext == null) {
            throw new IllegalStateException("context has not been set");
        }
        if (mAcquisitionStore == null) {
            throw new IllegalStateException("acquisition store has not been set");
        }
        if (mServerURL == null) {
            throw new IllegalStateException("server URL is not valid");
        }
    }

    void tick() {
        if (!mAcquisitionStore.isEmptyConnections()
                && (mConnectionProcessorFuture_ == null || mConnectionProcessorFuture_.isDone())) {
            ensureExecutor();
            mConnectionProcessorFuture_ = mExecutorService.submit(
                    new HttpConnectionProcessor(mServerURL, mAcquisitionStore));
        }
    }

    void ensureExecutor() {
        if (mExecutorService == null) {
            mExecutorService = Executors.newSingleThreadExecutor();
        }
    }

}
