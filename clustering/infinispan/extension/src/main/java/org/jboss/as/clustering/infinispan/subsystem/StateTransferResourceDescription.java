/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StateTransferConfiguration;
import org.infinispan.configuration.cache.StateTransferConfigurationBuilder;
import org.jboss.as.clustering.controller.DurationAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Describes a state transfer cache component resource.
 * @author Paul Ferraro
 */
public enum StateTransferResourceDescription implements CacheComponentResourceDescription<StateTransferConfiguration, StateTransferConfigurationBuilder> {
    INSTANCE;

    private final PathElement path = ComponentResourceDescription.pathElement("state-transfer");
    private final BinaryServiceDescriptor<StateTransferConfiguration> descriptor = CacheComponentResourceDescription.createServiceDescriptor(this.path, StateTransferConfiguration.class);
    private final RuntimeCapability<Void> capability = RuntimeCapability.Builder.of(this.descriptor).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).setAllowMultipleRegistrations(true).build();

    static final DurationAttributeDefinition TIMEOUT = new DurationAttributeDefinition.Builder("timeout", ChronoUnit.MILLIS).setDefaultValue(Duration.ofMinutes(4)).build();

    enum Attribute implements AttributeDefinitionProvider {
        CHUNK_SIZE("chunk-size", ModelType.INT, new ModelNode(512), IntRangeValidator.POSITIVE),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue, ParameterValidator validator) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
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
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public BinaryServiceDescriptor<StateTransferConfiguration> getServiceDescriptor() {
        return this.descriptor;
    }

    @Override
    public RuntimeCapability<Void> getCapability() {
        return this.capability;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(Stream.of(TIMEOUT), ResourceDescriptor.stream(EnumSet.allOf(Attribute.class)));
    }

    @Override
    public ServiceDependency<StateTransferConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        int chunkSize = Attribute.CHUNK_SIZE.resolveModelAttribute(context, model).asInt();
        Duration timeout = TIMEOUT.resolve(context, model);
        return ServiceDependency.from(new Supplier<>() {
            @Override
            public StateTransferConfigurationBuilder get() {
                boolean timeoutEnabled = !timeout.isZero();
                return new ConfigurationBuilder().clustering().stateTransfer()
                        .chunkSize(chunkSize)
                        .fetchInMemoryState(true)
                        .awaitInitialTransfer(timeoutEnabled)
                        .timeout(timeoutEnabled ? timeout.toSeconds() : Long.MAX_VALUE, TimeUnit.SECONDS)
                        ;
            }
        });
    }
}
