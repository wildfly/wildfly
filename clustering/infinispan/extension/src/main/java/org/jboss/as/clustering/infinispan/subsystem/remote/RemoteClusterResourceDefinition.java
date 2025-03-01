/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.List;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.OperationHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanExtension;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.network.OutboundSocketBinding;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * /subsystem=infinispan/remote-cache-container=X/remote-cluster=Y
 *
 * @author Radoslav Husar
 */
public class RemoteClusterResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    public static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("remote-cluster", name);
    }

    static final BinaryServiceDescriptor<Void> SERVICE_DESCRIPTOR = BinaryServiceDescriptor.of(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER_CONFIGURATION.getName() + "." + WILDCARD_PATH.getKey(), Void.class);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).build();

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        SOCKET_BINDINGS("socket-bindings", CapabilityReference.builder(CAPABILITY, OutboundSocketBinding.SERVICE_DESCRIPTOR).build()),
        ;

        private final AttributeDefinition definition;

        Attribute(String name, CapabilityReferenceRecorder reference) {
            this.definition = new StringListAttributeDefinition.Builder(name)
                    .setAllowExpression(false)
                    .setRequired(true)
                    .setCapabilityReference(reference)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    private final FunctionExecutorRegistry<RemoteCacheContainer> executors;

    RemoteClusterResourceDefinition(FunctionExecutorRegistry<RemoteCacheContainer> executors) {
        super(WILDCARD_PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(List.of(CAPABILITY));
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.restartParent(ResourceOperationRuntimeHandler.configureService(RemoteCacheContainerConfigurationServiceConfigurator.INSTANCE));
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            new OperationHandler<>(new RemoteClusterOperationExecutor(this.executors), RemoteClusterOperation.class).register(registration);
        }

        return registration;
    }
}
