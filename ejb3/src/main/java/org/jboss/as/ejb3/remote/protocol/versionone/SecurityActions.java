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

package org.jboss.as.ejb3.remote.protocol.versionone;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.as.security.remoting.RemotingContext;
import org.jboss.remoting3.Connection;

final class SecurityActions {

    private SecurityActions() {
        // forbidden inheritance
    }

    /**
     * Gets context classloader.
     *
     * @return the current context classloader
     */
    static ClassLoader getContextClassLoader() {
        if (System.getSecurityManager() == null) {
            return Thread.currentThread().getContextClassLoader();
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    return Thread.currentThread().getContextClassLoader();
                }
            });
        }
    }

    /**
     * Sets context classloader.
     *
     * @param classLoader
     *            the classloader
     */
    static void setContextClassLoader(final ClassLoader classLoader) {
        if (System.getSecurityManager() == null) {
            Thread.currentThread().setContextClassLoader(classLoader);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    Thread.currentThread().setContextClassLoader(classLoader);
                    return null;
                }
            });
        }
    }

    /**
     * Set the Remoting Connection on the RemotingContext.
     *
     * @param connection - The Remoting connection.
     */
    static void remotingContextSetConnection(final Connection connection) {
        remotingContextAccociationActions().setConnection(connection);
    }

    /**
     * Clear the Remoting Connection on the RemotingContext.
     */
    static void remotingContextClear() {
        remotingContextAccociationActions().clear();
    }

    private static RemotingContextAssociationActions remotingContextAccociationActions() {
        return System.getSecurityManager() == null ? RemotingContextAssociationActions.NON_PRIVILEGED
                : RemotingContextAssociationActions.PRIVILEGED;
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
                AccessController.doPrivileged(new PrivilegedAction<Void>() {

                    public Void run() {
                        NON_PRIVILEGED.setConnection(connection);
                        return null;
                    }
                });

            }

            @Override
            public void clear() {
                AccessController.doPrivileged(CLEAR_ACTION);
            }
        };

    }

}
