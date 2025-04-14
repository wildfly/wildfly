/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.sso.infinispan;

import java.util.List;

import org.infinispan.Cache;
import org.jboss.as.controller.ServiceNameFactory;
import org.wildfly.clustering.cache.infinispan.embedded.EmbeddedCacheConfiguration;
import org.wildfly.clustering.infinispan.service.CacheConfigurationServiceInstaller;
import org.wildfly.clustering.infinispan.service.CacheServiceInstaller;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.session.infinispan.embedded.user.InfinispanUserManagerFactory;
import org.wildfly.clustering.web.service.user.DistributableUserManagementProvider;
import org.wildfly.common.function.Functions;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * An Infinispan cache-based {@link DistributableUserManagementProvider}.
 * @author Paul Ferraro
 */
public class InfinispanUserManagementProvider implements DistributableUserManagementProvider {

    private final BinaryServiceConfiguration configuration;

    public InfinispanUserManagementProvider(BinaryServiceConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Iterable<ServiceInstaller> getServiceInstallers(String name) {
        BinaryServiceConfiguration configuration = this.configuration.withChildName(name);

        ServiceInstaller configurationInstaller = new CacheConfigurationServiceInstaller(configuration, CacheConfigurationServiceInstaller.fromTemplate(this.configuration));
        ServiceInstaller cacheInstaller = new CacheServiceInstaller(configuration);

        ServiceDependency<Cache<?, ?>> cache = configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE);
        EmbeddedCacheConfiguration cacheConfiguration = new EmbeddedCacheConfiguration() {
            @SuppressWarnings("unchecked")
            @Override
            public <K, V> Cache<K, V> getCache() {
                return (Cache<K, V>) cache.get();
            }
        };
        ServiceInstaller installer = ServiceInstaller.builder(InfinispanUserManagerFactory::new, Functions.constantSupplier(cacheConfiguration))
                .provides(ServiceNameFactory.resolveServiceName(DistributableUserManagementProvider.USER_MANAGER_FACTORY, name))
                .requires(cache)
                .build();

        return List.of(configurationInstaller, cacheInstaller, installer);
    }
}
