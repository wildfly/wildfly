/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.MemoryConfiguration;
import org.infinispan.configuration.cache.MemoryConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.eviction.EvictionStrategy;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class MemoryResourceDefinition extends ComponentResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String value) {
        return PathElement.pathElement("memory", value);
    }

    static final BinaryServiceDescriptor<MemoryConfiguration> SERVICE_DESCRIPTOR = serviceDescriptor(WILDCARD_PATH, MemoryConfiguration.class);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).setAllowMultipleRegistrations(true).build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        SIZE("size", ModelType.LONG, null) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(LongRangeValidator.POSITIVE);
            }
        },
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

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder;
        }
    }

    enum SharedAttribute implements org.jboss.as.clustering.controller.Attribute {
        SIZE_UNIT("size-unit", ModelType.STRING, new ModelNode(MemorySizeUnit.ENTRIES.name())),
        ;
        private final AttributeDefinition definition;

        SharedAttribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    private final StorageType type;
    private final AttributeDefinition sizeUnitAttribute;
    private final UnaryOperator<ResourceDescriptor> configurator;

    MemoryResourceDefinition(StorageType type, PathElement path, UnaryOperator<ResourceDescriptor> configurator, AttributeDefinition sizeUnitAttribute) {
        super(path, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(path, WILDCARD_PATH));
        this.type = type;
        this.configurator = configurator;
        this.sizeUnitAttribute = sizeUnitAttribute;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addAttributes(Attribute.class)
                .addCapabilities(List.of(CAPABILITY))
                ;

        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        long size = MemoryResourceDefinition.Attribute.SIZE.resolveModelAttribute(context, model).asLong(-1L);
        MemorySizeUnit unit = MemorySizeUnit.valueOf(this.sizeUnitAttribute.resolveModelAttribute(context, model).asString());
        StorageType type = this.type;
        Supplier<MemoryConfiguration> configurationFactory = new Supplier<>() {
            @Override
            public MemoryConfiguration get() {
                EvictionStrategy strategy = size > 0 ? EvictionStrategy.REMOVE : EvictionStrategy.MANUAL;
                MemoryConfigurationBuilder builder = new ConfigurationBuilder().memory()
                        .storage(type)
                        .whenFull(strategy)
                        ;
                if (strategy.isEnabled()) {
                    if (unit == MemorySizeUnit.ENTRIES) {
                        builder.maxCount(size);
                    } else {
                        builder.maxSize(Long.toString(unit.applyAsLong(size)));
                    }
                }
                return builder.create();
            }
        };
        return CapabilityServiceInstaller.builder(CAPABILITY, configurationFactory).build();
    }
}
