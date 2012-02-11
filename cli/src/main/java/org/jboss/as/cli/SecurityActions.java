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
package org.jboss.as.cli;

import java.security.AccessController;
import java.security.PrivilegedAction;


/**
 * Package privileged actions
 *
 * @author Scott.Stark@jboss.org
 * @author Alexey Loubyansky
 */
class SecurityActions {
    private interface TCLAction {
        class UTIL {
            static TCLAction getTCLAction() {
                return System.getSecurityManager() == null ? NON_PRIVILEGED : PRIVILEGED;
            }

            static String getSystemProperty(String name) {
                return getTCLAction().getSystemProperty(name);
            }

            static ClassLoader getContextClassLoader() {
                return getTCLAction().getContextClassLoader();
            }
        }

        TCLAction NON_PRIVILEGED = new TCLAction() {
            @Override
            public String getSystemProperty(String name) {
                return System.getProperty(name);
            }

            @Override
            public ClassLoader getContextClassLoader() {
                return Thread.currentThread().getContextClassLoader();
            }
        };

        TCLAction PRIVILEGED = new TCLAction() {

            @Override
            public String getSystemProperty(final String name) {
                return (String) AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    public Object run() {
                        return System.getProperty(name);
                    }
                });
            }

            @Override
            public ClassLoader getContextClassLoader() {
                return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    public Object run() {
                        return Thread.currentThread().getContextClassLoader();
                    }
                });
            }
        };

        String getSystemProperty(String name);

        ClassLoader getContextClassLoader();
    }

    protected static String getSystemProperty(String name) {
        return TCLAction.UTIL.getSystemProperty(name);
    }

    protected static ClassLoader getContextClassLoader() {
        return TCLAction.UTIL.getContextClassLoader();
    }
}
