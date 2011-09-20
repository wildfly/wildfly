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
package org.jboss.as.jmx.model;

import java.security.AccessController;
import java.security.PrivilegedAction;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SecurityActions {
    static ClassLoader getClassLoader(final Class<?> clazz) {
        if (System.getSecurityManager() == null) {
            return GetClassLoaderAction.NON_PRIVILEGED.getClassLoader(clazz);
        } else {
            return GetClassLoaderAction.PRIVILEGED.getClassLoader(clazz);
        }
    }

    private interface GetClassLoaderAction {

        ClassLoader getClassLoader(Class<?> clazz);

        GetClassLoaderAction NON_PRIVILEGED = new GetClassLoaderAction() {
            @Override
            public ClassLoader getClassLoader(final Class<?> clazz) {
                return clazz.getClassLoader();
            }
        };

        GetClassLoaderAction PRIVILEGED = new GetClassLoaderAction() {

            @Override
            public ClassLoader getClassLoader(final Class<?> clazz) {
                return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                    public ClassLoader run() {
                        return clazz.getClassLoader();
                    }
                });
            }
        };
    }

}
