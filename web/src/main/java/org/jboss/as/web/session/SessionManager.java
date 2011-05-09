/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Manager;
import org.apache.catalina.Session;

/**
 * Common interface for the http session replication managers.
 *
 * @author Scott.Stark@jboss.org
 * @version $Revision: 108925 $
 */
public interface SessionManager extends Manager {
    /**
     * Retrieve the JvmRoute for the enclosing Engine.
     *
     * @return the JvmRoute or null.
     */
    String getJvmRoute();

    /**
     * Locate the most appropriate jvm route for the specified sessionId
     *
     * @param sessionId a session identifier
     * @return a jvm route
     */
    String locate(String sessionId);

    /**
     * Sets a new cookie for the given session id and response
     *
     * @param sessionId The session id
     */
    void setNewSessionCookie(String sessionId, HttpServletResponse response);

    /**
     * Remove the active session locally from the manager without replicating to the cluster. This can be useful when the
     * session is expired, for example, where there is not need to propagate the expiration.
     *
     * @param session
     */
    void removeLocal(Session session);

    /**
     * Store the modified session.
     *
     * @param session
     */
    boolean storeSession(Session session);

    Map.Entry<String, String> parse(String sessionId);

    String createSessionId(String realId, String jvmRoute);
}
