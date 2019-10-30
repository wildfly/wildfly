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

package org.jboss.as.ejb3.remote;

import java.security.PrivilegedAction;

import org.jboss.as.security.remoting.RemoteConnection;
import org.jboss.as.security.remoting.RemotingContext;
import org.jboss.remoting3.Connection;
import org.wildfly.security.manager.WildFlySecurityManager;

import static java.security.AccessController.doPrivileged;

final class SecurityActions {

    private SecurityActions() {
        // forbidden inheritance
    }

    /**
     * Set the Remoting Connection on the RemotingContext.
     *
     * @param connection - The Remoting connection.
     */
    static void remotingContextSetConnection(final Connection connection) {
        remotingContextAssociationActions().setConnection(connection);
    }

    /**
     * Set the Remoting Connection on the RemotingContext.
     *
     * @param connection - The Remoting connection.
     */
    static void remotingContextSetConnection(final RemoteConnection connection) {
        remoteContextAssociationActions().setConnection(connection);
    }

    /**
     * Clear the Remoting Connection on the RemotingContext.
     */
    static void remotingContextClear() {
        remotingContextAssociationActions().clear();
    }

    private static RemotingContextAssociationActions remotingContextAssociationActions() {
        return ! WildFlySecurityManager.isChecking() ? RemotingContextAssociationActions.NON_PRIVILEGED
                : RemotingContextAssociationActions.PRIVILEGED;
    }

    private static RemoteContextAssociationActions remoteContextAssociationActions() {
        return ! WildFlySecurityManager.isChecking() ? RemoteContextAssociationActions.NON_PRIVILEGED
                : RemoteContextAssociationActions.PRIVILEGED;
    }
    private interface RemotingContextAssociationActions {

        void setConnection(final Connection connection);

        void clear();

        RemotingContextAssociationActions NON_PRIVILEGED = new RemotingContextAssociationActions() {

            public void setConnection(Connection connection) {
                RemotingContext.setConnection(connection);
            }

            public void clear() {
                RemotingContext.clear();
            }
        };

        RemotingContextAssociationActions PRIVILEGED = new RemotingContextAssociationActions() {

            private PrivilegedAction<Void> CLEAR_ACTION = new PrivilegedAction<Void>() {

                public Void run() {
                    NON_PRIVILEGED.clear();
                    return null;
                }
            };

            public void setConnection(final Connection connection) {
                doPrivileged(new PrivilegedAction<Void>() {

                    public Void run() {
                        NON_PRIVILEGED.setConnection(connection);
                        return null;
                    }
                });

            }

            @Override
            public void clear() {
                doPrivileged(CLEAR_ACTION);
            }
        };

    }

    private interface RemoteContextAssociationActions {

        void setConnection(final RemoteConnection connection);

        void clear();

        RemoteContextAssociationActions NON_PRIVILEGED = new RemoteContextAssociationActions() {

            public void setConnection(RemoteConnection connection) {
                RemotingContext.setConnection(connection);
            }

            public void clear() {
                RemotingContext.clear();
            }
        };

        RemoteContextAssociationActions PRIVILEGED = new RemoteContextAssociationActions() {

            private PrivilegedAction<Void> CLEAR_ACTION = new PrivilegedAction<Void>() {

                public Void run() {
                    NON_PRIVILEGED.clear();
                    return null;
                }
            };

            public void setConnection(final RemoteConnection connection) {
                doPrivileged(new PrivilegedAction<Void>() {

                    public Void run() {
                        NON_PRIVILEGED.setConnection(connection);
                        return null;
                    }
                });

            }

            @Override
            public void clear() {
                doPrivileged(CLEAR_ACTION);
            }
        };

    }
}
