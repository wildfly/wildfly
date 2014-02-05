/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
    Map.Entry<String, String> parse(String requestedSessionId);

    /**
     * Formats the specified session identifier and route identifier into a single identifier.
     * @param sessionId a session identifier
     * @param route a route identifier.
     * @return a single identifier containing the specified session identifier and routing identifier.
     */
    String format(String sessionId, String route);
}
