/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.deployers;

import org.jboss.as.util.security.GetContextClassLoaderAction;
import org.jboss.as.util.security.SetContextClassLoaderAction;

import static java.lang.System.getSecurityManager;
import static java.lang.Thread.currentThread;
import static java.security.AccessController.doPrivileged;

/**
 * Security actions helper.
 *
 * @author alessio.soldano@jboss.com
 * @since 17-Feb-2010
 */
class SecurityActions {
    /**
     * Get context classloader.
     *
     * @return the current context classloader
     */
    static ClassLoader getContextClassLoader() {
        return getSecurityManager() == null ? currentThread().getContextClassLoader() : doPrivileged(GetContextClassLoaderAction.getInstance());
    }

    /**
     * Set context classloader.
     *
     * @param classLoader the classloader
     */
    static void setContextClassLoader(final ClassLoader classLoader) {
        if (getSecurityManager() == null) {
            currentThread().setContextClassLoader(classLoader);
        } else {
            doPrivileged(new SetContextClassLoaderAction(classLoader));
        }
    }
}
