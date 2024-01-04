/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;

/**
 * A {@code NamingStore} that allows entries to be written as well as retrieved.
 *
 * @author John Bailey
 */
public interface WritableNamingStore extends NamingStore {
    /**
     * Bind and object into the naming store, creating parent contexts if needed.  All parent contexts must be
     * created before this can be executed.  The bind object type will be determined by the class of the object being passed in.
     *
     * @param name   The entry name
     * @param object The entry object
     * @throws javax.naming.NamingException If any problems occur
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
     * @param name   The entry name
     * @param object The entry object
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
     * Create a sub-context for the provided name.
     *
     * @param name    The entry name
     * @return The new sub-context
     * @throws NamingException If any errors occur
     */
    Context createSubcontext(Name name) throws NamingException;


}
