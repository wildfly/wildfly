/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.session.infinispan;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.eviction.EvictionStrategy;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.wildfly.clustering.cache.infinispan.embedded.container.DataContainerConfigurationBuilder;
import org.wildfly.clustering.infinispan.service.CacheConfigurationServiceInstaller;
import org.wildfly.clustering.infinispan.service.CacheServiceInstaller;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.infinispan.dispatcher.CacheContainerCommandDispatcherFactory;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.clustering.session.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.session.infinispan.embedded.InfinispanSessionManagerFactory;
import org.wildfly.clustering.session.infinispan.embedded.InfinispanSessionManagerFactoryConfiguration;
import org.wildfly.clustering.session.infinispan.embedded.metadata.SessionMetaDataKey;
import org.wildfly.clustering.session.spec.servlet.HttpSessionActivationListenerProvider;
import org.wildfly.clustering.session.spec.servlet.HttpSessionProvider;
import org.wildfly.clustering.web.service.WebDeploymentServiceDescriptor;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementConfiguration;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.common.function.Functions;
import org.wildfly.extension.clustering.web.session.AbstractSessionManagementProvider;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

import jakarta.servlet.ServletContext;

/**
 * An Infinispan cache-based {@link DistributableSessionManagementProvider}.
 * @author Paul Ferraro
 */
public class InfinispanSessionManagementProvider extends AbstractSessionManagementProvider {

    public InfinispanSessionManagementProvider(DistributableSessionManagementConfiguration<DeploymentUnit> configuration, BinaryServiceConfiguration cacheConfiguration, Supplier<RouteLocatorProvider> locatorProviderFactory) {
        super(configuration, cacheConfiguration, locatorProviderFactory);
    }

    @Override
    public <C> DeploymentServiceInstaller getSessionManagerFactoryServiceInstaller(SessionManagerFactoryConfiguration<C> configuration) {
        BinaryServiceConfiguration templateCacheConfiguration = this.getCacheConfiguration();
        BinaryServiceConfiguration deploymentCacheConfiguration = templateCacheConfiguration.withChildName(configuration.getDeploymentName());

        UnaryOperator<ConfigurationBuilder> configurator = new UnaryOperator<>() {
            @Override
            public ConfigurationBuilder apply(ConfigurationBuilder builder) {
                // Ensure expiration is not enabled on cache
                ExpirationConfiguration expiration = builder.expiration().create();
                if ((expiration.lifespan() >= 0) || (expiration.maxIdle() >= 0)) {
                    builder.expiration().lifespan(-1).maxIdle(-1);
                }

                OptionalInt size = configuration.getMaxActiveSessions();
                EvictionStrategy strategy = size.isPresent() ? EvictionStrategy.REMOVE : EvictionStrategy.NONE;
                builder.memory().storage(StorageType.HEAP)
                        .whenFull(strategy)
                        .maxCount(size.orElse(0))
                        ;
                if (strategy.isEnabled()) {
                    // Only evict creation meta-data entries
                    // We will cascade eviction to the remaining entries for a given session
                    builder.addModule(DataContainerConfigurationBuilder.class).evictable(SessionMetaDataKey.class::isInstance);
                }
                PersistenceConfiguration persistence = builder.persistence().create();
                // If cache is configured to passivate and purge on startup, but application does not define a passivation threshold, then remove useless stores
                if (size.isEmpty() && persistence.passivation() && persistence.stores().stream().allMatch(StoreConfiguration::purgeOnStartup)) {
                    builder.persistence().passivation(false).clearStores();
                }
                return builder;
            }
        };
        DeploymentServiceInstaller configurationInstaller = new CacheConfigurationServiceInstaller(deploymentCacheConfiguration, CacheConfigurationServiceInstaller.fromTemplate(templateCacheConfiguration).map(configurator));
        DeploymentServiceInstaller cacheInstaller = new CacheServiceInstaller(deploymentCacheConfiguration);

        ServiceDependency<CacheContainerCommandDispatcherFactory> commandDispatcherFactory = deploymentCacheConfiguration.getServiceDependency(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY).map(CacheContainerCommandDispatcherFactory.class::cast);
        ServiceDependency<Cache<?, ?>> cache = deploymentCacheConfiguration.getServiceDependency(InfinispanServiceDescriptor.CACHE);
        InfinispanSessionManagerFactoryConfiguration factoryConfiguration = new InfinispanSessionManagerFactoryConfiguration() {
            @SuppressWarnings("unchecked")
            @Override
            public <K, V> Cache<K, V> getCache() {
                return (Cache<K, V>) cache.get();
            }

            @Override
            public CacheContainerCommandDispatcherFactory getCommandDispatcherFactory() {
                return commandDispatcherFactory.get();
            }
        };
        Supplier<SessionManagerFactory<ServletContext, C>> factory = new Supplier<>() {
            @Override
            public SessionManagerFactory<ServletContext, C> get() {
                return new InfinispanSessionManagerFactory<>(configuration, HttpSessionProvider.INSTANCE, HttpSessionActivationListenerProvider.INSTANCE, factoryConfiguration);
            }
        };
        DeploymentServiceInstaller installer = ServiceInstaller.builder(factory)
                .provides(ServiceNameFactory.resolveServiceName(WebDeploymentServiceDescriptor.SESSION_MANAGER_FACTORY, configuration.getDeploymentName()))
                .requires(List.of(cache, commandDispatcherFactory))
                .onStop(Functions.closingConsumer())
                .build();

        return DeploymentServiceInstaller.combine(configurationInstaller, cacheInstaller, installer);
    }
}
