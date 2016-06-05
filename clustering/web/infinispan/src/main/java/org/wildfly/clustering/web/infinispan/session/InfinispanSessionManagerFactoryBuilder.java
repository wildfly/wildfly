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

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.spi.service.CacheBuilder;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationBuilder;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.service.AliasServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupServiceName;
import org.wildfly.clustering.spi.GroupServiceName;
import org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.session.SessionManagerFactory;

public class InfinispanSessionManagerFactoryBuilder implements Builder<SessionManagerFactory<TransactionBatch>>, Value<SessionManagerFactory<TransactionBatch>>, InfinispanSessionManagerFactoryConfiguration {
    public static final String DEFAULT_CACHE_CONTAINER = "web";

    private final CapabilityServiceSupport support;
    private final SessionManagerFactoryConfiguration configuration;

    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> cache = new InjectedValue<>();
    private final InjectedValue<KeyAffinityServiceFactory> affinityFactory = new InjectedValue<>();
    private final InjectedValue<CommandDispatcherFactory> dispatcherFactory = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<NodeFactory> nodeFactory = new InjectedValue<>();

    public InfinispanSessionManagerFactoryBuilder(CapabilityServiceSupport support, SessionManagerFactoryConfiguration configuration) {
        this.support = support;
        this.configuration = configuration;
    }

    @Override
    public ServiceName getServiceName() {
        return ServiceName.JBOSS.append("clustering", "web", this.configuration.getDeploymentName());
    }

    @Override
    public ServiceBuilder<SessionManagerFactory<TransactionBatch>> build(ServiceTarget target) {
        ServiceName baseServiceName = ServiceName.JBOSS.append("infinispan");
        String configCacheName = this.configuration.getCacheName();
        ServiceName configServiceName = ServiceName.parse((configCacheName != null) ? configCacheName : DEFAULT_CACHE_CONTAINER);
        if (!baseServiceName.isParentOf(configServiceName)) {
            configServiceName = baseServiceName.append(configServiceName);
        }
        String containerName = ((configServiceName.length() > 3) ? configServiceName.getParent() : configServiceName).getSimpleName();
        String templateCacheName =  (configServiceName.length() > 3) ? configServiceName.getSimpleName() : null;
        String cacheName = this.configuration.getDeploymentName();

        new TemplateConfigurationBuilder(InfinispanCacheRequirement.CONFIGURATION.getServiceName(this.support, containerName, cacheName), containerName, cacheName, templateCacheName).configure(this.support).build(target).install();

        new CacheBuilder<>(InfinispanCacheRequirement.CACHE.getServiceName(this.support, containerName, cacheName), containerName, cacheName).configure(this.support).build(target)
                .addAliases(InfinispanRouteLocatorBuilder.getCacheServiceAlias(cacheName))
                .install();

        new AliasServiceBuilder<>(InfinispanRouteLocatorBuilder.getNodeFactoryServiceAlias(cacheName), CacheGroupServiceName.NODE_FACTORY.getServiceName(containerName, RouteCacheGroupBuilderProvider.CACHE_NAME), NodeFactory.class).build(target).install();
        new AliasServiceBuilder<>(InfinispanRouteLocatorBuilder.getRegistryServiceAlias(cacheName), CacheGroupServiceName.REGISTRY.getServiceName(containerName, RouteCacheGroupBuilderProvider.CACHE_NAME), Registry.class).build(target).install();

        return target.addService(this.getServiceName(), new ValueService<>(this))
                .addDependency(InfinispanCacheRequirement.CACHE.getServiceName(this.support, containerName, cacheName), Cache.class, this.cache)
                .addDependency(InfinispanRequirement.KEY_AFFINITY_FACTORY.getServiceName(this.support, containerName), KeyAffinityServiceFactory.class, this.affinityFactory)
                .addDependency(GroupServiceName.COMMAND_DISPATCHER.getServiceName(containerName), CommandDispatcherFactory.class, this.dispatcherFactory)
                .addDependency(InfinispanRouteLocatorBuilder.getNodeFactoryServiceAlias(cacheName), NodeFactory.class, this.nodeFactory)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;
    }

    @Override
    public SessionManagerFactory<TransactionBatch> getValue() {
        return new InfinispanSessionManagerFactory(this);
    }

    @Override
    public SessionManagerFactoryConfiguration getSessionManagerFactoryConfiguration() {
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
    public NodeFactory<Address> getNodeFactory() {
        return this.nodeFactory.getValue();
    }
}
