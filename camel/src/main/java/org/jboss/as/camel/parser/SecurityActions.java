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

package org.jboss.as.camel.parser;

import org.jboss.as.util.security.GetClassLoaderAction;
import org.jboss.as.util.security.ReadPropertyAction;
import org.jboss.as.util.security.WritePropertyAction;

import static java.lang.System.getProperty;
import static java.lang.System.getSecurityManager;
import static java.lang.System.setProperty;
import static java.security.AccessController.doPrivileged;

/**
 * Privileged actions used by this package.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Jan-2010
 */
class SecurityActions {

    private SecurityActions() {
    }

    static String getSystemProperty(final String key, final String defaultValue) {
        return getSecurityManager() == null ? getProperty(key, defaultValue) : doPrivileged(new ReadPropertyAction(key, defaultValue));
    }

    static String setSystemProperty(final String key, final String value) {
        return getSecurityManager() == null ? setProperty(key, value) : doPrivileged(new WritePropertyAction(key, value));
    }

    static ClassLoader getClassLoader(final Class<?> clazz) {
        return getSecurityManager() == null ? clazz.getClassLoader() : doPrivileged(new GetClassLoaderAction(clazz));
    }
}
