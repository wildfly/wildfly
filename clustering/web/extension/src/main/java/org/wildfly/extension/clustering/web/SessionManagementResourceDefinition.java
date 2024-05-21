/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.List;
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
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.web.service.routing.RoutingProvider;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementConfiguration;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;

/**
 * Base definition for session management resources.
 * @author Paul Ferraro
 */
public abstract class SessionManagementResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfigurator, ResourceModelResolver<DistributableSessionManagementConfiguration<DeploymentUnit>> {

    static final RuntimeCapability<Void> SESSION_MANAGEMENT_PROVIDER = RuntimeCapability.Builder.of(DistributableSessionManagementProvider.SERVICE_DESCRIPTOR)
            .addRequirements(RoutingProvider.SERVICE_DESCRIPTOR.getName())
            .setAllowMultipleRegistrations(true)
            .build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        GRANULARITY("granularity", ModelType.STRING, null) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(EnumValidator.create(SessionGranularity.class));
            }
        },
        MARSHALLER("marshaller", ModelType.STRING, new ModelNode(SessionMarshallerFactory.JBOSS.name())) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(EnumValidator.create(SessionMarshallerFactory.class));
            }
        }
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(defaultValue == null)
                    .setDefaultValue(defaultValue)
                    .setFlags(Flag.RESTART_RESOURCE_SERVICES)
                ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    private final UnaryOperator<ResourceDescriptor> configurator;

    public SessionManagementResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator) {
        super(path, DistributableWebExtension.SUBSYSTEM_RESOLVER.createChildResolver(path, PathElement.pathElement("session-management")));
        this.configurator = configurator;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addAttributes(Attribute.class)
                .addCapabilities(List.of(SESSION_MANAGEMENT_PROVIDER))
                ;
        ResourceServiceHandler handler = ResourceServiceHandler.of(ResourceOperationRuntimeHandler.configureService(this));
        new SimpleResourceRegistrar(descriptor, handler).register(registration);

        new NoAffinityResourceDefinition().register(registration);
        new LocalAffinityResourceDefinition().register(registration);

        return registration;
    }

    @Override
    public DistributableSessionManagementConfiguration<DeploymentUnit> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        SessionGranularity granularity = SessionGranularity.valueOf(Attribute.GRANULARITY.resolveModelAttribute(context, model).asString());
        SessionMarshallerFactory marshallerFactory = SessionMarshallerFactory.valueOf(Attribute.MARSHALLER.resolveModelAttribute(context, model).asString());
        return new DistributableSessionManagementConfiguration<>() {
            @Override
            public SessionAttributePersistenceStrategy getAttributePersistenceStrategy() {
                return granularity.getAttributePersistenceStrategy();
            }

            @Override
            public Function<DeploymentUnit, ByteBufferMarshaller> getMarshallerFactory() {
                return marshallerFactory;
            }
        };
    }
}
