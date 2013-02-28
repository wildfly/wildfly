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
package org.jboss.as.server.operations;

import org.jboss.as.util.security.ClearPropertyAction;
import org.jboss.as.util.security.GetClassLoaderAction;
import org.jboss.as.util.security.ReadPropertyAction;
import org.jboss.as.util.security.WritePropertyAction;

import static java.lang.System.clearProperty;
import static java.lang.System.getProperty;
import static java.lang.System.getSecurityManager;
import static java.lang.System.setProperty;
import static java.security.AccessController.doPrivileged;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class SecurityActions {

    static String setSystemProperty(final String key, final String value) throws SecurityException {
        return getSecurityManager() == null ? setProperty(key, value) : doPrivileged(new WritePropertyAction(key, value));
    }

    static String clearSystemProperty(final String key) throws SecurityException {
        return getSecurityManager() == null ? clearProperty(key) : doPrivileged(new ClearPropertyAction(key));
    }

    static String getSystemProperty(final String name) {
        return getSecurityManager() == null ? getProperty(name) : doPrivileged(new ReadPropertyAction(name));
    }

    static ClassLoader getClassLoader(final Class<?> clazz) {
        return getSecurityManager() == null ? clazz.getClassLoader() : doPrivileged(new GetClassLoaderAction(clazz));
    }
}
