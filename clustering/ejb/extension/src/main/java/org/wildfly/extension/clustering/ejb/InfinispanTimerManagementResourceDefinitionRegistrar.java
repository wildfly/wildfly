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

import org.jboss.as.clustering.controller.EnumAttributeDefinition;
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
import org.jboss.modules.Module;
import org.wildfly.clustering.ejb.infinispan.timer.InfinispanTimerManagementProvider;
import org.wildfly.clustering.ejb.timer.TimerManagementConfiguration;
import org.wildfly.clustering.ejb.timer.TimerManagementProvider;
import org.wildfly.clustering.infinispan.service.InfinispanCacheConfigurationAttributeGroup;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.server.service.CacheConfigurationAttributeGroup;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Registers a resource definition for an embedded Infinispan timer management provider.
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public class InfinispanTimerManagementResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator {

    static final ResourceRegistration REGISTRATION = ResourceRegistration.of(PathElement.pathElement("infinispan-timer-management"));

    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(TimerManagementProvider.SERVICE_DESCRIPTOR).build();

    static final CacheConfigurationAttributeGroup CACHE_ATTRIBUTE_GROUP = new InfinispanCacheConfigurationAttributeGroup(CAPABILITY);
    static final EnumAttributeDefinition<TimerContextMarshallerFactory> MARSHALLER = new EnumAttributeDefinition.Builder<>("marshaller", TimerContextMarshallerFactory.JBOSS).build();
    static final AttributeDefinition MAX_ACTIVE_TIMERS = new SimpleAttributeDefinitionBuilder("max-active-timers", ModelType.INT)
            .setAllowExpression(true)
            .setRequired(false)
            .setFlags(Flag.RESTART_RESOURCE_SERVICES)
            .setValidator(new IntRangeValidator(1))
            .build();

    static final ISOStandardDurationAttributeDefinition IDLE_THRESHOLD = new ISOStandardDurationAttributeDefinition.Builder("idle-threshold")
            .setRequired(false)
            .setStability(Stability.COMMUNITY)
            .build();

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = DistributableEjbSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(REGISTRATION.getPathElement());
        ResourceDescriptor descriptor = ResourceDescriptor.builder(resolver)
                .addAttributes(CACHE_ATTRIBUTE_GROUP.getAttributes())
                .addAttributes(List.of(MARSHALLER, MAX_ACTIVE_TIMERS, IDLE_THRESHOLD))
                .addCapability(CAPABILITY)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .build();
        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(REGISTRATION, resolver).build());
        ManagementResourceRegistrar.of(descriptor).register(registration);
        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        OptionalInt maxActiveTimers = Optional.ofNullable(MAX_ACTIVE_TIMERS.resolveModelAttribute(context, model).asIntOrNull()).map(OptionalInt::of).orElse(OptionalInt.empty());
        Optional<Duration> idleThreshold = Optional.ofNullable(IDLE_THRESHOLD.resolve(context, model));
        Function<Module, ByteBufferMarshaller> marshallerFactory = MARSHALLER.resolve(context, model);
        TimerManagementConfiguration config = new TimerManagementConfiguration() {
            @Override
            public Function<Module, ByteBufferMarshaller> getMarshallerFactory() {
                return marshallerFactory;
            }

            @Override
            public OptionalInt getSizeThreshold() {
                return maxActiveTimers;
            }

            @Override
            public Optional<Duration> getIdleThreshold() {
                return idleThreshold;
            }
        };
        return CapabilityServiceInstaller.builder(CAPABILITY, new InfinispanTimerManagementProvider(config, CACHE_ATTRIBUTE_GROUP.resolve(context, model))).build();
    }
}
