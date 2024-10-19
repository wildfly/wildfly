/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ResourceDescription;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.wildfly.service.descriptor.ServiceDescriptor;
import org.wildfly.subsystem.resource.ResourceDescriptor;

/**
 * @author Paul Ferraro
 *
 */
public interface ResourceCapabilityDescription<T> extends ResourceDescription, UnaryOperator<ResourceDescriptor.Builder> {
    ServiceDescriptor<T> getServiceDescriptor();

    RuntimeCapability<Void> getCapability();

    @Override
    default ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return builder.addAttributes(this.getAttributes().toList()).addCapability(this.getCapability());
    }
}
