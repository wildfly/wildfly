/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow;

import io.undertow.servlet.api.SessionPersistenceManager;
import org.jboss.modules.ModuleLoader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Persistent session manager that simply stores the session information in a map
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class InMemoryModularPersistentSessionManager extends AbstractPersistentSessionManager {

    InMemoryModularPersistentSessionManager(final Consumer<SessionPersistenceManager> serviceConsumer,
                                            final Supplier<ModuleLoader> moduleLoader) {
        super(serviceConsumer, moduleLoader);
    }
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
