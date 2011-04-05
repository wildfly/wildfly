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

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingException;
import javax.naming.event.NamingListener;
import java.util.List;

/**
 * Interface to layout a contract for naming entry back-end storage.  This will be used by {@code NamingContext} instances
 * to manage naming entries.
 *
 * @author John E. Bailey
 */
public interface NamingStore {
    /**
     * Bind and object into the naming store, creating parent contexts if needed.  All parent contexts must be
     * created before this can be executed.  The bind object type will be determined by the class of the object being passed in.
     *
     * @param name   The entry name
     * @param object The entry object
     * @throws NamingException If any problems occur
     */
    void bind(Name name, Object object) throws NamingException;

    /**
     * Bind and object into the naming store, creating parent contexts if needed.  All parent contexts must be
     * created before this can be executed.
     *
     * @param name     The entry name
     * @param object   The entry object
     * @param bindType The entry class
     * @throws NamingException If any problems occur
     */
    void bind(Name name, Object object, Class<?> bindType) throws NamingException;

    /**
     * Re-bind and object into the naming store.  All parent contexts must be created before this can be executed.
     * The bind object type will be determined by the class of the object being passed in.
     *
     * @param name     The entry name
     * @param object   The entry object
     * @throws NamingException If any problems occur
     */
    void rebind(Name name, Object object) throws NamingException;

    /**
     * Re-bind and object into the naming store.  All parent contexts must be created before this can be executed.
     *
     * @param name     The entry name
     * @param object   The entry object
     * @param bindType The entry class
     * @throws NamingException If any problems occur
     */
    void rebind(Name name, Object object, Class<?> bindType) throws NamingException;

    /**
     * Unbind an object from the naming store.  An entry for the name must exist.
     *
     * @param name The entry name
     * @throws NamingException If any problems occur
     */
    void unbind(Name name) throws NamingException;

    /**
     * Look up an object from the naming store.  An entry for this name must already exist.
     *
     * @param name The entry name
     * @return The object from the store.
     * @throws NamingException If any errors occur.
     */
    Object lookup(Name name) throws NamingException;

    /**
     * List the NameClassPair instances for the provided name.  An entry for this name must already exist and be bound
     * to a valid context.
     *
     * @param name The entry name
     * @return The NameClassPair instances
     * @throws NamingException If any errors occur
     */
    List<NameClassPair> list(Name name) throws NamingException;

    /**
     * List the binding objects for a specified name.  An entry for this name must already exist and be bound
     * to a valid context.
     *
     * @param name The entry name
     * @return The bindings
     * @throws NamingException If any errors occur
     */
    List<Binding> listBindings(Name name) throws NamingException;

    /**
     * Close the naming store and cleanup any resource used by the store.
     *
     * @throws NamingException If any errors occur
     */
    void close() throws NamingException;

    /**
     * Add a {@code NamingListener} for a specific target and scope.
     *
     * @param target   The target name to add the listener to
     * @param scope    The listener scope
     * @param listener The listener
     */
    void addNamingListener(Name target, int scope, NamingListener listener);

    /**
     * Remove a {@code NamingListener} from all targets and scopes
     *
     * @param listener The listener
     */
    void removeNamingListener(NamingListener listener);
}
