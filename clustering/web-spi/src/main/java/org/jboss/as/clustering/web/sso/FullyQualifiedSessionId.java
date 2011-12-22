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

package org.jboss.as.clustering.web.sso;

import java.io.Serializable;

/**
 * Encapsulates a session id along with the name of the owning context and its hostname.
 *
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 */
public class FullyQualifiedSessionId implements Serializable {
    private static final long serialVersionUID = 6081884018218825708L;

    private final String hostName;
    private final String contextName;
    private final String sessionId;

    public FullyQualifiedSessionId(String sessionId, String contextName, String hostName) {
        this.sessionId = sessionId;
        this.contextName = contextName;
        this.hostName = hostName;
    }

    /**
     * Get the contextPath.
     *
     * @return the contextPath.
     */
    public String getContextName() {
        return contextName;
    }

    /**
     * Get the hostName.
     *
     * @return the hostName.
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Get the sessionId.
     *
     * @return the sessionId.
     */
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj instanceof FullyQualifiedSessionId) {
            FullyQualifiedSessionId other = (FullyQualifiedSessionId) obj;
            return (hostName.equals(other.hostName) && contextName.equals(other.contextName) && sessionId
                    .equals(other.sessionId));
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result += 29 * hostName.hashCode();
        result += 29 * contextName.hashCode();
        result += 29 * sessionId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return hostName + "/" + contextName + "/" + sessionId;
    }
}