/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.LockingConfiguration;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/locking=LOCKING
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class LockingResourceDefinition extends ComponentResourceDefinition {

    static final PathElement PATH = pathElement("locking");

    static final BinaryServiceDescriptor<LockingConfiguration> SERVICE_DESCRIPTOR = serviceDescriptor(PATH, LockingConfiguration.class);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        ACQUIRE_TIMEOUT("acquire-timeout", ModelType.LONG, new ModelNode(TimeUnit.SECONDS.toMillis(15))) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setMeasurementUnit(MeasurementUnit.MILLISECONDS);
            }
        },
        CONCURRENCY("concurrency-level", ModelType.INT, new ModelNode(1000)) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDeprecated(InfinispanSubsystemModel.VERSION_17_0_0.getVersion());
            }
        },
        ISOLATION("isolation", ModelType.STRING, new ModelNode(IsolationLevel.READ_COMMITTED.name())) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(EnumValidator.create(IsolationLevel.class));
            }
        },
        STRIPING("striping", ModelType.BOOLEAN, ModelNode.FALSE),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
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

    LockingResourceDefinition() {
        super(PATH);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver()).addAttributes(Attribute.class);
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        long timeout = Attribute.ACQUIRE_TIMEOUT.resolveModelAttribute(context, model).asLong();
        int concurrency = Attribute.CONCURRENCY.resolveModelAttribute(context, model).asInt();
        IsolationLevel isolation = IsolationLevel.valueOf(Attribute.ISOLATION.resolveModelAttribute(context, model).asString());
        boolean striping = Attribute.STRIPING.resolveModelAttribute(context, model).asBoolean();

        Supplier<LockingConfiguration> configurationFactory = new Supplier<>() {
            @Override
            public LockingConfiguration get() {
                return new ConfigurationBuilder().locking()
                        .lockAcquisitionTimeout(timeout)
                        .concurrencyLevel(concurrency)
                        .isolationLevel(isolation)
                        .useLockStriping(striping)
                        .create();
            }
        };
        return CapabilityServiceInstaller.builder(CAPABILITY, configurationFactory).build();
    }
}
