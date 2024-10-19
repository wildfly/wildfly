/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PartitionHandlingConfiguration;
import org.infinispan.configuration.cache.PartitionHandlingConfigurationBuilder;
import org.infinispan.conflict.MergePolicy;
import org.infinispan.partitionhandling.PartitionHandling;
import org.jboss.as.clustering.controller.EnumAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.AttributeTranslation;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for the partition handling component of a cache configuration.
 * @author Paul Ferraro
 */
public class PartitionHandlingResourceDefinitionRegistrar extends ConfigurationResourceDefinitionRegistrar<PartitionHandlingConfiguration, PartitionHandlingConfigurationBuilder> {

    static final BinaryServiceDescriptor<PartitionHandlingConfiguration> SERVICE_DESCRIPTOR = BinaryServiceDescriptorFactory.createServiceDescriptor(ComponentResourceRegistration.PARTITION_HANDLING, PartitionHandlingConfiguration.class);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).setAllowMultipleRegistrations(true).build();

    static final EnumAttributeDefinition<PartitionHandling> WHEN_SPLIT = new EnumAttributeDefinition.Builder<>("when-split", PartitionHandling.ALLOW_READ_WRITES).build();
    static final EnumAttributeDefinition<MergePolicy> MERGE_POLICY = new EnumAttributeDefinition.Builder<>("merge-policy", MergePolicy.NONE).setAllowedValues(EnumSet.complementOf(EnumSet.of(MergePolicy.CUSTOM))).build();

    static final AttributeDefinition ENABLED = new SimpleAttributeDefinitionBuilder("enabled", ModelType.BOOLEAN)
            .setAllowExpression(true)
            .setRequired(false)
            .setDefaultValue(ModelNode.FALSE)
            .setDeprecated(InfinispanSubsystemModel.VERSION_16_0_0.getVersion())
            .setFlags(AttributeAccess.Flag.ALIAS)
            .build();

    PartitionHandlingResourceDefinitionRegistrar() {
        super(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return ComponentResourceRegistration.PARTITION_HANDLING;
            }

            @Override
            public RuntimeCapability<Void> getCapability() {
                return CAPABILITY;
            }
        });
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .addAttributes(List.of(WHEN_SPLIT, MERGE_POLICY))
                .translateAttribute(ENABLED, new AttributeTranslation() {
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
