/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye;

/**
 * Encapsulates a generic registry of objects.
 * @author Paul Ferraro
 * @param <T> The target object type of this registry
 */
public interface Registry<T> {
    /**
     * Registers the specified object with this registry
     * @param name the object name
     * @param object the object to register
     */
    void register(String name, T object);

    /**
     * Unregisters the specified object from this registry
     * @param name the object name
     */
    void unregister(String name);
}
