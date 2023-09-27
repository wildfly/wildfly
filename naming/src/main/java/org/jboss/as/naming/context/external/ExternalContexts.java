/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming.context.external;

import org.jboss.msc.service.ServiceName;

/**
 * The external contexts which exist in Wildfly's Naming subsystem.
 * @author Eduardo Martins
 */
public interface ExternalContexts {

    /**
     * Adds an external context.
     * @param serviceName the external context's service name
     * @throws IllegalArgumentException if there is already an external context with such service name, or if there is already an external context which is a child or parent of the one to add
     */
    void addExternalContext(ServiceName serviceName) throws IllegalArgumentException;

    /**
     * Removes an external context.
     * @param serviceName the external context's service name
     * @return true if an external context with the specified jndi name was found and removed.
     */
    boolean removeExternalContext(ServiceName serviceName);

    /**
     * Retrieves the external context that is a parent of the specified child service name.
     * @param childServiceName a external context's child service name
     * @return null if there is currently no external context, which is a parent of the specified service name.
     */
    ServiceName getParentExternalContext(ServiceName childServiceName);
}
