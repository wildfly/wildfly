/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.web.session;

import java.util.Map;

/**
 * Exposes the mechanism for parsing and formation routing information from/into a requested session identifier.
 * @author Paul Ferraro
 */
public interface RoutingSupport {
    /**
     * Parses the routing information from the specified session identifier.
     * @param requestedSessionId the requested session identifier.
     * @return a map entry containing the session ID and routing information as the key and value, respectively.
     */
    Map.Entry<CharSequence, CharSequence> parse(CharSequence requestedSessionId);

    /**
     * Formats the specified session identifier and route identifier into a single identifier.
     * @param sessionId a session identifier
     * @param route a route identifier.
     * @return a single identifier containing the specified session identifier and routing identifier.
     */
    CharSequence format(CharSequence sessionId, CharSequence route);
}
