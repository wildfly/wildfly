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
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import org.jboss.as.naming.logging.NamingLogger;

import java.util.Collection;
import java.util.Iterator;

/**
 * Common utility functions used by the naming implementation.
 *
 * @author John E. Bailey
 */
public class NamingUtils {
    private NamingUtils() {}

    /**
     * Create a subcontext including any intermediate contexts.
     * @param ctx the parent JNDI Context under which value will be bound
     * @param name the name relative to ctx of the subcontext.
     * @return The new or existing JNDI subcontext
     * @throws NamingException on any JNDI failure
     */
    private static Context createSubcontext(Context ctx, final Name name) throws NamingException {
        Context subctx = ctx;
        for (int pos = 0; pos < name.size(); pos++) {
            final String ctxName = name.get(pos);
            try {
                subctx = (Context) ctx.lookup(ctxName);
            }
            catch (NameNotFoundException e) {
                subctx = ctx.createSubcontext(ctxName);
            }
            // The current subctx will be the ctx for the next name component
            ctx = subctx;
        }
        return subctx;
    }

    /**
     * Get the last component of a name.
     *
     * @param name the name
     * @return the last component
     */
    public static String getLastComponent(final Name name) {
        if(name.size() > 0)
            return name.get(name.size() - 1);
        return "";
    }

    /**
     * Determine if a name is empty, or if ot contains only one component which is the empty string.
     *
     * @param name the name
     * @return {@code true} if the name is empty or contains one empty component
     */
    public static boolean isEmpty(final Name name) {
        return name.isEmpty() || (name.size() == 1 && "".equals(name.get(0)));
    }

    /**
     * Determine whether the last component is empty.
     *
     * @param name the name to test
     * @return {@code true} if the last component is empty, or if the name is empty
     */
    public static boolean isLastComponentEmpty(final Name name) {
        return name.isEmpty() || getLastComponent(name).equals("");
    }

    /**
     * Create a name-not-found exception.
     *
     * @param name the name
     * @param contextName the context name
     * @return the exception
     */
    public static NameNotFoundException nameNotFoundException(final String name, final Name contextName) {
        return NamingLogger.ROOT_LOGGER.nameNotFoundInContext(name, contextName);
    }

    /**
     * Create a name-already-bound exception.
     *
     * @param name the name
     * @return the exception
     */
    public static NameAlreadyBoundException nameAlreadyBoundException(final Name name) {
        return new NameAlreadyBoundException(name.toString());
    }

    /**
     * Create an invalid name exception for an empty name.
     *
     * @return the exception
     */
    public static InvalidNameException emptyNameException() {
        return NamingLogger.ROOT_LOGGER.emptyNameNotAllowed();
    }

    /**
     * Return a not-context exception for a name.
     *
     * @param name the name
     * @return the exception
     */
    public static NotContextException notAContextException(Name name) {
        return new NotContextException(name.toString());
    }

    /**
     * Return a general naming exception with a root cause.
     *
     * @param message the message
     * @param cause the exception cause, or {@code null} for none
     * @return the exception
     */
    public static NamingException namingException(final String message, final Throwable cause) {
        final NamingException exception = new NamingException(message);
        if (cause != null) exception.initCause(cause);
        return exception;
    }

    /**
     * Return a general naming exception with a root cause and a remaining name field.
     *
     * @param message the message
     * @param cause the exception cause, or {@code null} for none
     * @param remainingName the remaining name
     * @return the exception
     */
    public static NamingException namingException(final String message, final Throwable cause, final Name remainingName) {
        final NamingException exception = namingException(message, cause);
        exception.setRemainingName(remainingName);
        return exception;
    }

    /**
     * Return a cannot-proceed exception.
     *
     * @param resolvedObject the resolved object
     * @param remainingName the remaining name
     * @return the exception
     */
    public static CannotProceedException cannotProceedException(final Object resolvedObject, final Name remainingName) {
        final CannotProceedException cpe = new CannotProceedException();
        cpe.setResolvedObj(resolvedObject);
        cpe.setRemainingName(remainingName);
        return cpe;
    }

    /**
     * Return a naming enumeration over a collection.
     *
     * @param collection the collection
     * @param <T> the member type
     * @return the enumeration
     */
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

    /**
     * Rebind val to name in ctx, and make sure that all intermediate contexts exist
     *
     * @param ctx the parent JNDI Context under which value will be bound
     * @param name the name relative to ctx where value will be bound
     * @param value the value to bind.
     * @throws NamingException for any error
     */
    public static void rebind(final Context ctx, final String name, final Object value) throws NamingException {
       final Name n = ctx.getNameParser("").parse(name);
       rebind(ctx, n, value);
    }

    /**
     * Rebind val to name in ctx, and make sure that all intermediate contexts exist
     *
     * @param ctx the parent JNDI Context under which value will be bound
     * @param name the name relative to ctx where value will be bound
     * @param value the value to bind.
     * @throws NamingException for any error
     */
    public static void rebind(final Context ctx, final Name name, final Object value) throws NamingException {
       final int size = name.size();
       final String atom = name.get(size - 1);
       final Context parentCtx = createSubcontext(ctx, name.getPrefix(size - 1));
       parentCtx.rebind(atom, value);
    }

    /**
     * Unbinds a name from ctx, and removes parents if they are empty
     *
     * @param ctx  the parent JNDI Context under which the name will be unbound
     * @param name The name to unbind
     * @throws NamingException for any error
     */
    public static void unbind(Context ctx, String name) throws NamingException {
        unbind(ctx, ctx.getNameParser("").parse(name));
    }

    /**
     * Unbinds a name from ctx, and removes parents if they are empty
     *
     * @param ctx  the parent JNDI Context under which the name will be unbound
     * @param name The name to unbind
     * @throws NamingException for any error
     */
    public static void unbind(Context ctx, Name name) throws NamingException {
        ctx.unbind(name); //unbind the end node in the name
        int sz = name.size();
        // walk the tree backwards, stopping at the domain
        while (--sz > 0) {
            Name pname = name.getPrefix(sz);
            try {
                ctx.destroySubcontext(pname);
            }
            catch (NamingException e) {
                //log.trace("Unable to remove context " + pname, e);
                break;
            }
        }
    }
}
