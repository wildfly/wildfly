/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.msc.service.ServiceName;

/**
 * A registry of service values.
 * @author Paul Ferraro
 */
public interface ServiceValueRegistry<T> {

    /**
     * Adds a service to this registry.
     * @param name a service name
     * @return a mechanism for capturing the value of the service
     */
    ServiceValueCaptor<T> add(ServiceName name);

    /**
     * Removes a service from this registry
     * @param name a service name
     */
    ServiceValueCaptor<T> remove(ServiceName name);
}
