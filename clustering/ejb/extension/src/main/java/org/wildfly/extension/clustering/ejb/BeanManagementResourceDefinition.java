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
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.ejb.bean.BeanDeploymentMarshallingContext;
import org.wildfly.clustering.ejb.bean.BeanManagementConfiguration;
import org.wildfly.clustering.ejb.bean.BeanManagementProvider;
import org.wildfly.clustering.ejb.cache.bean.BeanMarshallerFactory;
import org.wildfly.clustering.ejb.remote.ClientMappingsRegistryProvider;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;

/**
 * Common resource definition for bean management resources.
 * @author Paul Ferraro
 */
public abstract class BeanManagementResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfigurator, ResourceModelResolver<BeanManagementConfiguration> {

    static final RuntimeCapability<Void> BEAN_MANAGEMENT_PROVIDER = RuntimeCapability.Builder.of(BeanManagementProvider.SERVICE_DESCRIPTOR)
            .addRequirements(ClientMappingsRegistryProvider.SERVICE_DESCRIPTOR.getName())
            .setAllowMultipleRegistrations(true)
            .build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        MAX_ACTIVE_BEANS("max-active-beans", ModelType.INT) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(true).setValidator(new IntRangeValidator(1));
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setRequired(false)
                    .setFlags(Flag.RESTART_RESOURCE_SERVICES)
                ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    private final UnaryOperator<ResourceDescriptor> configurator;

    BeanManagementResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator) {
        super(path, DistributableEjbExtension.SUBSYSTEM_RESOLVER.createChildResolver(path, PathElement.pathElement("bean-management")));
        this.configurator = configurator;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(List.of(BEAN_MANAGEMENT_PROVIDER))
                ;

        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);

        new SimpleResourceRegistrar(this.configurator.apply(descriptor), ResourceServiceHandler.of(handler)).register(registration);

        return registration;
    }

    @Override
    public BeanManagementConfiguration resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        OptionalInt maxActiveBeans = Optional.ofNullable(Attribute.MAX_ACTIVE_BEANS.getDefinition().resolveModelAttribute(context, model).asIntOrNull()).map(OptionalInt::of).orElse(OptionalInt.empty());
        return new BeanManagementConfiguration() {
            @Override
            public OptionalInt getMaxActiveBeans() {
                return maxActiveBeans;
            }

            @Override
            public Function<BeanDeploymentMarshallingContext, ByteBufferMarshaller> getMarshallerFactory() {
                // Currently hard-coded to use JBoss Marshalling
                return BeanMarshallerFactory.JBOSS;
            }
        };
    }
}