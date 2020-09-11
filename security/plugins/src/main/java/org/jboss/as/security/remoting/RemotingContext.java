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

import javax.net.ssl.SSLSession;

import org.jboss.remoting3.Connection;
import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * A simple context to associate the Remoting Connection with the current thread.
 *
 * This association is used to make use of the user identity already authenticated on the connection.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RemotingContext {

    /**
     * A {@link org.jboss.as.security.remoting.RemotingPermission} needed to clear a {@link org.jboss.as.security.remoting.RemotingContext}'s {@link org.jboss.remoting3.Connection}. The name of the permission is "{@code clearConnection}."
     */
    private static final RemotingPermission CLEAR_CONNECTION = new RemotingPermission("clearConnection");
    /**
     * A {@link org.jboss.as.security.remoting.RemotingPermission} needed to retrieve a {@link org.jboss.as.security.remoting.RemotingContext}'s {@link org.jboss.remoting3.Connection}. The name of the permission is "{@code getConnection}."
     */
    private static final RemotingPermission GET_CONNECTION = new RemotingPermission("getConnection");
    /**
     * A {@link org.jboss.as.security.remoting.RemotingPermission} needed to check if a {@link org.jboss.as.security.remoting.RemotingContext}'s {@link org.jboss.remoting3.Connection} is set. The name of the permission is "{@code isConnectionSet}."
     */
    private static final RemotingPermission IS_CONNECTION_SET = new RemotingPermission("isConnectionSet");
    /**
     * A {@link org.jboss.as.security.remoting.RemotingPermission} needed to set a {@link org.jboss.as.security.remoting.RemotingContext}'s {@link org.jboss.remoting3.Connection}. The name of the permission is "{@code setConnection}."
     */
    private static final RemotingPermission SET_CONNECTION = new RemotingPermission("setConnection");

    private static ThreadLocal<RemoteConnection> connection = new ThreadLocal<RemoteConnection>();

    public static void setConnection(final Connection connection) {
        checkPermission(SET_CONNECTION);
        RemotingContext.connection.set(new RemotingRemoteConnection(connection));
    }

    public static void setConnection(final RemoteConnection connection) {
        checkPermission(SET_CONNECTION);
        RemotingContext.connection.set(connection);
    }
    public static void clear() {
        checkPermission(CLEAR_CONNECTION);

        connection.set(null);
    }

    public static Connection getConnection() {
        checkPermission(GET_CONNECTION);

        RemoteConnection remoteConnection = connection.get();
        if(remoteConnection instanceof  RemotingRemoteConnection) {
            return ((RemotingRemoteConnection) remoteConnection).connection;
        }
        return null;
    }

    public static RemoteConnection getRemoteConnection() {
        checkPermission(GET_CONNECTION);
        return connection.get();
    }

    public static boolean isSet() {
        checkPermission(IS_CONNECTION_SET);

        return connection.get() != null;
    }

    private static void checkPermission(final Permission permission) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(permission);
        }
    }

    private static final class RemotingRemoteConnection implements RemoteConnection {

        final Connection connection;

        private RemotingRemoteConnection(Connection connection) {
            this.connection = connection;
        }

        @Override
        public SSLSession getSslSession() {
            return connection.getSslSession();
        }

        @Override
        public SecurityIdentity getSecurityIdentity() {
            return connection.getLocalIdentity();
        }
    }

}
