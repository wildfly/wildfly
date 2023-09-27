/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.mojarra.context.flash;

import java.security.PrivilegedAction;

import jakarta.servlet.http.HttpSessionActivationListener;

import org.wildfly.security.manager.WildFlySecurityManager;

import com.sun.faces.context.flash.ELFlash;

/**
 * Utility methods requiring privileged actions.
 * Do not change class/method visibility to avoid being called from other {@link java.security.CodeSource}s, thus granting privilege escalation to external code.
 * @author Paul Ferraro
 */
class Reflect {
    /**
     * Returns a reference to the package protected {@link com.sun.faces.context.flash.SessionHelper} class.
     * @param loader a class loader
     * @param className the name of the class to load
     * @return the loaded class
     */
    static Class<? extends HttpSessionActivationListener> getSessionHelperClass() {
        return loadClass(WildFlySecurityManager.getClassLoaderPrivileged(ELFlash.class), "com.sun.faces.context.flash.SessionHelper").asSubclass(HttpSessionActivationListener.class);
    }

    /**
     * Loads a class with the specified name from the specified class loader.
     * @param loader a class loader
     * @param className the name of the class to load
     * @return the loaded class
     */
    static Class<?> loadClass(ClassLoader loader, String className) {
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<>() {
            @Override
            public Class<?> run() {
                try {
                    return loader.loadClass(className);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        });
    }
}
