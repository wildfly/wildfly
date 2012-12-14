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

package org.jboss.as.embedded;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.Properties;

/**
 * Security actions to access system environment information.  No methods in
 * this class are to be made public under any circumstances!
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class SecurityActions {

    private SecurityActions() {
    }

    static String getSystemProperty(final String key) {
        if (System.getSecurityManager() == null) {
            return System.getProperty(key);
        }

        return AccessController.doPrivileged(new PrivilegedAction<String>() {

            @Override
            public String run() {
                return System.getProperty(key);
            }
        });
    }

    static void setSystemProperty(final String key, final String value) {
        if (System.getSecurityManager() == null) {
            System.setProperty(key, value);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {

                @Override
                public Void run() {
                    System.setProperty(key, value);
                    return null;
                }
            });
        }
    }

    static void clearSystemProperty(final String key) {
        if (System.getSecurityManager() == null) {
            System.clearProperty(key);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {

                @Override
                public Void run() {
                    System.clearProperty(key);
                    return null;
                }
            });
        }
    }

    public static Properties getSystemProperties() {
        if (System.getSecurityManager() == null) {
            return System.getProperties();
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<Properties>() {
                @Override
                public Properties run() {
                    return System.getProperties();
                }
            });
        }
    }

    public static Map<String, String> getSystemEnvironment() {
        if (System.getSecurityManager() == null) {
            return System.getenv();
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<Map<String, String>>() {
                @Override
                public Map<String, String> run() {
                    return System.getenv();
                }
            });
        }
    }

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

    static void setContextClassLoader(final ClassLoader tccl) {

        if (System.getSecurityManager() == null) {
            Thread.currentThread().setContextClassLoader(tccl);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {

                @Override
                public Void run() {
                    Thread.currentThread().setContextClassLoader(tccl);
                    return null;
                }
            });
        }
    }

    static void setAccessible(final Method method) throws SecurityException {
        if (method == null) {
            throw new IllegalArgumentException("method must be specified");
        }
        if (System.getSecurityManager() == null) {
            method.setAccessible(true);
        } else {
            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

                    @Override
                    public Void run() throws Exception {
                        method.setAccessible(true);
                        return null;
                    }
                });
            } catch (final PrivilegedActionException pae) {
                final Throwable cause = pae.getCause();
                if (cause instanceof SecurityException) {
                    throw (SecurityException) cause;
                } else {
                    throw new RuntimeException("Unexpected exception encountered settingg accessibility of " + method
                            + " to true", cause);
                }
            }
        }
    }
}
