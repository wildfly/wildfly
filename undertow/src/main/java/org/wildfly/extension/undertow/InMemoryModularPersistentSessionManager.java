package org.wildfly.extension.undertow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistent session manager that simply stores the session information in a map
 *
 * @author Stuart Douglas
 */
public class InMemoryModularPersistentSessionManager extends AbstractPersistentSessionManager {

    /**
     * The serialized sessions
     */
    private final Map<String, Map<String, SessionEntry>> sessionData = Collections.synchronizedMap(new HashMap<String, Map<String, SessionEntry>>());

    @Override
    protected void persistSerializedSessions(String deploymentName, Map<String, SessionEntry> serializedData) {
        sessionData.put(deploymentName, serializedData);
    }

    @Override
    protected Map<String, SessionEntry> loadSerializedSessions(String deploymentName) {
        return sessionData.remove(deploymentName);
    }
}
