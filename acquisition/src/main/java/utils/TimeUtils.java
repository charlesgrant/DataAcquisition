package utils;

/**
 *
 */
public class TimeUtils {

    /**
     * method to return a current timestamp (9 bit)
     */
    public static int currentTimestamp() {
        return ((int)(System.currentTimeMillis() / 1000l));
    }
}
