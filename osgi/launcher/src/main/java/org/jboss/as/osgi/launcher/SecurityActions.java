/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.osgi.launcher;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.Properties;

/**
 * Secured actions not to leak out of this package
 *
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 */
final class SecurityActions {

    private SecurityActions() {
        throw new UnsupportedOperationException("No instances permitted");
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

    static Map<String, String> getSystemEnvironment() {
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

    private enum GetSystemPropertiesAction implements PrivilegedAction<Properties> {
        INSTANCE;

        @Override
        public Properties run() {
            return System.getProperties();
        }
    }

    static Properties getSystemProperties() {
        if (System.getSecurityManager() == null) {
            return System.getProperties();
        } else {
            return AccessController.doPrivileged(GetSystemPropertiesAction.INSTANCE);
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
}
