/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
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
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/expiration=EXPIRATION
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ExpirationResourceDefinition extends ComponentResourceDefinition {

    static final PathElement PATH = pathElement("expiration");

    static final BinaryServiceDescriptor<ExpirationConfiguration> SERVICE_DESCRIPTOR = serviceDescriptor(PATH, ExpirationConfiguration.class);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        INTERVAL("interval", new ModelNode(TimeUnit.MINUTES.toMillis(1))),
        LIFESPAN("lifespan", null),
        MAX_IDLE("max-idle", null),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, ModelType.LONG)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    ExpirationResourceDefinition() {
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
        long interval = Attribute.INTERVAL.resolveModelAttribute(context, model).asLong();
        long lifespan = Attribute.LIFESPAN.resolveModelAttribute(context, model).asLong(-1L);
        long maxIdle = Attribute.MAX_IDLE.resolveModelAttribute(context, model).asLong(-1L);

        Supplier<ExpirationConfiguration> configurationFactory = new Supplier<>() {
            @Override
            public ExpirationConfiguration get() {
                return new ConfigurationBuilder().expiration()
                        .lifespan(lifespan, TimeUnit.MILLISECONDS)
                        .maxIdle(maxIdle, TimeUnit.MILLISECONDS)
                        .reaperEnabled(interval > 0)
                        .wakeUpInterval(interval, TimeUnit.MILLISECONDS)
                        .create();
            }
        };
        return CapabilityServiceInstaller.builder(CAPABILITY, configurationFactory).build();
    }
}
