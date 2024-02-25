/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import org.infinispan.client.hotrod.ProtocolVersion;
import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.PropertiesAttributeDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.controller.UnaryRequirementCapability;
import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidatorBuilder;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanExtension;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanSubsystemModel;
import org.jboss.as.clustering.infinispan.subsystem.ThreadPoolResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.service.InfinispanClientRequirement;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.subsystem.service.capture.ServiceValueExecutorRegistry;

/**
 * /subsystem=infinispan/remote-cache-container=X
 *
 * @author Radoslav Husar
 */
public class RemoteCacheContainerResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

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
        MARSHALLER("marshaller", ModelType.STRING, new ModelNode(HotRodMarshallerFactory.LEGACY.name())) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new ParameterValidator() {
                    private final ParameterValidator validator = EnumValidator.create(HotRodMarshallerFactory.class);

                    @Override
                    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
                        this.validator.validateParameter(parameterName, value);
                        if (!value.isDefined() || value.equals(MARSHALLER.getDefinition().getDefaultValue())) {
                            InfinispanLogger.ROOT_LOGGER.marshallerEnumValueDeprecated(parameterName, HotRodMarshallerFactory.LEGACY, EnumSet.complementOf(EnumSet.of(HotRodMarshallerFactory.LEGACY)));
                        }
                    }
                });
            }
        },
        MAX_RETRIES("max-retries", ModelType.INT, new ModelNode(10)),
        PROPERTIES("properties"),
        PROTOCOL_VERSION("protocol-version", ModelType.STRING, new ModelNode(ProtocolVersion.PROTOCOL_VERSION_40.toString())) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new org.jboss.as.controller.operations.validation.EnumValidator<>(ProtocolVersion.class, EnumSet.complementOf(EnumSet.of(ProtocolVersion.PROTOCOL_VERSION_AUTO))));
            }
        },
        SOCKET_TIMEOUT("socket-timeout", ModelType.INT, new ModelNode(60000)),
        STATISTICS_ENABLED(ModelDescriptionConstants.STATISTICS_ENABLED, ModelType.BOOLEAN, ModelNode.FALSE),
        TCP_NO_DELAY("tcp-no-delay", ModelType.BOOLEAN, ModelNode.TRUE),
        TCP_KEEP_ALIVE("tcp-keep-alive", ModelType.BOOLEAN, ModelNode.FALSE),
        TRANSACTION_TIMEOUT("transaction-timeout", ModelType.LONG, new ModelNode(TimeUnit.MINUTES.toMillis(1))) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setMeasurementUnit(MeasurementUnit.MILLISECONDS);
            }
        },
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

    public enum ListAttribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<StringListAttributeDefinition.Builder> {
        MODULES("modules") {
            @Override
            public StringListAttributeDefinition.Builder apply(StringListAttributeDefinition.Builder builder) {
                return builder.setElementValidator(new ModuleIdentifierValidatorBuilder().configure(builder).build());
            }
        },
        ;
        private final AttributeDefinition definition;

        ListAttribute(String name) {
            this.definition = this.apply(new StringListAttributeDefinition.Builder(name)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public StringListAttributeDefinition.Builder apply(StringListAttributeDefinition.Builder builder) {
            return builder;
        }
    }

    public enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        KEY_SIZE_ESTIMATE("key-size-estimate", ModelType.INT, InfinispanSubsystemModel.VERSION_15_0_0) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(new ModelNode(64));
            }
        },
        VALUE_SIZE_ESTIMATE("value-size-estimate", ModelType.INT, InfinispanSubsystemModel.VERSION_15_0_0) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(new ModelNode(512));
            }
        }
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, InfinispanSubsystemModel deprecation) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDeprecated(deprecation.getVersion())
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
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

    public static final Set<PathElement> REQUIRED_CHILDREN = Set.of(ConnectionPoolResourceDefinition.PATH, ThreadPoolResourceDefinition.CLIENT.getPathElement(), SecurityResourceDefinition.PATH);

    public RemoteCacheContainerResourceDefinition() {
        super(WILDCARD_PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addAttributes(ListAttribute.class)
                .addAttributes(DeprecatedAttribute.class)
                .addCapabilities(Capability.class)
                .addRequiredChildren(REQUIRED_CHILDREN)
                .setResourceTransformation(RemoteCacheContainerResource::new)
                ;
        ServiceValueExecutorRegistry<RemoteCacheContainer> executors = ServiceValueExecutorRegistry.newInstance();
        ResourceServiceConfiguratorFactory factory = RemoteCacheContainerConfigurationServiceConfigurator::new;
        ResourceServiceHandler handler = new RemoteCacheContainerServiceHandler(factory, executors);
        new SimpleResourceRegistrar(descriptor, handler).register(registration);

        new ConnectionPoolResourceDefinition().register(registration);
        new RemoteClusterResourceDefinition(factory, executors).register(registration);
        new SecurityResourceDefinition().register(registration);

        ThreadPoolResourceDefinition.CLIENT.register(registration);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            new MetricHandler<>(new RemoteCacheContainerMetricExecutor(executors), RemoteCacheContainerMetric.class).register(registration);

            new RemoteCacheResourceDefinition(executors).register(registration);
        }

        return registration;
    }
}
