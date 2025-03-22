/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.client.service.HotRodCacheConfigurationDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.CacheConfigurationDescriptor;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementConfiguration;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.extension.clustering.web.session.hotrod.HotRodSessionManagementProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;

/**
 * Registers a resource definition for a HotRod session management provider.
 * @author Paul Ferraro
 */
public class HotRodSessionManagementResourceDefinitionRegistrar extends SessionManagementResourceDefinitionRegistrar {

    static final CacheConfigurationDescriptor CACHE_CONFIGURATION = new HotRodCacheConfigurationDescriptor(CAPABILITY);

    static final AttributeDefinition EXPIRATION_THREAD_POOL_SIZE = new SimpleAttributeDefinitionBuilder("expiration-thread-pool-size", ModelType.INT)
            .setAllowExpression(true)
            .setRequired(false)
            .setDefaultValue(new ModelNode(16))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    HotRodSessionManagementResourceDefinitionRegistrar() {
        super(SessionManagementResourceRegistration.HOTROD, CACHE_CONFIGURATION);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .addAttributes(List.of(EXPIRATION_THREAD_POOL_SIZE))
                .requireSingletonChildResource(AffinityResourceRegistration.LOCAL)
                // Workaround for WFCORE-7188
                .withOperationTransformation(Set.of(ModelDescriptionConstants.ADD), AddResourceOperationStepHandler::new)
                ;
    }

    @Override
    public DistributableSessionManagementProvider createSessionManagementProvider(DistributableSessionManagementConfiguration<DeploymentUnit> configuration, BinaryServiceConfiguration cacheConfiguration, Supplier<RouteLocatorProvider> locatorProvider) {
        return new HotRodSessionManagementProvider(configuration, cacheConfiguration, locatorProvider);
    }
}
