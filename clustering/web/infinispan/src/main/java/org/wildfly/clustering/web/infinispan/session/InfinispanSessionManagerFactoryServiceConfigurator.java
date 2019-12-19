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
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.infinispan.spi.DataContainerConfigurationBuilder;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceConfigurator;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationServiceConfigurator;
import org.wildfly.clustering.marshalling.spi.Marshallability;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.clustering.spi.NodeFactory;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.web.session.SessionManagerFactory;
import org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration;

public class InfinispanSessionManagerFactoryServiceConfigurator<C extends Marshallability, L> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, InfinispanSessionManagerFactoryConfiguration<C, L>, Supplier<SessionManagerFactory<L, TransactionBatch>>, Consumer<ConfigurationBuilder> {
    public static final String DEFAULT_CACHE_CONTAINER = "web";

    private final InfinispanSessionManagementConfiguration configuration;
    private final SessionManagerFactoryConfiguration<C, L> factoryConfiguration;

    private volatile ServiceConfigurator configurationConfigurator;
    private volatile ServiceConfigurator cacheConfigurator;

    private volatile SupplierDependency<NodeFactory<Address>> group;
    private volatile SupplierDependency<KeyAffinityServiceFactory> affinityFactory;
    private volatile SupplierDependency<CommandDispatcherFactory> dispatcherFactory;
    @SuppressWarnings("rawtypes")
    private volatile Supplier<Cache> cache;

    public InfinispanSessionManagerFactoryServiceConfigurator(InfinispanSessionManagementConfiguration configuration, SessionManagerFactoryConfiguration<C, L> factoryConfiguration) {
        super(ServiceName.JBOSS.append("clustering", "web", factoryConfiguration.getDeploymentName()));
        this.configuration = configuration;
        this.factoryConfiguration = factoryConfiguration;
    }

    @Override
    public SessionManagerFactory<L, TransactionBatch> get() {
        return new InfinispanSessionManagerFactory<>(this);
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        String containerName = this.configuration.getContainerName();
        String cacheName = this.configuration.getCacheName();
        String deploymentName = this.factoryConfiguration.getDeploymentName();
        this.configurationConfigurator = new TemplateConfigurationServiceConfigurator(InfinispanCacheRequirement.CONFIGURATION.getServiceName(support, containerName, deploymentName), containerName, deploymentName, cacheName, this).configure(support);
        this.cacheConfigurator = new CacheServiceConfigurator<>(InfinispanCacheRequirement.CACHE.getServiceName(support, containerName, deploymentName), containerName, deploymentName).configure(support);

        this.affinityFactory = new ServiceSupplierDependency<>(InfinispanRequirement.KEY_AFFINITY_FACTORY.getServiceName(support, containerName));
        this.dispatcherFactory = new ServiceSupplierDependency<>(ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(support, containerName));
        this.group = new ServiceSupplierDependency<>(ClusteringCacheRequirement.GROUP.getServiceName(support, containerName, this.factoryConfiguration.getServerName()));
        return this;
    }

    @Override
    public void accept(ConfigurationBuilder builder) {
        // Ensure expiration is not enabled on cache
        ExpirationConfiguration expiration = builder.expiration().create();
        if ((expiration.lifespan() >= 0) || (expiration.maxIdle() >= 0)) {
            builder.expiration().lifespan(-1).maxIdle(-1);
            InfinispanWebLogger.ROOT_LOGGER.expirationDisabled(InfinispanCacheRequirement.CONFIGURATION.resolve(this.configuration.getContainerName(), this.configuration.getCacheName()));
        }

        Integer size = this.factoryConfiguration.getMaxActiveSessions();
        EvictionStrategy strategy = (size != null) ? EvictionStrategy.REMOVE : EvictionStrategy.NONE;
        builder.memory().evictionType(EvictionType.COUNT)
                .evictionStrategy(strategy)
                .size((size != null) ? size.longValue() : 0)
                .storageType(StorageType.OBJECT)
                ;
        if (strategy.isEnabled()) {
            // Only evict creation meta-data entries
            // We will cascade eviction to the remaining entries for a given session
            builder.addModule(DataContainerConfigurationBuilder.class).evictable(SessionCreationMetaDataKey.class::isInstance);
        }
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

    @Override
    public String getContainerName() {
        return this.configuration.getContainerName();
    }

    @Override
    public String getCacheName() {
        return this.configuration.getCacheName();
    }

    @Override
    public SessionAttributePersistenceStrategy getAttributePersistenceStrategy() {
        return this.configuration.getAttributePersistenceStrategy();
    }

    @Override
    public Integer getMaxActiveSessions() {
        return this.factoryConfiguration.getMaxActiveSessions();
    }

    @Override
    public String getServerName() {
        return this.factoryConfiguration.getServerName();
    }

    @Override
    public String getDeploymentName() {
        return this.factoryConfiguration.getDeploymentName();
    }

    @Override
    public MarshalledValueFactory<C> getMarshalledValueFactory() {
        return this.factoryConfiguration.getMarshalledValueFactory();
    }

    @Override
    public C getMarshallingContext() {
        return this.factoryConfiguration.getMarshallingContext();
    }

    @Override
    public LocalContextFactory<L> getLocalContextFactory() {
        return this.factoryConfiguration.getLocalContextFactory();
    }

    @Override
    public Immutability getImmutability() {
        return this.factoryConfiguration.getImmutability();
    }
}
