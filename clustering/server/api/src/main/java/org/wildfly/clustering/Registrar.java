/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering;

/**
 * Defines the contract for registration-capable objects, e.g. for listener registration.
 * @author Paul Ferraro
 * @param <T> the type of object to be registered
 * @deprecated Replaced by {@link org.wildfly.clustering.server.Registrar}.
 */
@Deprecated(forRemoval = true)
public interface Registrar<T> {
    /**
     * Registers an object.  The object is unregistered when the generated {@link Registration} is closed.
     * @param object an object to register
     * @return an object registration.
     */
    Registration register(T object);
}
