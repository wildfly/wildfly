/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;


import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;

/**
 * Factory for generating a {@link ServiceName} for a binary requirement.
 * @author Paul Ferraro
 */
public interface BinaryServiceNameFactory {
    /**
     * Creates a {@link ServiceName} appropriate for the specified name.
     * @param context an operation context
     * @param parent a parent resource name
     * @param child a child resource name
     * @return a {@link ServiceName}
     */
    ServiceName getServiceName(OperationContext context, String parent, String child);

    /**
     * Creates a {@link ServiceName} appropriate for the specified name.
     * @param support support for capability services
     * @param parent a parent resource name
     * @param child a child resource name
     * @return a {@link ServiceName}
     */
    ServiceName getServiceName(CapabilityServiceSupport support, String parent, String child);

    /**
     * Creates a {@link ServiceName} appropriate for the address of the specified {@link OperationContext}
     * @param context an operation context
     * @param resolver a capability name resolver
     * @return a {@link ServiceName}
     */
    default ServiceName getServiceName(OperationContext context, BinaryCapabilityNameResolver resolver) {
        String[] parts = resolver.apply(context.getCurrentAddress());
        return this.getServiceName(context.getCapabilityServiceSupport(), parts[0], parts[1]);
    }
}
