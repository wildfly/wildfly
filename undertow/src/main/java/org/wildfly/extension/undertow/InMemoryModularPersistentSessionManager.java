/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
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
