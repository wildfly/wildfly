/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Collection;
import java.util.List;

import org.jboss.as.clustering.controller.DurationAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.wildfly.service.descriptor.ServiceDescriptor;

/**
 * @author Paul Ferraro
 *
 */
public interface ScheduledThreadPoolResourceRegistration<C> extends ResourceRegistration {
    ServiceDescriptor<C> getServiceDescriptor();
    RuntimeCapability<Void> getCapability();

    default Collection<AttributeDefinition> getAttributes() {
        return List.of(this.getMinThreads(), this.getKeepAlive());
    }

    AttributeDefinition getMinThreads();
    DurationAttributeDefinition getKeepAlive();
}
