/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PartitionHandlingConfiguration;
import org.infinispan.configuration.cache.PartitionHandlingConfigurationBuilder;
import org.infinispan.conflict.MergePolicy;
import org.infinispan.partitionhandling.PartitionHandling;
import org.jboss.as.clustering.controller.EnumAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.AttributeTranslation;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Resource definition of the partition handling component of a cache.
 * @author Paul Ferraro
 */
public enum PartitionHandlingResourceDescription implements CacheComponentResourceDescription<PartitionHandlingConfiguration, PartitionHandlingConfigurationBuilder> {
    INSTANCE;

    private final PathElement path = ComponentResourceDescription.pathElement("partition-handling");
    private final BinaryServiceDescriptor<PartitionHandlingConfiguration> descriptor = CacheComponentResourceDescription.createServiceDescriptor(this.path, PartitionHandlingConfiguration.class);
    private final RuntimeCapability<Void> capability = RuntimeCapability.Builder.of(this.descriptor).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).setAllowMultipleRegistrations(true).build();

    static final EnumAttributeDefinition<PartitionHandling> WHEN_SPLIT = new EnumAttributeDefinition.Builder<>("when-split", PartitionHandling.ALLOW_READ_WRITES).build();
    static final EnumAttributeDefinition<MergePolicy> MERGE_POLICY = new EnumAttributeDefinition.Builder<>("merge-policy", MergePolicy.NONE).setAllowedValues(EnumSet.complementOf(EnumSet.of(MergePolicy.CUSTOM))).build();

    enum DeprecatedAttribute implements AttributeDefinitionProvider {
        ENABLED("enabled", ModelType.BOOLEAN, ModelNode.FALSE, InfinispanSubsystemModel.VERSION_16_0_0),
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, ModelNode defaultValue, InfinispanSubsystemModel deprecation) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setDeprecated(deprecation.getVersion())
                    .setFlags(AttributeAccess.Flag.ALIAS)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public BinaryServiceDescriptor<PartitionHandlingConfiguration> getServiceDescriptor() {
        return this.descriptor;
    }

    @Override
    public RuntimeCapability<Void> getCapability() {
        return this.capability;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.of(WHEN_SPLIT, MERGE_POLICY);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return CacheComponentResourceDescription.super.apply(builder)
                .translateAttribute(DeprecatedAttribute.ENABLED.get(), new AttributeTranslation() {
                    @Override
                    public AttributeDefinition getTargetAttribute() {
                        return WHEN_SPLIT;
                    }

                    @Override
                    public AttributeValueTranslator getReadAttributeOperationTranslator() {
                        return (context, value) -> value.isDefined() ? (value.equals(WHEN_SPLIT.getDefaultValue()) ? ModelNode.FALSE : ModelNode.TRUE) : value;
                    }

                    @Override
                    public AttributeValueTranslator getWriteAttributeOperationTranslator() {
                        return (context, value) -> value.isDefined() ? new ModelNode((value.asBoolean() ? PartitionHandling.DENY_READ_WRITES : PartitionHandling.ALLOW_READ_WRITES).name()) : value;
                    }
                });
    }

    @Override
    public ServiceDependency<PartitionHandlingConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        PartitionHandling whenSplit = WHEN_SPLIT.resolve(context, model);
        MergePolicy mergePolicy = MERGE_POLICY.resolve(context, model);
        return ServiceDependency.from(new Supplier<>() {
            @Override
            public PartitionHandlingConfigurationBuilder get() {
                return new ConfigurationBuilder().clustering().partitionHandling()
                        .whenSplit(whenSplit)
                        .mergePolicy(mergePolicy);
            }
        });
    }
}
