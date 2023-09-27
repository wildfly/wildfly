/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;

/**
 * Factory for generating a {@link ServiceName} for a requirement.
 * @author Paul Ferraro
 */
public interface ServiceNameFactory {
    /**
     * Creates a {@link ServiceName} appropriate for the specified name.
     * @param context an operation context
     * @param name a potentially null name
     * @return a {@link ServiceName}
     */
    ServiceName getServiceName(OperationContext context);

    /**
     * Creates a {@link ServiceName} appropriate for the specified name.
     * @param support support for capability services
     * @param name a potentially null name
     * @return a {@link ServiceName}
     */
    ServiceName getServiceName(CapabilityServiceSupport support);
}
