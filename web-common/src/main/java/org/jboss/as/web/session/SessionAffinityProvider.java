/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.web.session;

/**
 * Determines the server for which a given session has affinity.
 * @author Paul Ferraro
 */
public interface SessionAffinityProvider {

    /**
     * Returns the identifier of the server for which the session with the specified identifier has affinity.
     * May return null, indicating that the session does not have affinity for any specific server.
     * @see {@link org.wildfly.undertow.Server#getId()}
     * @param sessionId a session identifier
     * @return the identifier of the server for which the session with the specified identifier has affinity, or null, if the specified session does not have affinity for any specific server.
     */
    String getAffinity(String sessionId);
}
