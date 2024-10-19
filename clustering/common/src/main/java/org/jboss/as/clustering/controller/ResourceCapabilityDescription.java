/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller;

import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.jboss.as.controller.capability.RuntimeCapability;
import org.wildfly.service.descriptor.ServiceDescriptor;
import org.wildfly.subsystem.resource.ResourceDescriptor;

/**
 * Describes a resource that provides a service-based capability.
 * @author Paul Ferraro
 */
public interface ResourceCapabilityDescription<T> extends ResourceDescription, UnaryOperator<ResourceDescriptor.Builder> {

    ServiceDescriptor<T> getServiceDescriptor();

    RuntimeCapability<Void> getCapability();

    @Override
    default ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return builder.addCapability(this.getCapability()).addAttributes(this.getAttributes().collect(Collectors.toList()));
    }
}
