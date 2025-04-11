/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.ejb;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.Module;
import org.wildfly.clustering.ejb.infinispan.timer.InfinispanTimerManagementProvider;
import org.wildfly.clustering.ejb.timer.TimerManagementConfiguration;
import org.wildfly.clustering.ejb.timer.TimerManagementProvider;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class InfinispanTimerManagementResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfigurator {

    static PathElement pathElement(String name) {
        return PathElement.pathElement("infinispan-timer-management", name);
    }
    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(TimerManagementProvider.SERVICE_DESCRIPTOR).build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        CACHE_CONTAINER("cache-container", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setRequired(true)
                        .setCapabilityReference(CapabilityReferenceRecorder.builder(CAPABILITY, InfinispanServiceDescriptor.DEFAULT_CACHE_CONFIGURATION).build())
                        ;
            }
        },
        CACHE("cache", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setCapabilityReference(CapabilityReferenceRecorder.builder(CAPABILITY, InfinispanServiceDescriptor.CACHE_CONFIGURATION).withParentAttribute(CACHE_CONTAINER.getDefinition()).build());
            }
        },
        MAX_ACTIVE_TIMERS("max-active-timers", ModelType.INT) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new IntRangeValidator(1));
            }
        },
        MARSHALLER("marshaller", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(new ModelNode(TimerContextMarshallerFactory.JBOSS.name()))
                        .setValidator(EnumValidator.create(TimerContextMarshallerFactory.class))
                        ;
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setFlags(Flag.RESTART_RESOURCE_SERVICES)
                ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    private final ResourceModelResolver<BinaryServiceConfiguration> resolver = BinaryServiceConfiguration.resolver(Attribute.CACHE_CONTAINER.getDefinition(), Attribute.CACHE.getDefinition());

    InfinispanTimerManagementResourceDefinition() {
        super(WILDCARD_PATH, DistributableEjbExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        // create the ManagementResourceRegistration for the infinispan-bean-management resource
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        // create the resolver for the infinispan-bean-management resource
        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(List.of(CAPABILITY))
                ;
        // create the service handler for the infinispan-brean-management resource
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        // register the resource descriptor and the handler
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        OptionalInt maxActiveTimers = Optional.ofNullable(Attribute.MAX_ACTIVE_TIMERS.getDefinition().resolveModelAttribute(context, model).asIntOrNull()).map(OptionalInt::of).orElse(OptionalInt.empty());
        Function<Module, ByteBufferMarshaller> marshallerFactory = TimerContextMarshallerFactory.valueOf(Attribute.MARSHALLER.resolveModelAttribute(context, model).asString());
        TimerManagementConfiguration config = new TimerManagementConfiguration() {
            @Override
            public Function<Module, ByteBufferMarshaller> getMarshallerFactory() {
                return marshallerFactory;
            }

            @Override
            public OptionalInt getMaxActiveTimers() {
                return maxActiveTimers;
            }
        };
        return CapabilityServiceInstaller.builder(CAPABILITY, new InfinispanTimerManagementProvider(config, this.resolver.resolve(context, model))).build();
    }
}
