/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming;

/**
 * A {@link ManagedReferenceFactory} which supports JNDI View lookups, which are done without the proper invocation context set.
 *
 * @author Eduardo Martins
 *
 */
public interface JndiViewManagedReferenceFactory extends ManagedReferenceFactory {

    String DEFAULT_JNDI_VIEW_INSTANCE_VALUE = "?";

    /**
     * Retrieves the reference's object instance JNDI View value.
     *
     * If it's not possible to obtain such data, the factory should return the static attribute
     * DEFAULT_JNDI_VIEW_INSTANCE_VALUE, exposed by this interface.
     *
     * @return
     */
    String getJndiViewInstanceValue();

}
