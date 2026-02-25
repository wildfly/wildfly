/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StateTransferConfiguration;
import org.infinispan.configuration.cache.StateTransferConfigurationBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.DurationAttributeDefinition;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for the state transfer component of a cache configuration.
 * @author Paul Ferraro
 */
public class StateTransferResourceDefinitionRegistrar extends ConfigurationResourceDefinitionRegistrar<StateTransferConfiguration, StateTransferConfigurationBuilder> {
    static final BinaryServiceDescriptor<StateTransferConfiguration> SERVICE_DESCRIPTOR = BinaryServiceDescriptorFactory.createServiceDescriptor(ComponentResourceRegistration.STATE_TRANSFER, StateTransferConfiguration.class);
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).setAllowMultipleRegistrations(true).build();

    static final DurationAttributeDefinition TIMEOUT = DurationAttributeDefinition.builder("timeout", ChronoUnit.MILLIS).setDefaultValue(Duration.ofMinutes(4)).build();
    static final AttributeDefinition CHUNK_SIZE = new SimpleAttributeDefinitionBuilder("chunk-size", ModelType.INT)
            .setAllowExpression(true)
            .setRequired(false)
            .setDefaultValue(new ModelNode(512))
            .setValidator(IntRangeValidator.POSITIVE)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    StateTransferResourceDefinitionRegistrar() {
        super(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return ComponentResourceRegistration.STATE_TRANSFER;
            }

            @Override
            public RuntimeCapability<Void> getCapability() {
                return CAPABILITY;
            }
        });
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).addAttributes(List.of(TIMEOUT, CHUNK_SIZE));
    }

    @Override
    public ServiceDependency<StateTransferConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        int chunkSize = CHUNK_SIZE.resolveModelAttribute(context, model).asInt();
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
