/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.stream.Collectors;

import org.infinispan.commons.configuration.Builder;
import org.jboss.as.controller.PathElement;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.service.ServiceDependency;

public interface ComponentResourceDescription<C, B extends Builder<C>> extends ResourceCapabilityDescription<C>, ResourceModelResolver<ServiceDependency<B>> {

    static PathElement pathElement(String name) {
        return PathElement.pathElement("component", name);
    }

    default InfinispanSubsystemModel getDeprecation() {
        return null;
    }

    @Override
    default ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return builder.addCapability(this.getCapability()).addAttributes(this.getAttributes().collect(Collectors.toUnmodifiableList()));
    }
}