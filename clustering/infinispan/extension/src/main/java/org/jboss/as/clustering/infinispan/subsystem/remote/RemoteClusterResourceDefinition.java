/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.OperationHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.RestartParentResourceRegistrar;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanExtension;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.service.BinaryRequirement;
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

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        SOCKET_BINDINGS("socket-bindings", new CapabilityReference(Capability.REMOTE_CLUSTER, CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING)),
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

    enum Requirement implements BinaryRequirement {
        REMOTE_CLUSTER("org.wildfly.clustering.infinispan.remote-cache-container.remote-cluster", Void.class),
        ;
        private final String name;
        private final Class<?> type;

        Requirement(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Class<?> getType() {
            return this.type;
        }
    }

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        REMOTE_CLUSTER("org.wildfly.clustering.infinispan.remote-cache-container.remote-cluster"),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name) {
            this.definition = RuntimeCapability.Builder.of(name, true).setDynamicNameMapper(BinaryCapabilityNameResolver.PARENT_CHILD).build();
        }

        @Override
        public RuntimeCapability<Void> getDefinition() {
            return this.definition;
        }
    }

    private final ResourceServiceConfiguratorFactory serviceConfiguratorFactory;
    private final FunctionExecutorRegistry<RemoteCacheContainer> executors;

    RemoteClusterResourceDefinition(ResourceServiceConfiguratorFactory serviceConfiguratorFactory, FunctionExecutorRegistry<RemoteCacheContainer> executors) {
        super(WILDCARD_PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
        this.serviceConfiguratorFactory = serviceConfiguratorFactory;
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(Capability.class);
        new RestartParentResourceRegistrar(this.serviceConfiguratorFactory, descriptor).register(registration);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            new OperationHandler<>(new RemoteClusterOperationExecutor(this.executors), RemoteClusterOperation.class).register(registration);
        }

        return registration;
    }
}
