/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.MemoryConfiguration;
import org.infinispan.configuration.cache.MemoryConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.eviction.EvictionStrategy;
import org.jboss.as.clustering.controller.EnumAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * @author Paul Ferraro
 *
 */
public interface MemoryResourceDescription extends CacheComponentResourceDescription<MemoryConfiguration, MemoryConfigurationBuilder> {

    static PathElement pathElement(String value) {
        return PathElement.pathElement("memory", value);
    }

    BinaryServiceDescriptor<MemoryConfiguration> SERVICE_DESCRIPTOR = CacheComponentResourceDescription.createServiceDescriptor(pathElement(PathElement.WILDCARD_VALUE), MemoryConfiguration.class);
    RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).setAllowMultipleRegistrations(true).build();

    EnumAttributeDefinition<MemorySizeUnit> SIZE_UNIT = new EnumAttributeDefinition.Builder<>("size-unit", MemorySizeUnit.ENTRIES).build();

    enum Attribute implements AttributeDefinitionProvider {
        SIZE("size", ModelType.LONG, LongRangeValidator.POSITIVE),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ParameterValidator validator) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setValidator(validator)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    @Override
    default PathElement getPathKey() {
        return pathElement(PathElement.WILDCARD_VALUE);
    }

    StorageType getStorageType();

    default EnumAttributeDefinition<MemorySizeUnit> getSizeUnitAttribute() {
        return SIZE_UNIT;
    }

    @Override
    default BinaryServiceDescriptor<MemoryConfiguration> getServiceDescriptor() {
        return SERVICE_DESCRIPTOR;
    }

    @Override
    default RuntimeCapability<Void> getCapability() {
        return CAPABILITY;
    }

    @Override
    default Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(ResourceDescriptor.stream(EnumSet.allOf(Attribute.class)), Stream.of(MemoryResourceDescription.this.getSizeUnitAttribute()));
    }

    @Override
    default ServiceDependency<MemoryConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        StorageType storageType = this.getStorageType();
        long size = Attribute.SIZE.resolveModelAttribute(context, model).asLong(-1L);
        MemorySizeUnit unit = this.getSizeUnitAttribute().resolve(context, model);
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
