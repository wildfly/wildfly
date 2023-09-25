/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ejb.Singleton;

import org.jboss.logging.Logger;

/**
 * Singleton class to gather information on processing from the webservices.
 * This log is called from test class to check the actions which were done.
 */
@Singleton
public class EventLog implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(EventLog.class);

    // Event logs for a name
    private volatile Map<String, List<EventLogEvent>> eventLog = new HashMap<String, List<EventLogEvent>>();

    private static final String GENERAL_EVENTLOG_NAME = "general";

    /**
     * Method checks whether the eventLogName exists in the datastore.
     * In case that it exists - do nothing.
     * In case that it does not exists - it creates the key with empty list of logged events.
     *
     * @param eventLogName name of key for events
     */
    public void foundEventLogName(String eventLogName) {
        getListToModify(eventLogName, eventLog);
    }

    public void addEvent(String eventLogName, EventLogEvent event) {
        log.debug("Adding event " + event + " to logger " + this);
        getListToModify(eventLogName, eventLog).add(event);
    }

    public List<EventLogEvent> getEventLog(String eventLogName) {
        eventLogName = eventLogName == null ? GENERAL_EVENTLOG_NAME : eventLogName;
        return eventLog.get(eventLogName);
    }

    public void clear() {
        eventLog.clear();
    }

    public static String asString(List<EventLogEvent> events) {
        String result = "";

        for (EventLogEvent logEvent : events) {
            result += logEvent.name() + ",";
        }
        return result;
    }

    // --- helper method
    private <T> List<T> getListToModify(String eventLogName, Map<String, List<T>> map) {
        eventLogName = eventLogName == null ? GENERAL_EVENTLOG_NAME : eventLogName;

        if (map.containsKey(eventLogName)) {
            return map.get(eventLogName);
        } else {
            List<T> newList = new ArrayList<T>();
            map.put(eventLogName, newList);
            return newList;
        }
    }
}
