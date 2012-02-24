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
package org.jboss.as.cmp.jdbc;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
interface TCLAction {
    class UTIL {
        static TCLAction getTCLAction() {
            return System.getSecurityManager() == null ? NON_PRIVILEGED : PRIVILEGED;
        }

        static ClassLoader getContextClassLoader() {
            return getTCLAction().getContextClassLoader();
        }

        static ClassLoader getContextClassLoader(Thread thread) {
            return getTCLAction().getContextClassLoader(thread);
        }

        static void setContextClassLoader(ClassLoader cl) {
            getTCLAction().setContextClassLoader(cl);
        }

        static void setContextClassLoader(Thread thread, ClassLoader cl) {
            getTCLAction().setContextClassLoader(thread, cl);
        }
    }

    TCLAction NON_PRIVILEGED = new TCLAction() {
        public ClassLoader getContextClassLoader() {
            return Thread.currentThread().getContextClassLoader();
        }

        public ClassLoader getContextClassLoader(Thread thread) {
            return thread.getContextClassLoader();
        }

        public void setContextClassLoader(ClassLoader cl) {
            Thread.currentThread().setContextClassLoader(cl);
        }

        public void setContextClassLoader(Thread thread, ClassLoader cl) {
            thread.setContextClassLoader(cl);
        }
    };

    TCLAction PRIVILEGED = new TCLAction() {
        private final PrivilegedAction getTCLPrivilegedAction = new PrivilegedAction() {
            public Object run() {
                return Thread.currentThread().getContextClassLoader();
            }
        };

        public ClassLoader getContextClassLoader() {
            return (ClassLoader) AccessController.doPrivileged(getTCLPrivilegedAction);
        }

        public ClassLoader getContextClassLoader(final Thread thread) {
            return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    return thread.getContextClassLoader();
                }
            });
        }

        public void setContextClassLoader(final ClassLoader cl) {
            AccessController.doPrivileged(
                    new PrivilegedAction() {
                        public Object run() {
                            Thread.currentThread().setContextClassLoader(cl);
                            return null;
                        }
                    }
            );
        }

        public void setContextClassLoader(final Thread thread, final ClassLoader cl) {
            AccessController.doPrivileged(
                    new PrivilegedAction() {
                        public Object run() {
                            thread.setContextClassLoader(cl);
                            return null;
                        }
                    }
            );
        }
    };

    ClassLoader getContextClassLoader();

    ClassLoader getContextClassLoader(Thread thread);

    void setContextClassLoader(ClassLoader cl);

    void setContextClassLoader(Thread thread, ClassLoader cl);
}
