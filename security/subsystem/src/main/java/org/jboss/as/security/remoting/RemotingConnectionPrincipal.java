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

package org.jboss.as.security.remoting;

import java.security.Principal;
import java.util.Collection;

import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.security.UserPrincipal;

/**
 * A {@link Principal} implementation to wrap a Remoting {@link Connection} and represent the identity authenticated against that Connection.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public final class RemotingConnectionPrincipal implements Principal {

    private final Connection connection;
    private final String name;
    private final int hashCode;

    public RemotingConnectionPrincipal(final Connection connection) {
        this.connection = connection;
        Collection<Principal> principals = connection.getPrincipals();
        String userName = null;
        for (Principal current : principals) {
            if (current instanceof UserPrincipal) {
                userName = current.getName();
                break;
            }
        }
        if (userName == null) {
            throw SecurityLogger.ROOT_LOGGER.noUserPrincipalFound();
        }
        name = userName;
        hashCode = connection.hashCode() * name.hashCode();

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RemotingConnectionPrincipal ? equals((RemotingConnectionPrincipal)obj) : false;
    }

    public boolean equals(RemotingConnectionPrincipal obj) {
        return this.connection.equals(obj.connection);
    }

    @Override
    public String toString() {
        return name;
    }

}
