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
 * Simple data class associating a server name with name of the server group
 * of which the server is a member.
 *
 * @author Brian Stansberry
 */
public final class ServerGroupMember implements Serializable {

    private static final long serialVersionUID = 819866199083400823L;

    private final String serverName;
    private final String serverGroupName;

    public ServerGroupMember(final String serverName, final String serverGroupName) {
        this.serverName = serverName;
        this.serverGroupName = serverGroupName;
    }

    public String getServerName() {
        return serverName;
    }

    public String getServerGroupName() {
        return serverGroupName;
    }

}
