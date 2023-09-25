/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming;


/**
 * A {@link ManagedReferenceFactory} which knows the class name of its {@link ManagedReference} object instance. This type of
 * {@link ManagedReferenceFactory} should be used for JNDI bindings, the {@link ServiceBasedNamingStore} relies on it to provide
 * proper support for {@link javax.naming.Context} list operations.
 *
 * @author Eduardo Martins
 *
 */
public interface ContextListManagedReferenceFactory extends ManagedReferenceFactory {

    String DEFAULT_INSTANCE_CLASS_NAME = Object.class.getName();

    /**
     * Retrieves the reference's object instance class name.
     *
     * If it's impossible to obtain such data, the factory should return the static attribute DEFAULT_INSTANCE_CLASS_NAME,
     * exposed by this interface.
     *
     * @return
     */
    String getInstanceClassName();
}
