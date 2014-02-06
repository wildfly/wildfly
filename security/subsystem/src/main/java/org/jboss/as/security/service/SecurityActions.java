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

package org.jboss.as.security.service;

import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.as.security.remoting.RemotingContext;
import org.wildfly.security.manager.action.GetModuleClassLoaderAction;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.remoting3.Connection;

import static java.security.AccessController.doPrivileged;

/**
 * Privileged blocks for this package
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 * @author Anil Saldhana
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class SecurityActions {

    static ModuleClassLoader getModuleClassLoader(final String moduleSpec) throws ModuleLoadException {
        ModuleLoader loader = Module.getCallerModuleLoader();
        final Module module = loader.loadModule(ModuleIdentifier.fromString(moduleSpec));
        return WildFlySecurityManager.isChecking() ? doPrivileged(new GetModuleClassLoaderAction(module)) : module.getClassLoader();
    }

    static Class<?> loadClass(final String name) throws ClassNotFoundException {
        if (WildFlySecurityManager.isChecking()) {
            try {
                return doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                    public Class<?> run() throws ClassNotFoundException {
                        ClassLoader[] cls = new ClassLoader[] { SecurityActions.class.getClassLoader(), // PB classes (not always on TCCL [modular env])
                                WildFlySecurityManager.getCurrentContextClassLoaderPrivileged(), // User defined classes
                                ClassLoader.getSystemClassLoader() // System loader, usually has app class path
                        };
                        ClassNotFoundException e = null;
                        for (ClassLoader cl : cls) {
                            if (cl == null) continue;

                            try {
                                return cl.loadClass(name);
                            } catch (ClassNotFoundException ce) {
                                e = ce;
                            }
                        }
                        throw e != null ? e : SecurityLogger.ROOT_LOGGER.cnfe(name);
                    }
                });
            } catch (PrivilegedActionException pae) {
                throw SecurityLogger.ROOT_LOGGER.cnfeThrow(name, pae);
            }
        } else {
            ClassLoader[] cls = new ClassLoader[] { SecurityActions.class.getClassLoader(), // PB classes (not always on TCCL [modular env])
                    WildFlySecurityManager.getCurrentContextClassLoaderPrivileged(), // User defined classes
                    ClassLoader.getSystemClassLoader() // System loader, usually has app class path
            };
            ClassNotFoundException e = null;
            for (ClassLoader cl : cls) {
                if (cl == null)
                    continue;

                try {
                    return cl.loadClass(name);
                } catch (ClassNotFoundException ce) {
                    e = ce;
                }
            }
            throw e != null ? e : SecurityLogger.ROOT_LOGGER.cnfe(name);
        }
    }

    static void remotingContextClear() {
        remotingContextAccociationActions().clear();
    }

    static Connection remotingContextGetConnection() {
        return remotingContextAccociationActions().getConnection();
    }

    static boolean remotingContextIsSet() {
        return remotingContextAccociationActions().isSet();
    }

    private static RemotingContextAssociationActions remotingContextAccociationActions() {
        return ! WildFlySecurityManager.isChecking() ? RemotingContextAssociationActions.NON_PRIVILEGED
                : RemotingContextAssociationActions.PRIVILEGED;
    }

    private interface RemotingContextAssociationActions {

        Connection getConnection();

        boolean isSet();

        void clear();

        RemotingContextAssociationActions NON_PRIVILEGED = new RemotingContextAssociationActions() {

            public boolean isSet() {
                return RemotingContext.isSet();
            }

            @Override
            public Connection getConnection() {
                return RemotingContext.getConnection();
            }

            @Override
            public void clear() {
                RemotingContext.clear();
            }
        };

        RemotingContextAssociationActions PRIVILEGED = new RemotingContextAssociationActions() {

            private final PrivilegedAction<Boolean> IS_SET_ACTION = new PrivilegedAction<Boolean>() {

                public Boolean run() {
                    return NON_PRIVILEGED.isSet();
                }
            };

            private final PrivilegedAction<Connection> GET_CONNECTION_ACTION = new PrivilegedAction<Connection>() {

                public Connection run() {
                    return NON_PRIVILEGED.getConnection();
                }
            };

            private final PrivilegedAction<Void> CLEAR_ACTION = new PrivilegedAction<Void>() {

                public Void run() {
                    NON_PRIVILEGED.clear();
                    return null;
                }
            };

            public boolean isSet() {
                return doPrivileged(IS_SET_ACTION);
            }

            public Connection getConnection() {
                return doPrivileged(GET_CONNECTION_ACTION);
            }

            public void clear() {
                doPrivileged(CLEAR_ACTION);
            }
        };

    }
}