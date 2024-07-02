/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;
import java.util.function.UnaryOperator;

import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

/**
 * @author Paul Ferraro
 */
public abstract class StoreWriteResourceDefinition extends ComponentResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String value) {
        return PathElement.pathElement("write", value);
    }

    static final BinaryServiceDescriptor<AsyncStoreConfiguration> SERVICE_DESCRIPTOR = serviceDescriptor(List.of(StoreResourceDefinition.WILDCARD_PATH, WILDCARD_PATH), AsyncStoreConfiguration.class);
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setAllowMultipleRegistrations(true).setDynamicNameMapper(BinaryCapabilityNameResolver.GREATGRANDPARENT_GRANDPARENT).build();

    private final UnaryOperator<ResourceDescriptor> configurator;

    StoreWriteResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator) {
        super(path, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(path));
        this.configurator = configurator;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addCapabilities(List.of(CAPABILITY))
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        return registration;
    }
}
