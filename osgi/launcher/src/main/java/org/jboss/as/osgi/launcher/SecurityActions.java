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
import org.wildfly.security.manager.ClearPropertyAction;
import org.wildfly.security.manager.GetContextClassLoaderAction;
import org.wildfly.security.manager.GetEnvironmentAction;
import org.wildfly.security.manager.GetSystemPropertiesAction;
import org.wildfly.security.manager.ReadPropertyAction;
import org.wildfly.security.manager.SetContextClassLoaderAction;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.security.manager.WritePropertyAction;

import static java.lang.System.clearProperty;
import static java.lang.System.getProperties;
import static java.lang.System.getProperty;
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
        if (! WildFlySecurityManager.isChecking()) {
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
        return ! WildFlySecurityManager.isChecking() ? getenv() : doPrivileged(GetEnvironmentAction.getInstance());
    }

    static void clearSystemProperty(final String key) {
        if (! WildFlySecurityManager.isChecking()) {
            clearProperty(key);
        } else {
            doPrivileged(new ClearPropertyAction(key));
        }
    }

    static void setSystemProperty(final String key, final String value) {
        if (! WildFlySecurityManager.isChecking()) {
            setProperty(key, value);
        } else {
            doPrivileged(new WritePropertyAction(key, value));
        }
    }

    static String getSystemProperty(final String key) {
        return ! WildFlySecurityManager.isChecking() ? getProperty(key) : doPrivileged(new ReadPropertyAction(key));
    }

    static Properties getSystemProperties() {
        return ! WildFlySecurityManager.isChecking() ? getProperties() : doPrivileged(GetSystemPropertiesAction.getInstance());
    }

    static ClassLoader getContextClassLoader() {
        return ! WildFlySecurityManager.isChecking() ? currentThread().getContextClassLoader() : doPrivileged(GetContextClassLoaderAction.getInstance());
    }

    static void setContextClassLoader(final ClassLoader tccl) {
        if (! WildFlySecurityManager.isChecking()) {
            currentThread().setContextClassLoader(tccl);
        } else {
            doPrivileged(new SetContextClassLoaderAction(tccl));
        }
    }
}
