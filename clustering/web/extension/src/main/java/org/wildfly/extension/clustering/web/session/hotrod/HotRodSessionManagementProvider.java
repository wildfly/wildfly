/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.session.hotrod;

import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.servlet.ServletContext;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.configuration.RemoteCacheConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.wildfly.clustering.cache.infinispan.remote.RemoteCacheConfiguration;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.clustering.infinispan.client.service.RemoteCacheConfigurationServiceInstallerFactory;
import org.wildfly.clustering.infinispan.client.service.RemoteCacheServiceInstallerFactory;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.clustering.session.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.session.infinispan.remote.HotRodSessionManagerFactory;
import org.wildfly.clustering.session.spec.servlet.HttpSessionActivationListenerProvider;
import org.wildfly.clustering.session.spec.servlet.HttpSessionProvider;
import org.wildfly.clustering.web.service.WebDeploymentServiceDescriptor;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementConfiguration;
import org.wildfly.common.function.Functions;
import org.wildfly.extension.clustering.web.session.AbstractSessionManagementProvider;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class HotRodSessionManagementProvider extends AbstractSessionManagementProvider {
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

    public HotRodSessionManagementProvider(DistributableSessionManagementConfiguration<DeploymentUnit> configuration, BinaryServiceConfiguration cacheConfiguration, RouteLocatorProvider locatorProvider) {
        super(configuration, cacheConfiguration, locatorProvider);
    }

    @Override
    public <C> DeploymentServiceInstaller getSessionManagerFactoryServiceInstaller(SessionManagerFactoryConfiguration<C> configuration) {
        BinaryServiceConfiguration deploymentCacheConfiguration = this.getCacheConfiguration().withChildName(configuration.getDeploymentName());
        String templateName = this.getCacheConfiguration().getChildName();

        Consumer<RemoteCacheConfigurationBuilder> configurator = new Consumer<>() {
            @Override
            public void accept(RemoteCacheConfigurationBuilder builder) {
                // Near caching not compatible with max-idle expiration.
                builder.forceReturnValues(false).nearCacheMode(NearCacheMode.DISABLED).transactionMode(TransactionMode.NONE);
                if (templateName != null) {
                    builder.templateName(templateName);
                } else {
                    builder.configuration(DEFAULT_CONFIGURATION);
                }
            }
        };
        DeploymentServiceInstaller configurationInstaller = new RemoteCacheConfigurationServiceInstallerFactory(configurator).apply(deploymentCacheConfiguration);
        DeploymentServiceInstaller cacheInstaller = RemoteCacheServiceInstallerFactory.INSTANCE.apply(deploymentCacheConfiguration);

        ServiceDependency<RemoteCache<?, ?>> cache = deploymentCacheConfiguration.getServiceDependency(HotRodServiceDescriptor.REMOTE_CACHE);
        RemoteCacheConfiguration cacheConfiguration = new RemoteCacheConfiguration() {
            @SuppressWarnings("unchecked")
            @Override
            public <CK, CV> RemoteCache<CK, CV> getCache() {
                return (RemoteCache<CK, CV>) cache.get();
            }
        };
        Supplier<SessionManagerFactory<ServletContext, C>> factory = new Supplier<>() {
            @Override
            public SessionManagerFactory<ServletContext, C> get() {
                return new HotRodSessionManagerFactory<>(configuration, HttpSessionProvider.INSTANCE, HttpSessionActivationListenerProvider.INSTANCE, cacheConfiguration);
            }
        };
        DeploymentServiceInstaller installer = ServiceInstaller.builder(factory)
                .provides(ServiceNameFactory.resolveServiceName(WebDeploymentServiceDescriptor.SESSION_MANAGER_FACTORY, configuration.getDeploymentName()))
                .requires(cache)
                .onStop(Functions.closingConsumer())
                .build();

        return DeploymentServiceInstaller.combine(configurationInstaller, cacheInstaller, installer);
    }
}
