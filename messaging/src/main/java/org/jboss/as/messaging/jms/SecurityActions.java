package org.jboss.as.messaging.jms;

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import org.jboss.as.util.security.GetContextClassLoaderAction;
import org.jboss.as.util.security.SetContextClassLoaderAction;
import org.jboss.as.util.security.SetContextClassLoaderFromClassAction;

import static java.lang.System.getSecurityManager;
import static java.lang.Thread.currentThread;
import static java.security.AccessController.doPrivileged;

public final class SecurityActions {

    private SecurityActions() {
        // forbidden inheritance
    }

    /**
     * Gets context classloader.
     *
     * @return the current context classloader
     */
    public static ClassLoader getContextClassLoader() {
        return getSecurityManager() == null ? currentThread().getContextClassLoader() : doPrivileged(GetContextClassLoaderAction.getInstance());
    }

    /**
     * Sets current thread context classloader to the class' ClassLoader
     *
     * @param clazz the class
     */
    public static ClassLoader setThreadContextClassLoader(Class clazz) {
        if (getSecurityManager() == null) {
            final Thread thread = currentThread();
            try {
                return thread.getContextClassLoader();
            } finally {
                thread.setContextClassLoader(clazz.getClassLoader());
            }
        } else {
            return doPrivileged(new SetContextClassLoaderFromClassAction(clazz));
        }
    }

    /**
     * Sets current thread context classloader to the classLoader
     *
     * @param cl the class loader
     */
    public static void setThreadContextClassLoader(ClassLoader cl) {
        if (getSecurityManager() == null) {
            currentThread().setContextClassLoader(cl);
        } else {
            doPrivileged(new SetContextClassLoaderAction(cl));
        }
    }
}
