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
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.configuration.cache.MemoryConfiguration;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.clustering.function.Consumers;
import org.jboss.as.clustering.function.Functions;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.spi.service.CacheBuilder;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationBuilder;
import org.wildfly.clustering.marshalling.spi.Marshallability;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.SuppliedValueService;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.clustering.spi.NodeFactory;
import org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.session.SessionManagerFactory;

public class InfinispanSessionManagerFactoryBuilder<C extends Marshallability, L> implements CapabilityServiceBuilder<SessionManagerFactory<L, TransactionBatch>>, InfinispanSessionManagerFactoryConfiguration<C, L>, Supplier<SessionManagerFactory<L, TransactionBatch>> {
    public static final String DEFAULT_CACHE_CONTAINER = "web";

    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> cache = new InjectedValue<>();

    private final SessionManagerFactoryConfiguration<C, L> configuration;
    private final String containerName;
    private final CapabilityServiceBuilder<?> configurationBuilder;
    private final CapabilityServiceBuilder<?> cacheBuilder;

    @SuppressWarnings("rawtypes")
    private volatile ValueDependency<NodeFactory> group;
    private volatile ValueDependency<KeyAffinityServiceFactory> affinityFactory;
    private volatile ValueDependency<CommandDispatcherFactory> dispatcherFactory;

    public InfinispanSessionManagerFactoryBuilder(SessionManagerFactoryConfiguration<C, L> configuration) {
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
        Consumer<ConfigurationBuilder> configurator = builder -> {
            // Ensure expiration is not enabled on cache
            ExpirationConfiguration expiration = builder.expiration().create();
            if ((expiration.lifespan() >= 0) || (expiration.maxIdle() >= 0)) {
                builder.expiration().lifespan(-1).maxIdle(-1);
                InfinispanWebLogger.ROOT_LOGGER.expirationDisabled(InfinispanCacheRequirement.CONFIGURATION.resolve(this.containerName, templateCacheName));
            }
            // Ensure eviction is not enabled on cache
            MemoryConfiguration memory = builder.memory().create();
            if (memory.size() >= 0) {
                builder.memory().size(-1);
                InfinispanWebLogger.ROOT_LOGGER.evictionDisabled(InfinispanCacheRequirement.CONFIGURATION.resolve(this.containerName, templateCacheName));
            }
        };

        this.configurationBuilder = new TemplateConfigurationBuilder(ServiceName.parse(InfinispanCacheRequirement.CONFIGURATION.resolve(this.containerName, cacheName)), this.containerName, cacheName, templateCacheName, configurator);
        this.cacheBuilder = new CacheBuilder<>(ServiceName.parse(InfinispanCacheRequirement.CACHE.resolve(this.containerName, cacheName)), this.containerName, cacheName);
    }

    @Override
    public SessionManagerFactory<L, TransactionBatch> get() {
        return new InfinispanSessionManagerFactory<>(this);
    }

    @Override
    public ServiceName getServiceName() {
        return ServiceName.JBOSS.append("clustering", "web", this.configuration.getDeploymentName());
    }

    @Override
    public Builder<SessionManagerFactory<L, TransactionBatch>> configure(CapabilityServiceSupport support) {
        this.configurationBuilder.configure(support);
        this.cacheBuilder.configure(support);

        this.affinityFactory = new InjectedValueDependency<>(InfinispanRequirement.KEY_AFFINITY_FACTORY.getServiceName(support, this.containerName), KeyAffinityServiceFactory.class);
        this.dispatcherFactory = new InjectedValueDependency<>(ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(support, this.containerName), CommandDispatcherFactory.class);
        this.group = new InjectedValueDependency<>(ClusteringCacheRequirement.GROUP.getServiceName(support, this.containerName, this.configuration.getServerName()), NodeFactory.class);
        return this;
    }

    @Override
    public ServiceBuilder<SessionManagerFactory<L, TransactionBatch>> build(ServiceTarget target) {
        this.configurationBuilder.build(target).install();
        this.cacheBuilder.build(target).install();

        ServiceBuilder<SessionManagerFactory<L, TransactionBatch>> builder = target.addService(this.getServiceName(), new SuppliedValueService<>(Functions.identity(), this, Consumers.close()))
                .addDependency(this.cacheBuilder.getServiceName(), Cache.class, this.cache)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                ;
        return new CompositeDependency(this.group, this.affinityFactory, this.dispatcherFactory).register(builder);
    }

    @Override
    public SessionManagerFactoryConfiguration<C, L> getSessionManagerFactoryConfiguration() {
        return this.configuration;
    }

    @Override
    public <K, V> Cache<K, V> getCache() {
        return this.cache.getValue();
    }

    @Override
    public KeyAffinityServiceFactory getKeyAffinityServiceFactory() {
        return this.affinityFactory.getValue();
    }

    @Override
    public CommandDispatcherFactory getCommandDispatcherFactory() {
        return this.dispatcherFactory.getValue();
    }

    @Override
    public NodeFactory<Address> getMemberFactory() {
        return this.group.getValue();
    }
}
