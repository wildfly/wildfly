/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web;

import java.util.Set;
import java.util.function.Supplier;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.wildfly.clustering.infinispan.service.InfinispanCacheConfigurationDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.CacheConfigurationDescriptor;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementConfiguration;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.extension.clustering.web.session.infinispan.InfinispanSessionManagementProvider;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;

/**
 * Registers a resource definition for an Infinispan session management provider.
 * @author Paul Ferraro
 */
public class InfinispanSessionManagementResourceDefinitionRegistrar extends SessionManagementResourceDefinitionRegistrar {

    static final CacheConfigurationDescriptor CACHE_CONFIGURATION = new InfinispanCacheConfigurationDescriptor(CAPABILITY);

    InfinispanSessionManagementResourceDefinitionRegistrar() {
        super(SessionManagementResourceRegistration.INFINISPAN, CACHE_CONFIGURATION);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .requireSingletonChildResource(AffinityResourceRegistration.PRIMARY_OWNER)
                // Workaround for WFCORE-7188
                .withOperationTransformation(Set.of(ModelDescriptionConstants.ADD), AddResourceOperationStepHandler::new)
                ;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);

        new PrimaryOwnerAffinityResourceDefinitionRegistrar().register(registration, context);
        new RankedAffinityResourceDefinitionRegistrar().register(registration, context);

        return registration;
    }

    @Override
    public DistributableSessionManagementProvider createSessionManagementProvider(DistributableSessionManagementConfiguration<DeploymentUnit> configuration, BinaryServiceConfiguration cacheConfiguration, Supplier<RouteLocatorProvider> locatorProvider) {
        return new InfinispanSessionManagementProvider(configuration, cacheConfiguration, locatorProvider);
    }
}
