/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.client.hotrod.ProtocolVersion;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.ModulesServiceConfigurator;
import org.jboss.as.clustering.controller.PropertiesAttributeDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanBindingFactory;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanExtension;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanSubsystemModel;
import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ModuleNameValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.Module;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.ServiceValueExecutorRegistry;

/**
 * /subsystem=infinispan/remote-cache-container=X
 *
 * @author Radoslav Husar
 */
public class RemoteCacheContainerResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfigurator {

    public static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String containerName) {
        return PathElement.pathElement("remote-cache-container", containerName);
    }

    static final RuntimeCapability<Void> REMOTE_CACHE_CONTAINER = RuntimeCapability.Builder.of(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER).build();
    static final RuntimeCapability<Void> REMOTE_CACHE_CONTAINER_CONFIGURATION = RuntimeCapability.Builder.of(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER_CONFIGURATION).build();
    private static final RuntimeCapability<Void> REMOTE_CACHE_CONTAINER_MODULES = RuntimeCapability.Builder.of(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER_MODULES).build();

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        CONNECTION_TIMEOUT("connection-timeout", ModelType.INT, new ModelNode(60000)),
        DEFAULT_REMOTE_CLUSTER("default-remote-cluster", ModelType.STRING, null) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false).setCapabilityReference(CapabilityReference.builder(REMOTE_CACHE_CONTAINER_CONFIGURATION, RemoteClusterResourceDefinition.SERVICE_DESCRIPTOR).withParentPath(WILDCARD_PATH).build());
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
        PROTOCOL_VERSION("protocol-version", ModelType.STRING, new ModelNode(ProtocolVersion.PROTOCOL_VERSION_41.toString())) {
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
                return builder.setElementValidator(ModuleNameValidator.INSTANCE);
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

    public static final Set<PathElement> REQUIRED_CHILDREN = Stream.concat(Set.of(SecurityResourceDefinition.PATH).stream(), EnumSet.allOf(ClientThreadPoolResourceDefinition.class).stream().map(ClientThreadPoolResourceDefinition::getPathElement)).collect(Collectors.toSet());

    private final ServiceValueExecutorRegistry<RemoteCacheContainer> registry = ServiceValueExecutorRegistry.newInstance();

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
                .addCapabilities(List.of(REMOTE_CACHE_CONTAINER, REMOTE_CACHE_CONTAINER_CONFIGURATION, REMOTE_CACHE_CONTAINER_MODULES))
                .addRequiredChildren(REQUIRED_CHILDREN)
                .setResourceTransformation(RemoteCacheContainerResource::new)
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        new ConnectionPoolResourceDefinition().register(registration);
        new RemoteClusterResourceDefinition(this.registry).register(registration);
        new SecurityResourceDefinition().register(registration);

        for (ClientThreadPoolResourceDefinition pool : EnumSet.allOf(ClientThreadPoolResourceDefinition.class)) {
            pool.register(registration);
        }

        if (registration.isRuntimeOnlyRegistrationValid()) {
            new MetricHandler<>(new RemoteCacheContainerMetricExecutor(this.registry), RemoteCacheContainerMetric.class).register(registration);

            new RemoteCacheResourceDefinition(this.registry).register(registration);
        }

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        Module defaultModule = Module.forClass(RemoteCacheContainer.class);

        ResourceServiceInstaller modulesInstaller = new ModulesServiceConfigurator(REMOTE_CACHE_CONTAINER_MODULES, ListAttribute.MODULES.getDefinition(), List.of(defaultModule)).configure(context, model);

        ResourceServiceInstaller captureInstaller = this.registry.capture(ServiceDependency.on(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER, name));
        ResourceServiceInstaller configurationInstaller = RemoteCacheContainerConfigurationServiceConfigurator.INSTANCE.configure(context, model);
        ResourceServiceInstaller containerInstaller = RemoteCacheContainerServiceConfigurator.INSTANCE.configure(context, model);

        ResourceServiceInstaller bindingInstaller = new BinderServiceInstaller(InfinispanBindingFactory.createRemoteCacheContainerBinding(name), context.getCapabilityServiceName(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER, name));

        return ResourceServiceInstaller.combine(modulesInstaller, captureInstaller, configurationInstaller, containerInstaller, bindingInstaller);
    }
}
