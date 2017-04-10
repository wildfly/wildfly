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

import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
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
import org.wildfly.clustering.marshalling.spi.Marshallability;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.session.SessionManagerFactory;

public class InfinispanSessionManagerFactoryBuilder<C extends Marshallability> implements CapabilityServiceBuilder<SessionManagerFactory<TransactionBatch>>, InfinispanSessionManagerFactoryConfiguration<C> {
    public static final String DEFAULT_CACHE_CONTAINER = "web";

    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> cache = new InjectedValue<>();

    private final SessionManagerFactoryConfiguration<C> configuration;
    private final String containerName;
    private final CapabilityServiceBuilder<?> configurationBuilder;
    private final CapabilityServiceBuilder<?> cacheBuilder;

    @SuppressWarnings("rawtypes")
    private volatile ValueDependency<NodeFactory> nodeFactory;
    private volatile ValueDependency<KeyAffinityServiceFactory> affinityFactory;
    private volatile ValueDependency<CommandDispatcherFactory> dispatcherFactory;

    public InfinispanSessionManagerFactoryBuilder(SessionManagerFactoryConfiguration<C> configuration) {
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

        this.configurationBuilder = new TemplateConfigurationBuilder(ServiceName.parse(InfinispanCacheRequirement.CONFIGURATION.resolve(this.containerName, cacheName)), this.containerName, cacheName, templateCacheName);
        this.cacheBuilder = new CacheBuilder<>(ServiceName.parse(InfinispanCacheRequirement.CACHE.resolve(this.containerName, cacheName)), this.containerName, cacheName);
    }

    @Override
    public ServiceName getServiceName() {
        return ServiceName.JBOSS.append("clustering", "web", this.configuration.getDeploymentName());
    }

    @Override
    public Builder<SessionManagerFactory<TransactionBatch>> configure(CapabilityServiceSupport support) {
        this.configurationBuilder.configure(support);
        this.cacheBuilder.configure(support);

        this.affinityFactory = new InjectedValueDependency<>(InfinispanRequirement.KEY_AFFINITY_FACTORY.getServiceName(support, this.containerName), KeyAffinityServiceFactory.class);
        this.dispatcherFactory = new InjectedValueDependency<>(ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(support, this.containerName), CommandDispatcherFactory.class);
        this.nodeFactory = new InjectedValueDependency<>(ClusteringCacheRequirement.NODE_FACTORY.getServiceName(support, this.containerName, this.configuration.getServerName()), NodeFactory.class);
        return this;
    }

    @Override
    public ServiceBuilder<SessionManagerFactory<TransactionBatch>> build(ServiceTarget target) {
        this.configurationBuilder.build(target).install();
        this.cacheBuilder.build(target).install();

        Value<SessionManagerFactory<TransactionBatch>> value = () -> new InfinispanSessionManagerFactory<>(this);
        ServiceBuilder<SessionManagerFactory<TransactionBatch>> builder = target.addService(this.getServiceName(), new ValueService<>(value))
                .addDependency(this.cacheBuilder.getServiceName(), Cache.class, this.cache)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                ;
        Stream.of(this.nodeFactory, this.affinityFactory, this.dispatcherFactory).forEach(dependency -> dependency.register(builder));
        return builder;
    }

    @Override
    public SessionManagerFactoryConfiguration<C> getSessionManagerFactoryConfiguration() {
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
