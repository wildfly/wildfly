/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;
import java.util.function.Supplier;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.MemoryConfiguration;
import org.infinispan.configuration.cache.MemoryConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.eviction.EvictionStrategy;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.EnumAttributeDefinition;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for the memory component of a cache.
 * @author Paul Ferraro
 */
public class MemoryResourceDefinitionRegistrar extends ConfigurationResourceDefinitionRegistrar<MemoryConfiguration, MemoryConfigurationBuilder> {
    static final BinaryServiceDescriptor<MemoryConfiguration> SERVICE_DESCRIPTOR = BinaryServiceDescriptorFactory.createServiceDescriptor(List.of(MemoryResourceRegistration.WILDCARD), MemoryConfiguration.class);
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).setAllowMultipleRegistrations(true).build();

    static final AttributeDefinition SIZE = new SimpleAttributeDefinitionBuilder("size", ModelType.LONG)
            .setAllowExpression(true)
            .setRequired(false)
            .setValidator(LongRangeValidator.POSITIVE)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();
    static final EnumAttributeDefinition<MemorySizeUnit> SIZE_UNIT = EnumAttributeDefinition.nameBuilder("size-unit", MemorySizeUnit.class)
            .setDefaultValue(MemorySizeUnit.ENTRIES)
            .build();

    interface Configurator extends ConfigurationResourceDefinitionRegistrar.Configurator<MemoryConfiguration> {
        StorageType getStorageType();

        default EnumAttributeDefinition<MemorySizeUnit> getSizeUnitAttribute() {
            return SIZE_UNIT;
        }

        @Override
        default RuntimeCapability<Void> getCapability() {
            return CAPABILITY;
        }
    }

    private final StorageType storageType;
    private final EnumAttributeDefinition<MemorySizeUnit> sizeUnitAttribute;

    MemoryResourceDefinitionRegistrar(Configurator configurator) {
        super(configurator);
        this.storageType = configurator.getStorageType();
        this.sizeUnitAttribute = configurator.getSizeUnitAttribute();
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .addAttributes(List.of(SIZE, this.sizeUnitAttribute));
    }

    @Override
    public ServiceDependency<MemoryConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        StorageType storageType = this.storageType;
        long size = SIZE.resolveModelAttribute(context, model).asLong(-1L);
        MemorySizeUnit unit = this.sizeUnitAttribute.resolve(context, model);
        EvictionStrategy strategy = (size > 0) ? EvictionStrategy.REMOVE : EvictionStrategy.MANUAL;
        return ServiceDependency.from(new Supplier<>() {
            @Override
            public MemoryConfigurationBuilder get() {
                MemoryConfigurationBuilder builder = new ConfigurationBuilder().memory()
                        .storage(storageType)
                        .whenFull(strategy)
                        ;
                if (strategy.isEnabled()) {
                    if (unit == MemorySizeUnit.ENTRIES) {
                        builder.maxCount(size);
                    } else {
                        builder.maxSize(Long.toString(unit.applyAsLong(size)));
                    }
                }
                return builder;
            }
        });
    }
}
