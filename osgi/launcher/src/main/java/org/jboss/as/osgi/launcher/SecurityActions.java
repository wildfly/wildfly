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
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.Properties;
import org.jboss.as.util.security.ClearPropertyAction;
import org.jboss.as.util.security.GetContextClassLoaderAction;
import org.jboss.as.util.security.GetEnvironmentAction;
import org.jboss.as.util.security.GetSystemPropertiesAction;
import org.jboss.as.util.security.ReadPropertyAction;
import org.jboss.as.util.security.SetContextClassLoaderAction;
import org.jboss.as.util.security.WritePropertyAction;

import static java.lang.System.clearProperty;
import static java.lang.System.getProperties;
import static java.lang.System.getProperty;
import static java.lang.System.getSecurityManager;
import static java.lang.System.getenv;
import static java.lang.System.setProperty;
import static java.lang.Thread.currentThread;
import static java.security.AccessController.doPrivileged;

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
        if (getSecurityManager() == null) {
            method.setAccessible(true);
        } else {
            try {
                doPrivileged(new PrivilegedExceptionAction<Void>() {

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
        return getSecurityManager() == null ? getenv() : doPrivileged(GetEnvironmentAction.getInstance());
    }

    static void clearSystemProperty(final String key) {
        if (getSecurityManager() == null) {
            clearProperty(key);
        } else {
            doPrivileged(new ClearPropertyAction(key));
        }
    }

    static void setSystemProperty(final String key, final String value) {
        if (getSecurityManager() == null) {
            setProperty(key, value);
        } else {
            doPrivileged(new WritePropertyAction(key, value));
        }
    }

    static String getSystemProperty(final String key) {
        return getSecurityManager() == null ? getProperty(key) : doPrivileged(new ReadPropertyAction(key));
    }

    static Properties getSystemProperties() {
        return getSecurityManager() == null ? getProperties() : doPrivileged(GetSystemPropertiesAction.getInstance());
    }

    static ClassLoader getContextClassLoader() {
        return getSecurityManager() == null ? currentThread().getContextClassLoader() : doPrivileged(GetContextClassLoaderAction.getInstance());
    }

    static void setContextClassLoader(final ClassLoader tccl) {
        if (getSecurityManager() == null) {
            currentThread().setContextClassLoader(tccl);
        } else {
            doPrivileged(new SetContextClassLoaderAction(tccl));
        }
    }
}
