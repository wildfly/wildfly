/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import org.jboss.as.util.security.GetContextClassLoaderAction;
import org.jboss.as.util.security.ReadPropertyAction;
import org.jboss.as.util.security.SetContextClassLoaderAction;
import org.jboss.as.util.security.SetContextClassLoaderFromClassAction;

import static java.lang.System.getProperty;
import static java.lang.System.getSecurityManager;
import static java.lang.Thread.currentThread;
import static java.security.AccessController.doPrivileged;

/**
 * Security actions to perform possibly privileged operations. No methods in this class are to be made public under any
 * circumstances!
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class SecurityActions {

    static String getSystemProperty(final String key) {
        return getSecurityManager() == null ? getProperty(key) : doPrivileged(new ReadPropertyAction(key));
    }

    static String getSystemProperty(final String key, final String defaultValue) {
        return getSecurityManager() == null ? getProperty(key, defaultValue) : doPrivileged(new ReadPropertyAction(key, defaultValue));
    }

    static ClassLoader getThreadContextClassLoader() {
        return getSecurityManager() == null ? currentThread().getContextClassLoader() : doPrivileged(GetContextClassLoaderAction.getInstance());
    }

    static ClassLoader setThreadContextClassLoader(Class cl) {
        if (getSecurityManager() == null) try {
            return currentThread().getContextClassLoader();
        } finally {
            currentThread().setContextClassLoader(cl.getClassLoader());
        } else {
            return doPrivileged(new SetContextClassLoaderFromClassAction(cl));
        }
    }

    static void setThreadContextClassLoader(ClassLoader cl) {
        if (getSecurityManager() == null) {
            currentThread().setContextClassLoader(cl);
        } else {
            doPrivileged(new SetContextClassLoaderAction(cl));
        }
    }
}