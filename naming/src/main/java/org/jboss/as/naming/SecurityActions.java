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

package org.jboss.as.naming;

import static org.jboss.as.naming.NamingMessages.MESSAGES;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

final class SecurityActions {

    private SecurityActions() {
        // forbidden inheritance
    }

    static String getSystemProperty(final String property) {
        if (System.getSecurityManager() == null) {
            return System.getProperty(property);
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty(property);
                }
            });
        }
    }

    static void setSystemProperty(final String property, final String value) {
        if (System.getSecurityManager() == null) {
            System.setProperty(property, value);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    System.setProperty(property, value);
                    return null;
                }
            });
        }
    }

    /**
     * Gets context classloader.
     *
     * @return the current context classloader
     */
    static ClassLoader getContextClassLoader() {
        if (System.getSecurityManager() == null) {
            return Thread.currentThread().getContextClassLoader();
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return Thread.currentThread().getContextClassLoader();
                }
            });
        }
    }

    /**
     * Sets context classloader.
     *
     * @param classLoader
     *            the classloader
     */
    static void setContextClassLoader(final ClassLoader classLoader) {
        if (System.getSecurityManager() == null) {
            Thread.currentThread().setContextClassLoader(classLoader);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    Thread.currentThread().setContextClassLoader(classLoader);
                    return null;
                }
            });
        }
    }

    static Object lookup(final Context context, final Name name) throws NamingException {
        if (System.getSecurityManager() == null) {
            return context.lookup(name);
        } else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() throws Exception {
                        return context.lookup(name);
                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable cause = e.getCause();
                throw rethrowTypedOrUnchecked(NamingException.class, cause, MESSAGES.unexpectedExceptionDuringPrivilegedLookup(cause));
            }
        }
    }

    static Object lookup(final NamingContext context, final Name name, final boolean dereference) throws NamingException {
        if (System.getSecurityManager() == null) {
            return context.lookup(name, dereference);
        } else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() throws Exception {
                        return context.lookup(name, dereference);
                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable cause = e.getCause();
                throw rethrowTypedOrUnchecked(NamingException.class, cause, MESSAGES.unexpectedExceptionDuringPrivilegedLookup(cause));
            }
        }
    }

    static Object lookup(final javax.naming.InitialContext context, final String name) throws NamingException {
        if (System.getSecurityManager() == null) {
            return context.lookup(name);
        } else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() throws Exception {
                        return context.lookup(name);
                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable cause = e.getCause();
                throw rethrowTypedOrUnchecked(NamingException.class, cause, MESSAGES.unexpectedExceptionDuringPrivilegedLookup(cause));
            }
        }
    }

    static NamingEnumeration<NameClassPair> list(final Context context, final Name name) throws NamingException {
        if (System.getSecurityManager() == null) {
            return context.list(name);
        } else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<NamingEnumeration<NameClassPair>>() {
                    @Override
                    public NamingEnumeration<NameClassPair> run() throws Exception {
                        return context.list(name);
                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable cause = e.getCause();
                throw rethrowTypedOrUnchecked(NamingException.class, cause, MESSAGES.unexpectedExceptionDuringPrivilegedList(cause));
            }
        }
    }

    static NamingEnumeration<Binding> listBindings(final Context context, final Name name) throws NamingException {
        if (System.getSecurityManager() == null) {
            return context.listBindings(name);
        } else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<NamingEnumeration<Binding>>() {
                    @Override
                    public NamingEnumeration<Binding> run() throws Exception {
                        return context.listBindings(name);
                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable cause = e.getCause();
                throw rethrowTypedOrUnchecked(NamingException.class, cause, MESSAGES.unexpectedExceptionDuringPrivilegedListBindings(cause));
            }
        }
    }

    private static <T extends Throwable> RuntimeException rethrowTypedOrUnchecked(Class<T> exceptionType, Throwable exception, RuntimeException fallback) throws T {
        if (exceptionType.isInstance(exception)) {
            throw exceptionType.cast(exception);
        } else if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        } else if (exception instanceof Error) {
            throw (Error) exception;
        } else {
            throw fallback;
        }
    }
}
