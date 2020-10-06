/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.EnumSet;
import java.util.function.UnaryOperator;

import org.infinispan.client.hotrod.ProtocolVersion;
import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.PropertiesAttributeDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.ServiceValueExecutorRegistry;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.UnaryRequirementCapability;
import org.jboss.as.clustering.controller.validation.EnumValidator;
import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidatorBuilder;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanExtension;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanModel;
import org.jboss.as.clustering.infinispan.subsystem.ThreadPoolResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker.DiscardAttributeValueChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.client.InfinispanClientRequirement;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * /subsystem=infinispan/remote-cache-container=X
 *
 * @author Radoslav Husar
 */
public class RemoteCacheContainerResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfiguratorFactory {

    public static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String containerName) {
        return PathElement.pathElement("remote-cache-container", containerName);
    }

    public enum Capability implements CapabilityProvider {
        CONTAINER(InfinispanClientRequirement.REMOTE_CONTAINER),
        CONFIGURATION(InfinispanClientRequirement.REMOTE_CONTAINER_CONFIGURATION),
        ;

        private final org.jboss.as.clustering.controller.Capability capability;

        Capability(UnaryRequirement requirement) {
            this.capability = new UnaryRequirementCapability(requirement);
        }

        @Override
        public org.jboss.as.clustering.controller.Capability getCapability() {
            return this.capability;
        }
    }

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        CONNECTION_TIMEOUT("connection-timeout", ModelType.INT, new ModelNode(60000)),
        DEFAULT_REMOTE_CLUSTER("default-remote-cluster", ModelType.STRING, null) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false).setCapabilityReference(new CapabilityReference(Capability.CONFIGURATION, RemoteClusterResourceDefinition.Requirement.REMOTE_CLUSTER, WILDCARD_PATH));
            }
        },
        KEY_SIZE_ESTIMATE("key-size-estimate", ModelType.INT, new ModelNode(64)),
        MAX_RETRIES("max-retries", ModelType.INT, new ModelNode(10)),
        MODULE("module", ModelType.STRING, new ModelNode("org.jboss.as.clustering.infinispan")) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new ModuleIdentifierValidatorBuilder().configure(builder).build());
            }
        },
        PROPERTIES("properties"),
        PROTOCOL_VERSION("protocol-version", ModelType.STRING, new ModelNode(ProtocolVersion.PROTOCOL_VERSION_30.toString())) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new EnumValidator<>(ProtocolVersion.class, EnumSet.complementOf(EnumSet.of(ProtocolVersion.PROTOCOL_VERSION_AUTO))));
            }
        },
        SOCKET_TIMEOUT("socket-timeout", ModelType.INT, new ModelNode(60000)),
        STATISTICS_ENABLED(ModelDescriptionConstants.STATISTICS_ENABLED, ModelType.BOOLEAN, ModelNode.FALSE),
        TCP_NO_DELAY("tcp-no-delay", ModelType.BOOLEAN, ModelNode.TRUE),
        TCP_KEEP_ALIVE("tcp-keep-alive", ModelType.BOOLEAN, ModelNode.FALSE),
        VALUE_SIZE_ESTIMATE("value-size-estimate", ModelType.INT, new ModelNode(512)),
        ;

        private final AttributeDefinition definition;

        Attribute(String name) {
            this.definition = new PropertiesAttributeDefinition.Builder(name)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(defaultValue == null)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder;
        }
    }

    @SuppressWarnings("deprecation")
    public static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        if (InfinispanModel.VERSION_7_0_0.requiresTransformation(version)) {
            parent.rejectChildResource(RemoteCacheContainerResourceDefinition.WILDCARD_PATH);
        } else {
            ResourceTransformationDescriptionBuilder builder = parent.addChildResource(RemoteCacheContainerResourceDefinition.WILDCARD_PATH);

            if (InfinispanModel.VERSION_13_0_0.requiresTransformation(version)) {
                builder.getAttributeBuilder()
                        .setDiscard(DiscardAttributeChecker.UNDEFINED, Attribute.PROPERTIES.getDefinition())
                        .addRejectCheck(RejectAttributeChecker.DEFINED, Attribute.PROPERTIES.getDefinition())
                        .end();
            }
            if (InfinispanModel.VERSION_12_0_0.requiresTransformation(version)) {
                builder.getAttributeBuilder().setValueConverter(AttributeConverter.DEFAULT_VALUE, Attribute.PROTOCOL_VERSION.getName());
            }
            if (InfinispanModel.VERSION_11_0_0.requiresTransformation(version)) {
                builder.getAttributeBuilder()
                        .setDiscard(new DiscardAttributeValueChecker(false, true, ModelNode.FALSE), Attribute.STATISTICS_ENABLED.getDefinition())
                        .addRejectCheck(RejectAttributeChecker.DEFINED, Attribute.STATISTICS_ENABLED.getDefinition())
                        .end();
            }

            RemoteCacheContainerMetric.buildTransformation(version, builder);

            ConnectionPoolResourceDefinition.buildTransformation(version, builder);
            SecurityResourceDefinition.buildTransformation(version, builder);
            RemoteTransactionResourceDefinition.buildTransformation(version, builder);
            NoNearCacheResourceDefinition.buildTransformation(version, builder);
            InvalidationNearCacheResourceDefinition.buildTransformation(version, builder);
            RemoteClusterResourceDefinition.buildTransformation(version, builder);

            ThreadPoolResourceDefinition.CLIENT.buildTransformation(builder, version);

            RemoteCacheResourceDefinition.buildTransformation(version, builder);
        }
    }

    public RemoteCacheContainerResourceDefinition() {
        super(WILDCARD_PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
    }

    @SuppressWarnings("deprecation")
    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(Capability.class)
                .addRequiredChildren(ConnectionPoolResourceDefinition.PATH, ThreadPoolResourceDefinition.CLIENT.getPathElement(), SecurityResourceDefinition.PATH, RemoteTransactionResourceDefinition.PATH)
                .addRequiredSingletonChildren(NoNearCacheResourceDefinition.PATH)
                .setResourceTransformation(RemoteCacheContainerResource::new)
                ;
        ServiceValueExecutorRegistry<RemoteCacheContainer> executors = new ServiceValueExecutorRegistry<>();
        ResourceServiceHandler handler = new RemoteCacheContainerServiceHandler(this, executors);
        new SimpleResourceRegistration(descriptor, handler).register(registration);

        new ConnectionPoolResourceDefinition().register(registration);
        new RemoteClusterResourceDefinition(this, executors).register(registration);
        new SecurityResourceDefinition().register(registration);
        new RemoteTransactionResourceDefinition().register(registration);

        new InvalidationNearCacheResourceDefinition().register(registration);
        new NoNearCacheResourceDefinition().register(registration);

        ThreadPoolResourceDefinition.CLIENT.register(registration);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            new MetricHandler<>(new RemoteCacheContainerMetricExecutor(executors), RemoteCacheContainerMetric.class).register(registration);

            new RemoteCacheResourceDefinition(executors).register(registration);
        }

        return registration;
    }

    @Override
    public ResourceServiceConfigurator createServiceConfigurator(PathAddress address) {
        return new RemoteCacheContainerConfigurationServiceConfigurator(address);
    }
}
