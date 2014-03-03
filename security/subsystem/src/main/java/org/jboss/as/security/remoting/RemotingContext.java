/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.security.Permission;

import org.jboss.remoting3.Connection;

/**
 * A simple context to associate the Remoting Connection with the current thread.
 *
 * This association is used to make use of the user identity already authenticated on the connection.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RemotingContext {

    private static final RuntimePermission SET_CONNECTION_PERMISSION = new RuntimePermission("org.jboss.as.security.remoting.SET_CONNECTION");
    private static final RuntimePermission CLEAR_CONNECTION_PERMISSION = new RuntimePermission("org.jboss.as.security.remoting.CLEAR_CONNECTION");
    private static final RuntimePermission GET_CONNECTION_PERMISSION = new RuntimePermission("org.jboss.as.security.remoting.GET_CONNECTION");
    private static final RuntimePermission IS_SET_PERMISSION = new RuntimePermission("org.jboss.as.security.remoting.IS_CONNECTION_SET");

    private static ThreadLocal<Connection> connection = new ThreadLocal<Connection>();

    public static void setConnection(final Connection connection) {
        checkPermission(SET_CONNECTION_PERMISSION);

        RemotingContext.connection.set(connection);
    }

    public static void clear() {
        checkPermission(CLEAR_CONNECTION_PERMISSION);

        connection.set(null);
    }

    public static Connection getConnection() {
        checkPermission(GET_CONNECTION_PERMISSION);

        return connection.get();
    }

    public static boolean isSet() {
        checkPermission(IS_SET_PERMISSION);

        return connection.get() != null;
    }

    private static void checkPermission(final Permission permission) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(permission);
        }
    }

}
