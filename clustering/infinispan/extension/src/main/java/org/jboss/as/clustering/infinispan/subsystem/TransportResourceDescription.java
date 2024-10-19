/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;

import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.ResourceDescriptor;

/**
 * @author Paul Ferraro
 */
public interface TransportResourceDescription extends CacheContainerComponentResourceDescription<TransportConfiguration, TransportConfigurationBuilder> {

    static PathElement pathElement(String value) {
        return PathElement.pathElement("transport", value);
    }

    static final UnaryServiceDescriptor<TransportConfiguration> SERVICE_DESCRIPTOR = CacheContainerComponentResourceDescription.createServiceDescriptor(pathElement(PathElement.WILDCARD_VALUE), TransportConfiguration.class);
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();

    @Override
    default PathElement getPathKey() {
        return pathElement(PathElement.WILDCARD_VALUE);
    }

    String resolveGroupName(OperationContext context, ModelNode model) throws OperationFailedException;

    @Override
    default UnaryServiceDescriptor<TransportConfiguration> getServiceDescriptor() {
        return SERVICE_DESCRIPTOR;
    }

    @Override
    default RuntimeCapability<Void> getCapability() {
        return CAPABILITY;
    }

    @Override
    default ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        RuntimeCapability<Void> commandDispatcherFactory = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).setAllowMultipleRegistrations(true).build();
        RuntimeCapability<Void> group = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.GROUP).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).setAllowMultipleRegistrations(true).build();

        return CacheContainerComponentResourceDescription.super.apply(builder).addCapabilities(List.of(commandDispatcherFactory, group));
    }
}
