/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PartitionHandlingConfiguration;
import org.infinispan.conflict.MergePolicy;
import org.infinispan.partitionhandling.PartitionHandling;
import org.jboss.as.clustering.controller.AttributeTranslation;
import org.jboss.as.clustering.controller.AttributeValueTranslator;
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
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Resource definition of the partition handling component of a cache.
 * @author Paul Ferraro
 */
public class PartitionHandlingResourceDefinition extends ComponentResourceDefinition {

    static final PathElement PATH = pathElement("partition-handling");

    static final BinaryServiceDescriptor<PartitionHandlingConfiguration> SERVICE_DESCRIPTOR = serviceDescriptor(PATH, PartitionHandlingConfiguration.class);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        WHEN_SPLIT("when-split", PartitionHandling.ALLOW_READ_WRITES, EnumValidator.create(PartitionHandling.class)),
        MERGE_POLICY("merge-policy", MergePolicy.NONE, EnumValidator.create(MergePolicy.class, EnumSet.complementOf(EnumSet.of(MergePolicy.CUSTOM)))),
        ;
        private final AttributeDefinition definition;

        <E extends Enum<E>> Attribute(String name, E defaultValue, EnumValidator<E> validator) {
            this(name, ModelType.STRING, new ModelNode(defaultValue.name()), builder -> builder.setValidator(validator));
        }

        Attribute(String name, ModelType type, ModelNode defaultValue, UnaryOperator<SimpleAttributeDefinitionBuilder> configurator) {
            this.definition = configurator.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setDefaultValue(defaultValue)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build()
                    ;
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute {
        ENABLED("enabled", ModelType.BOOLEAN, ModelNode.FALSE, InfinispanSubsystemModel.VERSION_16_0_0),
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

    PartitionHandlingResourceDefinition() {
        super(PATH);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addAttributeTranslation(DeprecatedAttribute.ENABLED, new AttributeTranslation() {
                    @Override
                    public org.jboss.as.clustering.controller.Attribute getTargetAttribute() {
                        return Attribute.WHEN_SPLIT;
                    }

                    @Override
                    public AttributeValueTranslator getReadTranslator() {
                        return (context, value) -> value.isDefined() ? (value.equals(Attribute.WHEN_SPLIT.getDefinition().getDefaultValue()) ? ModelNode.FALSE : ModelNode.TRUE) : value;
                    }

                    @Override
                    public AttributeValueTranslator getWriteTranslator() {
                        return (context, value) -> value.isDefined() ? new ModelNode((value.asBoolean() ? PartitionHandling.DENY_READ_WRITES : PartitionHandling.ALLOW_READ_WRITES).name()) : value;
                    }
                })
                .addCapabilities(List.of(CAPABILITY))
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        PartitionHandling whenSplit = PartitionHandling.valueOf(Attribute.WHEN_SPLIT.resolveModelAttribute(context, model).asString());
        MergePolicy mergePolicy = MergePolicy.valueOf(Attribute.MERGE_POLICY.resolveModelAttribute(context, model).asString());

        Supplier<PartitionHandlingConfiguration> configurationFactory = new Supplier<>() {
            @Override
            public PartitionHandlingConfiguration get() {
                return new ConfigurationBuilder().clustering().partitionHandling()
                        .whenSplit(whenSplit)
                        .mergePolicy(mergePolicy)
                        .create();
            }
        };
        return CapabilityServiceInstaller.builder(CAPABILITY, configurationFactory).build();
    }
}
