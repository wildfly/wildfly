/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing.infinispan;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.Configurations;
import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceController;
import org.wildfly.clustering.cache.Key;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.infinispan.registry.CacheContainerRegistry;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.web.service.deployment.WebDeploymentConfiguration;
import org.wildfly.clustering.web.service.deployment.WebDeploymentServiceDescriptor;
import org.wildfly.extension.clustering.web.routing.LocalRouteLocatorProvider;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Route locator provider for Infinispan-based implementations.
 * Falls back to local route location if backing cache is local.
 * @author Paul Ferraro
 */
public class InfinispanRouteLocatorProvider extends LocalRouteLocatorProvider {
    private final BiFunction<Cache<Key<String>, ?>, CacheContainerRegistry<String, Void>, UnaryOperator<String>> factory;

    InfinispanRouteLocatorProvider(BiFunction<Cache<Key<String>, ?>, CacheContainerRegistry<String, Void>, UnaryOperator<String>> factory) {
        this.factory = factory;
    }

    @Override
    public ServiceInstaller getServiceInstaller(BinaryServiceConfiguration configuration, WebDeploymentConfiguration deployment) {
        ServiceInstaller localInstaller = super.getServiceInstaller(configuration, deployment);
        DeploymentUnit unit = deployment.getDeploymentUnit();
        ServiceDependency<Configuration> cacheConfiguration = configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE_CONFIGURATION);
        ServiceDependency<Cache<Key<String>, ?>> cache = configuration.withChildName(deployment.getDeploymentName()).getServiceDependency(InfinispanServiceDescriptor.CACHE).map(Cache.class::cast);
        ServiceDependency<CacheContainerRegistry<String, Void>> registry = configuration.withChildName(deployment.getServerName()).getServiceDependency(ClusteringServiceDescriptor.REGISTRY).map(CacheContainerRegistry.class::cast);
        Supplier<UnaryOperator<String>> factory = cache.combine(registry, this.factory);
        return ServiceInstaller.builder(new ServiceInstaller() {
            @Override
            public ServiceController<?> install(RequirementServiceTarget target) {
                // Fallback to local routing if cache is local
                if (!Configurations.needSegments(cacheConfiguration.get())) {
                    return localInstaller.install(target);
                }

                return ServiceInstaller.builder(factory)
                        .provides(WebDeploymentServiceDescriptor.ROUTE_LOCATOR.resolve(unit))
                        .requires(List.of(cache, registry))
                        .build()
                        .install(target);
            }
        }, unit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT)).requires(cacheConfiguration).build();
    }
}
