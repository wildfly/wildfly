/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
