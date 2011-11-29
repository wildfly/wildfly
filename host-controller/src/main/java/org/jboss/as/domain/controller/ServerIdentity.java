/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.controller;

import java.io.Serializable;

/**
 * Identifying information for a server in a domain. A bit of a misnomer, as
 * the server's name is sufficient identification since all servers in a
 * domain must have unique names.
 *
 * @author Brian Stansberry
 */
public class ServerIdentity implements Serializable {

    private static final long serialVersionUID = -5853735093238463353L;

    private final String hostName;
    private final String serverName;
    private final String serverGroupName;

    public ServerIdentity(final String hostName, final String serverGroupName, final String serverName) {
        this.hostName = hostName;
        this.serverGroupName = serverGroupName;
        this.serverName = serverName;
    }

    public String getServerGroupName() {
        return serverGroupName;
    }

    public String getHostName() {
        return hostName;
    }

    public String getServerName() {
        return serverName;
    }

    /**
     * Returns the hash code of the server name and host name.
     */
    @Override
    public int hashCode() {
        int result = 17;
        result += 31 * serverName.hashCode();
        result += 31 * hostName.hashCode();
        return result;
    }

    /**
     * Returns {@code true} if {@code obj} is a HostedServer with the same server name and host name.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj instanceof ServerIdentity) {
            ServerIdentity other = (ServerIdentity) obj;
            return serverName.equals(other.serverName) && hostName.equals(other.hostName);
        }
        return false;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
            .append("{name=")
            .append(serverName)
            .append(", host=")
            .append(hostName)
            .append(", server-group=")
            .append(serverGroupName)
            .append("}")
            .toString();
    }
}
