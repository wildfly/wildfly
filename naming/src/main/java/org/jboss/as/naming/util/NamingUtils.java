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

package org.jboss.as.naming.util;

import javax.naming.CannotProceedException;
import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Iterator;

/**
 * Common utility functions used by the naming implementation.
 * 
 * @author John E. Bailey
 */
public class NamingUtils {
    private NamingUtils() {}

    public static String getLastComponent(final Name name) {
        if(name.size() > 0)
            return name.get(name.size() - 1);
        return "";
    }

    public static boolean isEmpty(final Name name) {
        return name.isEmpty() || (name.size() == 1 && "".equals(name.get(0)));
    }

    public static boolean isLastComponentEmpty(final Name name) {
        return name.isEmpty() || getLastComponent(name).equals("");
    }

    public static NameNotFoundException nameNotFoundException(final String name, final Name contextName) {
        return new NameNotFoundException(String.format("Name '%s' not found in context '%s'", name, contextName.toString()));
    }

    public static NameAlreadyBoundException nameAlreadyBoundException(final Name name) throws NameAlreadyBoundException {
        throw new NameAlreadyBoundException(name.toString());
    }

    public static InvalidNameException emptyNameException() {
        return new InvalidNameException("An empty name is not allowed");
    }

    public static NotContextException notAContextException(Name name) {
        return new NotContextException(name.toString());
    }

    public static NamingException namingException(final String message, final Throwable cause) {
        final NamingException exception = new NamingException(message);
        exception.setRootCause(cause);
        return exception;
    }

    public static NamingException namingException(final String message, final Throwable cause, final Name remainingName) {
        final NamingException exception = namingException(message, cause);
        exception.setRemainingName(remainingName);
        return exception;
    }

    public static CannotProceedException cannotProceedException(final Object resolvedObject, final Name remainingName) {
        final CannotProceedException cpe = new CannotProceedException();
        cpe.setResolvedObj(resolvedObject);
        cpe.setRemainingName(remainingName);
        return cpe;
    }

    private static final Name EMPTY_NAME = new CompositeName();

    public static Name emptyName() {
        return cast(EMPTY_NAME.clone());
    }

    public static <T> NamingEnumeration<T> namingEnumeration(final Collection<T> collection) {
        final Iterator<T> iterator = collection.iterator();
        return new NamingEnumeration<T>() {
            public T next() {
                return nextElement();
            }

            public boolean hasMore() {
                return hasMoreElements();
            }

            public void close() {
            }

            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            public T nextElement() {
                return iterator.next();
            }
        };
    }

    private static final Method CLONE = AccessController.doPrivileged(new PrivilegedAction<Method>() {
        public Method run() {
            final Method method;
            try {
                method = Object.class.getDeclaredMethod("clone");
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
            method.setAccessible(true);
            return method;
        }
    });

    @SuppressWarnings("unchecked")
    public static <T extends Cloneable> T clone(T object) throws NamingException {
        if(object == null) return null;
        try {
            return (T)CLONE.invoke(object);
        } catch (IllegalAccessException e) {
            throw namingException("Failed to clone " + object, e);
        } catch (InvocationTargetException e) {
            throw namingException("Failed to clone " + object, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast(final Object object) {
        return (T)object;
    }

    public static Reference asReference(final Object object) {
        final Reference reference = cast(object);
        return reference;
    }

    public static Referenceable asReferenceable(final Object object) {
        final Referenceable referenceable = cast(object);
        return referenceable;
    }
}
