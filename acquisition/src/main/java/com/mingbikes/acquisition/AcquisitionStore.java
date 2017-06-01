package com.mingbikes.acquisition;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Acquisition store
 */
public class AcquisitionStore {

    private static final String PREFERENCES = "ACQUISITION_STORE";
    private static final String DELIMITER = ":::";
    private static final String CONNECTIONS_PREFERENCE = "CONNECTIONS";
    private static final String EVENTS_PREFERENCE = "EVENTS";

    // limit max
    private static final int MAX_EVENTS = 100;
    private static final int MAX_REQUESTS = 1000;

    private final SharedPreferences mPreferences;

    /**
     * Constructs a AcquisitionStore object.
     * @param context used to retrieve storage meta data, must not be null.
     * @throws IllegalArgumentException if context is null
     */
    public AcquisitionStore(final Context context) {
        if (context == null) {
            throw new IllegalArgumentException("must provide valid context");
        }
        mPreferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    /**
     * Adds a event to the local store.
     * @param key name of the custom event, required, must not be the empty string
     */
    public synchronized void addEvent(final String key) {
        if(key == null || key.length() == 0) {
            return;
        }

        final Event event = new Event();
        event.key = key;

        addEvent(event);
    }

    /**
     * Adds a event to the local store.
     * @param key name of the custom event, required, must not be the empty string
     * @param segmentation segmentation values for the custom event, may be null
     */
    public synchronized void addEvent(final String key, final Map<String, String> segmentation) {
        if(key == null || key.length() == 0) {
            return;
        }

        final Event event = new Event();
        event.key = key;
        event.segmentation = segmentation;

        addEvent(event);
    }

    /**
     * Removes the specified events from the local store. Does nothing if the event collection
     * is null or empty.
     * @param eventsToRemove collection containing the events to remove from the local store
     */
    public synchronized void removeEvents(final Collection<Event> eventsToRemove) {
        if (eventsToRemove != null && eventsToRemove.size() > 0) {
            final List<Event> events = eventsList();
            if (events.removeAll(eventsToRemove)) {
                mPreferences.edit().putString(EVENTS_PREFERENCE, joinEvents(events, DELIMITER)).commit();
            }
        }
    }

    /**
     * Adds a event to the local store.
     * @param event event to be added to the local store, must not be null
     */
    void addEvent(final Event event) {
        final List<Event> events = eventsList();
        if (events.size() < MAX_EVENTS) {
            events.add(event);
            mPreferences.edit().putString(EVENTS_PREFERENCE, joinEvents(events, DELIMITER)).commit();
        }
    }

    /**
     * Returns an unsorted array of the current stored event JSON strings.
     */
    public String[] events() {
        final String joinedEventsStr = mPreferences.getString(EVENTS_PREFERENCE, "");
        return joinedEventsStr.length() == 0 ? new String[0] : joinedEventsStr.split(DELIMITER);
    }

    /**
     * Returns a list of the current stored events, sorted by timestamp from oldest to newest.
     */
    public List<Event> eventsList() {
        final String[] array = events();
        final List<Event> events = new ArrayList<>(array.length);
        for (String s : array) {
            try {
                final Event event = Event.fromJSON(new JSONObject(s));
                if (event != null) {
                    events.add(event);
                }
            } catch (JSONException ignored) {
                // should not happen since JSONObject is being constructed
                // events -> json objects -> json strings -> storage -> json strings -> here
            }
        }
        // order the events from oldest to newest
        Collections.sort(events, new Comparator<Event>() {
            @Override
            public int compare(final Event e1, final Event e2) {
                return e1.timestamp - e2.timestamp;
            }
        });
        return events;
    }

    /**
     * Converts each event JSON string delimited by the specified delimiter.
     * @param collection events to join into a delimited string
     * @param delimiter split the string
     */
    static String joinEvents(final Collection<Event> collection, final String delimiter) {
        final List<String> strings = new ArrayList<>();
        for (Event e : collection) {
            strings.add(e.toJSON().toString());
        }
        return join(strings, delimiter);
    }

    /**
     * Joins all the strings in the specified collection into a single string with the specified delimiter.
     */
    static String join(final Collection<String> collection, final String delimiter) {
        final StringBuilder builder = new StringBuilder();

        int count = collection.size();
        int i = 0;
        for (String s : collection) {
            builder.append(s);
            if (++i < count) {
                builder.append(delimiter);
            }
        }

        return builder.toString();
    }

    public boolean isEmptyConnections() {
        return mPreferences.getString(CONNECTIONS_PREFERENCE, "").length() == 0;
    }

    /**
     * Returns an unsorted array of the current stored connections.
     */
    public String[] connections() {
        final String joinedConnStr = mPreferences.getString(CONNECTIONS_PREFERENCE, "");
        return joinedConnStr.length() == 0 ? new String[0] : joinedConnStr.split(DELIMITER);
    }

    /**
     * Adds a connection to the local store.
     * @param str the connection to be added, ignored if null or empty
     */
    public synchronized void addConnection(final String str) {
        if (str != null && str.length() > 0) {
            final List<String> connections = new ArrayList<>(Arrays.asList(connections()));
            if (connections.size() < MAX_REQUESTS) {
                connections.add(str);
                mPreferences.edit().putString(CONNECTIONS_PREFERENCE, join(connections, DELIMITER)).commit();
            }
        }
    }

    /**
     * Removes a connection from the local store.
     * @param str the connection to be removed, ignored if null or empty,
     *            or if a matching connection cannot be found
     */
    public synchronized void removeConnection(final String str) {
        if (str != null && str.length() > 0) {
            final List<String> connections = new ArrayList<>(Arrays.asList(connections()));
            if (connections.remove(str)) {
                mPreferences.edit().putString(CONNECTIONS_PREFERENCE, join(connections, DELIMITER)).commit();
            }
        }
    }

    /**
     * Retrieve a preference from local store.
     * @param key the preference key
     */
    public synchronized String getPreference(final String key) {
        return mPreferences.getString(key, null);
    }

    /**
     * Add a preference to local store.
     * @param key the preference key
     * @param value the preference value, supply null value to remove preference
     */
    public synchronized void setPreference(final String key, final String value) {
        if (value == null) {
            mPreferences.edit().remove(key).commit();
        } else {
            mPreferences.edit().putString(key, value).commit();
        }
    }

    // for unit testing
    synchronized void clear() {
        final SharedPreferences.Editor prefsEditor = mPreferences.edit();
        prefsEditor.remove(EVENTS_PREFERENCE);
        prefsEditor.remove(CONNECTIONS_PREFERENCE);
        prefsEditor.clear();
        prefsEditor.commit();
    }
}
