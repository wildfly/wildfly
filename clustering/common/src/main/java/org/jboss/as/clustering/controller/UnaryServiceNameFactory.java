/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.msc.service.ServiceName;

/**
 * Factory for generating a {@link ServiceName} for a unary requirement.
 * @author Paul Ferraro
 */
public interface UnaryServiceNameFactory {
    /**
     * Creates a {@link ServiceName} appropriate for the specified name.
     * @param context an operation context
     * @param name a potentially null name
     * @return a {@link ServiceName}
     */
    ServiceName getServiceName(OperationContext context, String name);

    /**
     * Creates a {@link ServiceName} appropriate for the specified name.
     * @param support support for capability services
     * @param name a potentially null name
     * @return a {@link ServiceName}
     */
    ServiceName getServiceName(CapabilityServiceSupport support, String name);

    /**
     * Creates a {@link ServiceName} appropriate for the address of the specified {@link OperationContext}
     * @param context an operation context
     * @param resolver a capability name resolver
     * @return a {@link ServiceName}
     */
    default ServiceName getServiceName(OperationContext context, UnaryCapabilityNameResolver resolver) {
        String[] parts = resolver.apply(context.getCurrentAddress());
        return this.getServiceName(context.getCapabilityServiceSupport(), parts[0]);
    }
}
