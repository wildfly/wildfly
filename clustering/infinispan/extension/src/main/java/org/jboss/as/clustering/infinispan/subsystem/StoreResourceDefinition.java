/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.commons.configuration.Combine;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.PropertiesAttributeDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.wildfly.clustering.server.util.MapEntry;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Base class for store resources which require common store attributes only.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public abstract class StoreResourceDefinition<C extends StoreConfiguration, B extends StoreConfigurationBuilder<C, B>> extends ComponentResourceDefinition implements ResourceModelResolver<Map.Entry<Map.Entry<Supplier<B>, Consumer<B>>, Stream<Consumer<RequirementServiceBuilder<?>>>>> {

    protected static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    protected static PathElement pathElement(String value) {
        return PathElement.pathElement("store", value);
    }

    static final BinaryServiceDescriptor<PersistenceConfiguration> SERVICE_DESCRIPTOR = serviceDescriptor(WILDCARD_PATH, PersistenceConfiguration.class);
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        MAX_BATCH_SIZE("max-batch-size", ModelType.INT, new ModelNode(100)),
        PASSIVATION("passivation", ModelType.BOOLEAN, ModelNode.FALSE),
        PRELOAD("preload", ModelType.BOOLEAN, ModelNode.FALSE),
        PURGE("purge", ModelType.BOOLEAN, ModelNode.FALSE),
        SHARED("shared", ModelType.BOOLEAN, ModelNode.FALSE),
        SEGMENTED("segmented", ModelType.BOOLEAN, ModelNode.TRUE),
        PROPERTIES("properties"),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        Attribute(String name) {
            this.definition = new PropertiesAttributeDefinition.Builder(name)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute {
        FETCH_STATE("fetch-state", ModelType.BOOLEAN, ModelNode.TRUE, InfinispanSubsystemModel.VERSION_16_0_0),
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, ModelNode defaultValue, InfinispanSubsystemModel deprecation) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setDeprecated(deprecation.getVersion())
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static final Set<PathElement> REQUIRED_SINGLETON_CHILDREN = Set.of(StoreWriteThroughResourceDefinition.PATH);

    private final UnaryOperator<ResourceDescriptor> configurator;
    private final Class<B> builderClass;

    protected StoreResourceDefinition(PathElement path, ResourceDescriptionResolver resolver, UnaryOperator<ResourceDescriptor> configurator, Class<B> builderClass) {
        super(path, resolver);
        this.configurator = configurator;
        this.builderClass = builderClass;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addAttributes(Attribute.class)
                .addAttributes(DeprecatedAttribute.class)
                .addCapabilities(List.of(CAPABILITY))
                .addRequiredSingletonChildren(REQUIRED_SINGLETON_CHILDREN)
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        new StoreWriteBehindResourceDefinition().register(registration);
        new StoreWriteThroughResourceDefinition().register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        boolean passivation = Attribute.PASSIVATION.resolveModelAttribute(context, model).asBoolean();
        boolean preload = Attribute.PRELOAD.resolveModelAttribute(context, model).asBoolean();
        boolean purge = Attribute.PURGE.resolveModelAttribute(context, model).asBoolean();
        boolean segmented = Attribute.SEGMENTED.resolveModelAttribute(context, model).asBoolean();
        boolean shared = Attribute.SHARED.resolveModelAttribute(context, model).asBoolean();
        int maxBatchSize = Attribute.MAX_BATCH_SIZE.resolveModelAttribute(context, model).asInt();
        Properties properties = new Properties();
        for (Property property : Attribute.PROPERTIES.resolveModelAttribute(context, model).asPropertyListOrEmpty()) {
            properties.setProperty(property.getName(), property.getValue().asString());
        }

        Map.Entry<Map.Entry<Supplier<B>, Consumer<B>>, Stream<Consumer<RequirementServiceBuilder<?>>>> entry = this.resolve(context, model);
        Supplier<B> builderFactory = entry.getKey().getKey();
        Consumer<B> configurator = entry.getKey().getValue();
        Stream<Consumer<RequirementServiceBuilder<?>>> dependencies = entry.getValue();
        Supplier<PersistenceConfiguration> configurationFactory = new Supplier<>() {
            @Override
            public PersistenceConfiguration get() {
                B builder = builderFactory.get()
                        .maxBatchSize(maxBatchSize)
                        .preload(preload)
                        .purgeOnStartup(purge)
                        .segmented(segmented)
                        .shared(shared)
                        .withProperties(properties);
                configurator.accept(builder);
                return builder.persistence().passivation(passivation).create();
            }
        };
        return CapabilityServiceInstaller.builder(CAPABILITY, configurationFactory)
                .requires(dependencies.collect(Collectors.toList()))
                .build();
    }

    @Override
    public Map.Entry<Map.Entry<Supplier<B>, Consumer<B>>, Stream<Consumer<RequirementServiceBuilder<?>>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        Supplier<B> builderFactory = () -> new ConfigurationBuilder().persistence().addStore(this.builderClass);

        PathAddress address = context.getCurrentAddress();
        PathAddress cacheAddress = address.getParent();
        String containerName = cacheAddress.getParent().getLastElement().getValue();
        String cacheName = cacheAddress.getLastElement().getValue();

        ServiceDependency<AsyncStoreConfiguration> async = ServiceDependency.on(StoreWriteResourceDefinition.SERVICE_DESCRIPTOR, containerName, cacheName);
        Consumer<B> configurator = new Consumer<>() {
            @Override
            public void accept(B builder) {
                builder.async().read(async.get(), Combine.DEFAULT);
            }
        };
        return MapEntry.of(MapEntry.of(builderFactory, configurator), Stream.of(async));
    }
}
