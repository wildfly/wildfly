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

package org.jboss.as.connector.services.resourceadapters.deployment;

import org.jboss.as.util.security.GetContextClassLoaderAction;
import org.jboss.as.util.security.SetContextClassLoaderAction;

import static java.lang.System.getSecurityManager;
import static java.lang.Thread.currentThread;
import static java.security.AccessController.doPrivileged;

/**
 * Privileged Blocks
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
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
}
