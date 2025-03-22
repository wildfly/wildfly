/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web;

import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.EnumAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.CacheConfigurationAttributeGroup;
import org.wildfly.clustering.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.clustering.web.service.routing.RoutingProvider;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementConfiguration;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.extension.clustering.web.session.DistributableSessionManagementProviderFactory;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Registers a resource definition for a session management provider.
 * @author Paul Ferraro
 */
public abstract class SessionManagementResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator, UnaryOperator<ResourceDescriptor.Builder> {
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(DistributableSessionManagementProvider.SERVICE_DESCRIPTOR)
            .addRequirements(RoutingProvider.SERVICE_DESCRIPTOR.getName())
            .setAllowMultipleRegistrations(true)
            .build();

    static final EnumAttributeDefinition<SessionGranularity> GRANULARITY = new EnumAttributeDefinition.Builder<>("granularity", SessionGranularity.class).build();
    static final EnumAttributeDefinition<SessionMarshallerFactory> MARSHALLER = new EnumAttributeDefinition.Builder<>("marshaller", SessionMarshallerFactory.JBOSS).build();

    private final ResourceRegistration registration;
    private final CacheConfigurationAttributeGroup cacheAttributeGroup;
    private final DistributableSessionManagementProviderFactory providerFactory;

    SessionManagementResourceDefinitionRegistrar(ResourceRegistration registration, CacheConfigurationAttributeGroup cacheAttributeGroup, DistributableSessionManagementProviderFactory providerFactory) {
        this.registration = registration;
        this.cacheAttributeGroup = cacheAttributeGroup;
        this.providerFactory = providerFactory;
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return builder.addCapability(CAPABILITY)
                .addAttributes(this.cacheAttributeGroup.getAttributes())
                .addAttributes(List.of(GRANULARITY, MARSHALLER))
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                ;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = DistributableWebSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(this.registration.getPathElement(), PathElement.pathElement("session-management"));
        ResourceDescriptor descriptor = this.apply(ResourceDescriptor.builder(resolver)).build();
        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(this.registration, resolver).build());
        ManagementResourceRegistrar.of(descriptor).register(registration);

        new NoAffinityResourceDefinitionRegistrar().register(registration, context);
        new LocalAffinityResourceDefinitionRegistrar().register(registration, context);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        SessionGranularity granularity = GRANULARITY.resolve(context, model);
        SessionMarshallerFactory marshallerFactory = MARSHALLER.resolve(context, model);
        DistributableSessionManagementConfiguration<DeploymentUnit> configuration = new DistributableSessionManagementConfiguration<>() {
            @Override
            public SessionAttributePersistenceStrategy getAttributePersistenceStrategy() {
                return granularity.getAttributePersistenceStrategy();
            }

            @Override
            public Function<DeploymentUnit, ByteBufferMarshaller> getMarshallerFactory() {
                return marshallerFactory;
            }
        };
        BinaryServiceConfiguration cacheConfiguration = this.cacheAttributeGroup.resolve(context, model);
        DistributableSessionManagementProviderFactory providerFactory = this.providerFactory;
        return CapabilityServiceInstaller.builder(CAPABILITY, ServiceDependency.on(RouteLocatorProvider.SERVICE_DESCRIPTOR, context.getCurrentAddressValue()).map(new Function<>() {
            @Override
            public DistributableSessionManagementProvider apply(RouteLocatorProvider locatorProvider) {
                return providerFactory.createSessionManagementProvider(configuration, cacheConfiguration, locatorProvider);
            }
        })).build();
    }
}
