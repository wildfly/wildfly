/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.sso.hotrod;

import java.util.List;
import java.util.function.Consumer;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.configuration.RemoteCacheConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.jboss.as.controller.ServiceNameFactory;
import org.wildfly.clustering.cache.infinispan.remote.RemoteCacheConfiguration;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.clustering.infinispan.client.service.RemoteCacheConfigurationServiceInstallerFactory;
import org.wildfly.clustering.infinispan.client.service.RemoteCacheServiceInstallerFactory;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.session.infinispan.remote.user.HotRodUserManagerFactory;
import org.wildfly.clustering.web.service.WebDeploymentServiceDescriptor;
import org.wildfly.clustering.web.service.user.DistributableUserManagementProvider;
import org.wildfly.common.function.Functions;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class HotRodUserManagementProvider implements DistributableUserManagementProvider {
    private static final String DEFAULT_CONFIGURATION = """
{
    "distributed-cache": {
        "mode" : "SYNC",
        "transaction" : {
            "mode" : "NON_XA",
            "locking" : "PESSIMISTIC"
        }
    }
}""";

    private final BinaryServiceConfiguration configuration;

    public HotRodUserManagementProvider(BinaryServiceConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Iterable<ServiceInstaller> getServiceInstallers(String name) {
        String templateName = this.configuration.getChildName();

        Consumer<RemoteCacheConfigurationBuilder> configurator = new Consumer<>() {
            @Override
            public void accept(RemoteCacheConfigurationBuilder builder) {
                // Near caching not compatible with max-idle expiration.
                builder.forceReturnValues(false).nearCacheMode(NearCacheMode.INVALIDATED).transactionMode(TransactionMode.NONE);
                if (templateName != null) {
                    builder.templateName(templateName);
                } else {
                    builder.configuration(DEFAULT_CONFIGURATION);
                }
            }
        };
        BinaryServiceConfiguration configuration = this.configuration.withChildName(name);
        ServiceInstaller configurationInstaller = new RemoteCacheConfigurationServiceInstallerFactory(configurator).apply(configuration);
        ServiceInstaller cacheInstaller = RemoteCacheServiceInstallerFactory.INSTANCE.apply(configuration);

        ServiceDependency<RemoteCache<?, ?>> cache = configuration.getServiceDependency(HotRodServiceDescriptor.REMOTE_CACHE);
        RemoteCacheConfiguration cacheConfiguration = new RemoteCacheConfiguration() {
            @SuppressWarnings("unchecked")
            @Override
            public <K, V> RemoteCache<K, V> getCache() {
                return (RemoteCache<K, V>) cache.get();
            }
        };
        ServiceInstaller installer = ServiceInstaller.builder(HotRodUserManagerFactory::new, Functions.constantSupplier(cacheConfiguration))
                .provides(ServiceNameFactory.resolveServiceName(WebDeploymentServiceDescriptor.USER_MANAGER_FACTORY, name))
                .requires(cache)
                .build();
        return List.of(configurationInstaller, cacheInstaller, installer);
    }
}
