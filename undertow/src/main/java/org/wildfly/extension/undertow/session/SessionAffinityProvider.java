/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.session;

import java.util.Optional;

/**
 * Determines the server for which a given session has affinity.
 * TODO Relocate this to Undertow SPI module
 * @author Radoslav Husar
 */
public interface SessionAffinityProvider {

    /**
     * When present, returns the identifier of the server for which the session with the specified identifier has affinity.
     * When absent, the session does not have affinity for any specific server.
     * @see {@link org.wildfly.extension.undertow.Server#getRoute()}
     * @param sessionId a session identifier
     * @return the identifier of the server for which the session with the specified identifier has affinity, if present.
     */
    Optional<String> getAffinity(String sessionId);
}
