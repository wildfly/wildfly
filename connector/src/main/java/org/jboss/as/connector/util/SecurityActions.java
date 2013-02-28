/*
 * The Fungal kernel project
 * Copyright (C) 2010
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.connector.util;

import org.jboss.as.util.security.GetContextClassLoaderAction;
import org.jboss.as.util.security.ReadPropertyAction;
import org.jboss.as.util.security.SetContextClassLoaderAction;

import static java.lang.System.getProperty;
import static java.lang.System.getSecurityManager;
import static java.lang.Thread.currentThread;
import static java.security.AccessController.doPrivileged;

/**
 * Privileged Blocks
 *
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class SecurityActions {
    /**
     * Constructor
     */
    private SecurityActions() {
    }

    /**
     * Get the thread context class loader
     * @return The class loader
     */
    static ClassLoader getThreadContextClassLoader() {
        return getSecurityManager() == null ? currentThread().getContextClassLoader() : doPrivileged(GetContextClassLoaderAction.getInstance());
    }

    /**
     * Set the thread context class loader
     * @param cl The class loader
     */
    static void setThreadContextClassLoader(final ClassLoader cl) {
        if (getSecurityManager() == null) {
            currentThread().setContextClassLoader(cl);
        } else {
            doPrivileged(new SetContextClassLoaderAction(cl));
        }
    }

    /**
     * Get a system property
     * @param name The property name
     * @return The property value
     */
    static String getSystemProperty(final String name) {
        return getSecurityManager() == null ? getProperty(name) : doPrivileged(new ReadPropertyAction(name));
    }
}
