/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.clustering.web.infinispan.session;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.eviction.EvictionType;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.function.Consumers;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.infinispan.spi.EvictableDataContainer;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceConfigurator;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationServiceConfigurator;
import org.wildfly.clustering.marshalling.spi.Marshallability;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.clustering.spi.NodeFactory;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.session.SessionManagerFactory;
import org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration;

public class InfinispanSessionManagerFactoryServiceConfigurator<C extends Marshallability, L> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, InfinispanSessionManagerFactoryConfiguration<C, L>, Supplier<SessionManagerFactory<L, TransactionBatch>> {
    public static final String DEFAULT_CACHE_CONTAINER = "web";

    private final SessionManagerFactoryConfiguration<C, L> configuration;
    private final String containerName;
    private final CapabilityServiceConfigurator configurationConfigurator;
    private final CapabilityServiceConfigurator cacheConfigurator;

    private volatile SupplierDependency<NodeFactory<Address>> group;
    private volatile SupplierDependency<KeyAffinityServiceFactory> affinityFactory;
    private volatile SupplierDependency<CommandDispatcherFactory> dispatcherFactory;
    @SuppressWarnings("rawtypes")
    private volatile Supplier<Cache> cache;

    public InfinispanSessionManagerFactoryServiceConfigurator(SessionManagerFactoryConfiguration<C, L> configuration) {
        super(ServiceName.JBOSS.append("clustering", "web", configuration.getDeploymentName()));
        this.configuration = configuration;

        ServiceName baseServiceName = ServiceName.JBOSS.append("infinispan");
        String configCacheName = this.configuration.getCacheName();
        ServiceName configServiceName = ServiceName.parse((configCacheName != null) ? configCacheName : DEFAULT_CACHE_CONTAINER);
        if (!baseServiceName.isParentOf(configServiceName)) {
            configServiceName = baseServiceName.append(configServiceName);
        }
        this.containerName = ((configServiceName.length() > 3) ? configServiceName.getParent() : configServiceName).getSimpleName();
        String templateCacheName =  (configServiceName.length() > 3) ? configServiceName.getSimpleName() : null;
        String cacheName = this.configuration.getDeploymentName();

        // Ensure eviction and expiration are disabled
        @SuppressWarnings("deprecation")
        Consumer<ConfigurationBuilder> configurator = builder -> {
            // Ensure expiration is not enabled on cache
            ExpirationConfiguration expiration = builder.expiration().create();
            if ((expiration.lifespan() >= 0) || (expiration.maxIdle() >= 0)) {
                builder.expiration().lifespan(-1).maxIdle(-1);
                InfinispanWebLogger.ROOT_LOGGER.expirationDisabled(InfinispanCacheRequirement.CONFIGURATION.resolve(this.containerName, templateCacheName));
            }

            int size = configuration.getMaxActiveSessions();
            EvictionStrategy strategy = (size > 0) ? EvictionStrategy.REMOVE : EvictionStrategy.MANUAL;
            builder.memory().evictionStrategy(strategy).evictionType(EvictionType.COUNT).storageType(StorageType.OBJECT).size(size);
            if (strategy.isEnabled()) {
                // Only evict creation meta-data entries
                // We will cascade eviction to the remaining entries for a given session
                builder.dataContainer().dataContainer(EvictableDataContainer.createDataContainer(builder, size, SessionCreationMetaDataKey.class::isInstance));
            }
        };

        this.configurationConfigurator = new TemplateConfigurationServiceConfigurator(ServiceName.parse(InfinispanCacheRequirement.CONFIGURATION.resolve(this.containerName, cacheName)), this.containerName, cacheName, templateCacheName, configurator);
        this.cacheConfigurator = new CacheServiceConfigurator<>(ServiceName.parse(InfinispanCacheRequirement.CACHE.resolve(this.containerName, cacheName)), this.containerName, cacheName);
    }

    @Override
    public SessionManagerFactory<L, TransactionBatch> get() {
        return new InfinispanSessionManagerFactory<>(this);
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.configurationConfigurator.configure(support);
        this.cacheConfigurator.configure(support);

        this.affinityFactory = new ServiceSupplierDependency<>(InfinispanRequirement.KEY_AFFINITY_FACTORY.getServiceName(support, this.containerName));
        this.dispatcherFactory = new ServiceSupplierDependency<>(ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(support, this.containerName));
        this.group = new ServiceSupplierDependency<>(ClusteringCacheRequirement.GROUP.getServiceName(support, this.containerName, this.configuration.getServerName()));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        this.configurationConfigurator.build(target).install();
        this.cacheConfigurator.build(target).install();

        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<SessionManagerFactory<L, TransactionBatch>> factory = new CompositeDependency(this.group, this.affinityFactory, this.dispatcherFactory).register(builder).provides(this.getServiceName());
        this.cache = builder.requires(this.cacheConfigurator.getServiceName());
        Service service = new FunctionalService<>(factory, Function.identity(), this, Consumers.close());
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public SessionManagerFactoryConfiguration<C, L> getSessionManagerFactoryConfiguration() {
        return this.configuration;
    }

    @Override
    public <K, V> Cache<K, V> getCache() {
        return this.cache.get();
    }

    @Override
    public KeyAffinityServiceFactory getKeyAffinityServiceFactory() {
        return this.affinityFactory.get();
    }

    @Override
    public CommandDispatcherFactory getCommandDispatcherFactory() {
        return this.dispatcherFactory.get();
    }

    @Override
    public NodeFactory<Address> getMemberFactory() {
        return this.group.get();
    }
}
