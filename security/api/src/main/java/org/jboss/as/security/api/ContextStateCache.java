/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.security.api;

import org.jboss.as.security.remoting.RemoteConnection;
import org.jboss.security.SecurityContext;

/**
 * Where clients use the API in this module they can push an identity to the security context which also clears the identity
 * from the connection, when clients call push they are returned an instance of this class so that they can pass it later to a
 * call to pop to restore the state.
 *
 * Note: It is intentional that the content of this cache is not accessible to the client.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public final class ContextStateCache {

    private final RemoteConnection connection;
    private final SecurityContext securityContext;

    ContextStateCache(final RemoteConnection connection, final SecurityContext securityContext) {
        this.connection = connection;
        this.securityContext = securityContext;
    }

    RemoteConnection getConnection() {
        return connection;
    }

    SecurityContext getSecurityContext() {
        return securityContext;
    }

}
