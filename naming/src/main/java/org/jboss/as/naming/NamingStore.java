/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming;

import java.util.List;

import javax.naming.Binding;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingException;
import javax.naming.event.NamingListener;

/**
 * Interface to layout a contract for naming entry back-end storage.  This will be used by {@code NamingContext} instances
 * to manage naming entries.
 *
 * @author John E. Bailey
 * @author Eduardo Martins
 */
public interface NamingStore {

    /**
     * Retrieves the store's base name, which is the prefix for the absolute name of each entry in the store.
     * @return
     * @throws NamingException
     */
    Name getBaseName() throws NamingException;

    /**
     * Look up an object from the naming store.  An entry for this name must already exist.
     *
     * @param name The entry name
     * @return The object from the store.
     * @throws NamingException If any errors occur.
     */
    Object lookup(Name name) throws NamingException;

    /**
     * Look up an object from the naming store.  An entry for this name must already exist.
     *
     * @param name The entry name
     * @param dereference if true indicates that managed references should retrieve the instance.
     * @return The object from the store.
     * @throws NamingException If any errors occur.
     */
    Object lookup(Name name, boolean dereference) throws NamingException;

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
