/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.DurationAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.wildfly.service.descriptor.ServiceDescriptor;

/**
 * A resource registration for a scheduled thread pool.
 * @author Paul Ferraro
 */
public interface ScheduledThreadPoolResourceRegistration<C> extends ResourceRegistration {
    ServiceDescriptor<C> getServiceDescriptor();
    RuntimeCapability<Void> getCapability();

    AttributeDefinition getMinThreads();
    DurationAttributeDefinition getKeepAlive();
}
