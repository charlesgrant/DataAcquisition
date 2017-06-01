package com.mingbikes.acquisition;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *
 */
public class Acquisition {

    public static final String DEFAULT_APP_VERSION = "1.0";
    /**
     * log tag
     */
    public static final String TAG = "mingbikes.acquisition";

    public boolean enableLogging_ = false;
    private HttpConnectionQueue _connectionQueue;

    // see http://stackoverflow.com/questions/7048198/thread-safe-singletons-in-java
    private static class SingletonHolder {
        static final Acquisition instance = new Acquisition();
    }

    /**
     * Returns the Acquisition singleton.
     */
    public static Acquisition sharedInstance() {
        return SingletonHolder.instance;
    }

    /**
     * Sets whether debug logging is turned on or off. Logging is disabled by default.
     * @param enableLogging true to enable logging, false to disable logging
     * @return Acquisition instance
     */
    public synchronized Acquisition setLoggingEnabled(final boolean enableLogging) {
        enableLogging_ = enableLogging;
        return this;
    }

    public synchronized boolean isLoggingEnabled() {
        return enableLogging_;
    }

    /**
     * Log handled exception to report it to server as non fatal crash
     * @param exception Exception to log
     */
    public synchronized Acquisition logException(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        _connectionQueue.sendCrashReport(sw.toString(), true);
        return this;
    }

    /**
     * Enable crash reporting to send unhandled crash reports to server
     */
    public synchronized Acquisition enableCrashReporting() {
        //get default handler
        final Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                Acquisition.sharedInstance()._connectionQueue.sendCrashReport(sw.toString(), false);

                //if there was another handler before
                if(oldHandler != null){
                    //notify it also
                    oldHandler.uncaughtException(t,e);
                }
            }
        };

        Thread.setDefaultUncaughtExceptionHandler(handler);
        return this;
    }
}
