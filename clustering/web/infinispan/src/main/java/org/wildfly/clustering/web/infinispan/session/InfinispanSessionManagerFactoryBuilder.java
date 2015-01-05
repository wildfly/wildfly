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
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.spi.service.CacheContainerServiceName;
import org.wildfly.clustering.infinispan.spi.service.CacheBuilder;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceName;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceNameFactory;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupServiceName;
import org.wildfly.clustering.spi.GroupServiceName;
import org.wildfly.clustering.web.session.SessionManagerConfiguration;
import org.wildfly.clustering.web.session.SessionManagerFactory;

public class InfinispanSessionManagerFactoryBuilder implements Builder<SessionManagerFactory<TransactionBatch>>, Value<SessionManagerFactory<TransactionBatch>>, InfinispanSessionManagerFactoryConfiguration {
    public static final String DEFAULT_CACHE_CONTAINER = "web";

    private static ServiceName getCacheServiceName(String cacheName) {
        ServiceName baseServiceName = CacheContainerServiceName.CACHE_CONTAINER.getServiceName(DEFAULT_CACHE_CONTAINER).getParent();
        ServiceName serviceName = ServiceName.parse((cacheName != null) ? cacheName : DEFAULT_CACHE_CONTAINER);
        if (!baseServiceName.isParentOf(serviceName)) {
            serviceName = baseServiceName.append(serviceName);
        }
        return (serviceName.length() < 4) ? serviceName.append(CacheServiceNameFactory.DEFAULT_CACHE) : serviceName;
    }

    private final SessionManagerConfiguration configuration;

    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> cache = new InjectedValue<>();
    private final InjectedValue<KeyAffinityServiceFactory> affinityFactory = new InjectedValue<>();
    private final InjectedValue<CommandDispatcherFactory> dispatcherFactory = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<NodeFactory> nodeFactory = new InjectedValue<>();

    public InfinispanSessionManagerFactoryBuilder(SessionManagerConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public ServiceName getServiceName() {
        return ServiceName.JBOSS.append("clustering", "web", this.configuration.getDeploymentName());
    }

    @Override
    public ServiceBuilder<SessionManagerFactory<TransactionBatch>> build(ServiceTarget target) {
        ServiceName templateCacheServiceName = getCacheServiceName(this.configuration.getCacheName());
        String templateCacheName = templateCacheServiceName.getSimpleName();
        String containerName = templateCacheServiceName.getParent().getSimpleName();
        String cacheName = this.configuration.getDeploymentName();

        new TemplateConfigurationBuilder(containerName, cacheName, templateCacheName).build(target).install();

        new CacheBuilder<>(containerName, cacheName).build(target)
                .addAliases(InfinispanRouteLocatorBuilder.getCacheServiceAlias(cacheName))
                .install();

        return target.addService(this.getServiceName(), new ValueService<>(this))
                .addDependency(CacheServiceName.CACHE.getServiceName(containerName, cacheName), Cache.class, this.cache)
                .addDependency(CacheContainerServiceName.AFFINITY.getServiceName(containerName), KeyAffinityServiceFactory.class, this.affinityFactory)
                .addDependency(GroupServiceName.COMMAND_DISPATCHER.getServiceName(containerName), CommandDispatcherFactory.class, this.dispatcherFactory)
                .addDependency(CacheGroupServiceName.NODE_FACTORY.getServiceName(containerName), NodeFactory.class, this.nodeFactory)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;
    }

    @Override
    public SessionManagerFactory<TransactionBatch> getValue() {
        return new InfinispanSessionManagerFactory(this);
    }

    @Override
    public SessionManagerConfiguration getSessionManagerConfiguration() {
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
