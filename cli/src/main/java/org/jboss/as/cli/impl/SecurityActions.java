/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cli.impl;

import org.jboss.as.util.security.AddShutdownHookAction;
import org.jboss.as.util.security.GetClassLoaderAction;
import org.jboss.as.util.security.ReadEnvironmentPropertyAction;
import org.jboss.as.util.security.ReadPropertyAction;
import org.jboss.as.util.security.WritePropertyAction;

import static java.lang.Runtime.getRuntime;
import static java.lang.System.getProperty;
import static java.lang.System.getSecurityManager;
import static java.lang.System.getenv;
import static java.lang.System.setProperty;
import static java.security.AccessController.doPrivileged;

/**
 * Package privileged actions
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Scott.Stark@jboss.org
 * @author Alexey Loubyansky
 */
class SecurityActions {
    static void addShutdownHook(Thread hook) {
        if (getSecurityManager() == null) {
            getRuntime().addShutdownHook(hook);
        } else {
            doPrivileged(new AddShutdownHookAction(hook));
        }
    }

    static String getSystemProperty(String name) {
        return getSecurityManager() == null ? getProperty(name) : doPrivileged(new ReadPropertyAction(name));
    }

    static void setSystemProperty(String name, String value) {
        if (getSecurityManager() == null) {
            setProperty(name, value);
        } else {
            doPrivileged(new WritePropertyAction(name, value));
        }
    }

    static ClassLoader getClassLoader(Class<?> cls) {
        return getSecurityManager() == null ? cls.getClassLoader() : doPrivileged(new GetClassLoaderAction(cls));
    }

    static String getEnvironmentVariable(String name) {
        return getSecurityManager() == null ? getenv(name) : doPrivileged(new ReadEnvironmentPropertyAction(name));
    }
}
