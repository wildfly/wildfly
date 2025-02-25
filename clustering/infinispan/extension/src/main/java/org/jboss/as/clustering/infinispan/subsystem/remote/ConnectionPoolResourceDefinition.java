/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ConnectionPoolConfiguration;
import org.infinispan.client.hotrod.configuration.ExhaustedAction;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanSubsystemModel;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * /subsystem=infinispan/remote-cache-container=X/component=connection-pool
 *
 * @author Radoslav Husar
 */
@Deprecated(forRemoval = true)
public class ConnectionPoolResourceDefinition extends ComponentResourceDefinition {

    public static final PathElement PATH = pathElement("connection-pool");

    static final UnaryServiceDescriptor<ConnectionPoolConfiguration> SERVICE_DESCRIPTOR = serviceDescriptor(PATH, ConnectionPoolConfiguration.class);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        EXHAUSTED_ACTION("exhausted-action", ModelType.STRING, new ModelNode(ExhaustedAction.WAIT.name())) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(EnumValidator.create(ExhaustedAction.class));
            }
        },
        MAX_ACTIVE("max-active", ModelType.INT, null),
        MAX_WAIT("max-wait", ModelType.LONG, null),
        MIN_EVICTABLE_IDLE_TIME("min-evictable-idle-time", ModelType.LONG, new ModelNode(TimeUnit.MINUTES.toMillis(30))),
        MIN_IDLE("min-idle", ModelType.INT, new ModelNode(1)),
        ;

        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setMeasurementUnit((type == ModelType.LONG) ? MeasurementUnit.MILLISECONDS : null)
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

    ConnectionPoolResourceDefinition() {
        super(PATH);
        // No longer used as of Infinispan 15.1
        this.setDeprecated(InfinispanSubsystemModel.VERSION_20_0_0.getVersion());
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(List.of(CAPABILITY))
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        ExhaustedAction exhaustedAction = ExhaustedAction.valueOf(ConnectionPoolResourceDefinition.Attribute.EXHAUSTED_ACTION.resolveModelAttribute(context, model).asString());
        int maxActive = ConnectionPoolResourceDefinition.Attribute.MAX_ACTIVE.resolveModelAttribute(context, model).asInt(-1);
        long maxWait = ConnectionPoolResourceDefinition.Attribute.MAX_WAIT.resolveModelAttribute(context, model).asLong(-1L);
        long minEvictableIdleTime = ConnectionPoolResourceDefinition.Attribute.MIN_EVICTABLE_IDLE_TIME.resolveModelAttribute(context, model).asLong();
        int minIdle = ConnectionPoolResourceDefinition.Attribute.MIN_IDLE.resolveModelAttribute(context, model).asInt();

        Supplier<ConnectionPoolConfiguration> configurationFactory = new Supplier<>() {
            @Override
            public ConnectionPoolConfiguration get() {
                return new ConfigurationBuilder().connectionPool()
                        .exhaustedAction(exhaustedAction)
                        .maxActive(maxActive)
                        .maxWait(maxWait)
                        .minEvictableIdleTime(minEvictableIdleTime)
                        .minIdle(minIdle)
                        .create();
            }
        };
        return CapabilityServiceInstaller.builder(CAPABILITY, configurationFactory).build();
    }
}
