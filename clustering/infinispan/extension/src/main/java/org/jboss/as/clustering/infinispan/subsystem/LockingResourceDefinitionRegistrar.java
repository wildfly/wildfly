/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.IsolationLevel;
import org.infinispan.configuration.cache.LockingConfiguration;
import org.infinispan.configuration.cache.LockingConfigurationBuilder;
import org.jboss.as.clustering.controller.DurationAttributeDefinition;
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
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for the locking component of a cache configuration.
 * @author Paul Ferraro
 */
public class LockingResourceDefinitionRegistrar extends ConfigurationResourceDefinitionRegistrar<LockingConfiguration, LockingConfigurationBuilder> {

    static final BinaryServiceDescriptor<LockingConfiguration> SERVICE_DESCRIPTOR = BinaryServiceDescriptorFactory.createServiceDescriptor(ComponentResourceRegistration.LOCKING, LockingConfiguration.class);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).setAllowMultipleRegistrations(true).build();

    static final EnumAttributeDefinition<IsolationLevel> ISOLATION = new EnumAttributeDefinition.Builder<>("isolation", IsolationLevel.READ_COMMITTED).build();
    static final DurationAttributeDefinition ACQUIRE_TIMEOUT = new DurationAttributeDefinition.Builder("acquire-timeout", ChronoUnit.MILLIS).setDefaultValue(Duration.ofSeconds(15)).build();

    enum Attribute implements AttributeDefinitionProvider, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        CONCURRENCY("concurrency-level", ModelType.INT, new ModelNode(1000)) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDeprecated(InfinispanSubsystemModel.VERSION_17_0_0.getVersion());
            }
        },
        STRIPING("striping", ModelType.BOOLEAN, ModelNode.FALSE),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type))
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder;
        }
    }

    LockingResourceDefinitionRegistrar() {
        super(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return ComponentResourceRegistration.LOCKING;
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
                .addAttributes(List.of(ISOLATION, ACQUIRE_TIMEOUT))
                .provideAttributes(EnumSet.allOf(Attribute.class))
                ;
    }

    @Override
    public ServiceDependency<LockingConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        Duration timeout = ACQUIRE_TIMEOUT.resolve(context, model);
        int concurrency = Attribute.CONCURRENCY.resolveModelAttribute(context, model).asInt();
        IsolationLevel isolation = ISOLATION.resolve(context, model);
        boolean striping = Attribute.STRIPING.resolveModelAttribute(context, model).asBoolean();
        return ServiceDependency.from(new Supplier<>() {
            @Override
            public LockingConfigurationBuilder get() {
                return new ConfigurationBuilder().locking()
                        .lockAcquisitionTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                        .concurrencyLevel(concurrency)
                        .isolationLevel(isolation)
                        .useLockStriping(striping)
                        ;
            }
        });
    }
}
