/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

import org.jboss.msc.service.ServiceName;

/**
 * Provides a service name.
 * @author Paul Ferraro
 */
public interface ServiceNameProvider {
    /**
     * Returns the associated service name
     * @return a service name
     */
    ServiceName getServiceName();
}
