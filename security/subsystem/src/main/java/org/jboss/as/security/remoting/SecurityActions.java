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

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.remoting3.Connection;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * SecurityActions to manipulate the RemotingContext.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
final class SecurityActions {

    private SecurityActions() {
    }

    static Connection remotingContextGetConnection() {
        return remotingContextAssociationActions().getConnection();
    }

    private static RemotingContextAssociationActions remotingContextAssociationActions() {
        return ! WildFlySecurityManager.isChecking() ? RemotingContextAssociationActions.NON_PRIVILEGED
                : RemotingContextAssociationActions.PRIVILEGED;
    }

    private interface RemotingContextAssociationActions {

        Connection getConnection();

        RemotingContextAssociationActions NON_PRIVILEGED = new RemotingContextAssociationActions() {

            public Connection getConnection() {
                return RemotingContext.getConnection();
            }
        };

        RemotingContextAssociationActions PRIVILEGED = new RemotingContextAssociationActions() {

            private final PrivilegedAction<Connection> GET_CONNECTION_ACTION = new PrivilegedAction<Connection>() {

                public Connection run() {
                    return NON_PRIVILEGED.getConnection();
                }

            };

            public Connection getConnection() {
                return AccessController.doPrivileged(GET_CONNECTION_ACTION);
            }
        };

    }

}
