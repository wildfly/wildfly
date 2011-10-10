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

package org.jboss.as.web.security.jaspi;

import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * <p>
 * This class defines actions that must be executed in privileged blocks.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
class SecurityActions {

    /**
     * <p>
     * Loads the specified class.
     * </p>
     *
     * @param fqn the fully-qualified name of the class to be loaded.
     * @return the loaded class.
     * @throws PrivilegedActionException if an error occurs while loading the class.
     */
    static Class<?> loadClass(final String fqn) throws PrivilegedActionException {
        return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
            public Class<?> run() throws Exception {
                ClassLoader loader = SecurityActions.class.getClassLoader();
                return loader.loadClass(fqn);
            }
        });
    }

    /**
     * <p>
     * Obtains the current {@code SecurityContext}.
     * </p>
     *
     * @return a reference to the current {@code SecurityContext} instance.
     */
    static SecurityContext getSecurityContext() {
        return AccessController.doPrivileged(new PrivilegedAction<SecurityContext>() {
            public SecurityContext run() {
                return SecurityContextAssociation.getSecurityContext();
            }
        });
    }

}