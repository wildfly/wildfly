/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.ejb;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ISOStandardDurationAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.ejb.bean.BeanDeploymentMarshallingContext;
import org.wildfly.clustering.ejb.bean.BeanManagementConfiguration;
import org.wildfly.clustering.ejb.bean.BeanManagementProvider;
import org.wildfly.clustering.ejb.cache.bean.BeanMarshallerFactory;
import org.wildfly.clustering.ejb.remote.ClientMappingsRegistryProvider;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;

/**
 * Registers a resource definition for a bean management provider.
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public abstract class BeanManagementResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator, ResourceModelResolver<BeanManagementConfiguration>, UnaryOperator<ResourceDescriptor.Builder> {

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(BeanManagementProvider.SERVICE_DESCRIPTOR)
            .addRequirements(ClientMappingsRegistryProvider.SERVICE_DESCRIPTOR.getName())
            .setAllowMultipleRegistrations(true)
            .build();

    static final AttributeDefinition MAX_ACTIVE_BEANS = new SimpleAttributeDefinitionBuilder("max-active-beans", ModelType.INT)
            .setAllowExpression(true)
            .setRequired(false)
            .setFlags(Flag.RESTART_RESOURCE_SERVICES)
            .setValidator(new IntRangeValidator(1))
            .build();

    static final ISOStandardDurationAttributeDefinition IDLE_THRESHOLD = new ISOStandardDurationAttributeDefinition.Builder("idle-threshold")
            .setRequired(false)
            .setStability(Stability.COMMUNITY)
            .build();

    private final ResourceRegistration registration;

    BeanManagementResourceDefinitionRegistrar(ResourceRegistration registration) {
        this.registration = registration;
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return builder.addCapability(CAPABILITY)
                .addAttributes(List.of(MAX_ACTIVE_BEANS, IDLE_THRESHOLD))
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                ;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = DistributableEjbSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(this.registration.getPathElement(), PathElement.pathElement("bean-management"));
        ResourceDescriptor descriptor = this.apply(ResourceDescriptor.builder(resolver)).build();
        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(this.registration, resolver).build());
        ManagementResourceRegistrar.of(descriptor).register(registration);
        return registration;
    }

    @Override
    public BeanManagementConfiguration resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        OptionalInt maxActiveBeans = Optional.ofNullable(MAX_ACTIVE_BEANS.resolveModelAttribute(context, model).asIntOrNull()).map(OptionalInt::of).orElse(OptionalInt.empty());
        Optional<Duration> idleThreshold = Optional.ofNullable(IDLE_THRESHOLD.resolve(context, model));
        return new BeanManagementConfiguration() {
            @Override
            public OptionalInt getMaxSize() {
                return maxActiveBeans;
            }

            @Override
            public Optional<Duration> getIdleTimeout() {
                return idleThreshold;
            }

            @Override
            public Function<BeanDeploymentMarshallingContext, ByteBufferMarshaller> getMarshallerFactory() {
                // Currently hard-coded to use JBoss Marshalling
                return BeanMarshallerFactory.JBOSS;
            }
        };
    }
}