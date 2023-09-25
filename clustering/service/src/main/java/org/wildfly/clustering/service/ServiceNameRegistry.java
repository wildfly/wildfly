/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

import org.jboss.msc.service.ServiceName;

/**
 * Registry of services names for a set of requirements.
 * @author Paul Ferraro
 * @deprecated To be removed without replacement.
 */
@Deprecated(forRemoval = true)
public interface ServiceNameRegistry<R extends Requirement> {
    /**
     * Returns the service name for the specified requirement
     * @param requirement a requirement
     * @return a service name.
     */
    ServiceName getServiceName(R requirement);
}
