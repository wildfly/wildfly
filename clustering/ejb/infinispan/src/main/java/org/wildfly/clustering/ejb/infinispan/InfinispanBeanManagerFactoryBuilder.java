/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ejb.infinispan;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

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
import org.wildfly.clustering.ejb.BeanContext;
import org.wildfly.clustering.ejb.BeanManagerFactory;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilderConfiguration;
import org.wildfly.clustering.ejb.BeanPassivationConfiguration;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.spi.service.CacheContainerServiceName;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceName;
import org.wildfly.clustering.marshalling.VersionedMarshallingConfiguration;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupServiceName;
import org.wildfly.clustering.spi.GroupServiceName;

/**
 * @author Paul Ferraro
 */
public class InfinispanBeanManagerFactoryBuilder<G, I, T> implements Builder<BeanManagerFactory<G, I, T, TransactionBatch>>, Value<BeanManagerFactory<G, I, T, TransactionBatch>>, InfinispanBeanManagerFactoryConfiguration {

    private final String name;
    private final BeanContext context;
    private final BeanManagerFactoryBuilderConfiguration configuration;

    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> cache = new InjectedValue<>();
    private final InjectedValue<KeyAffinityServiceFactory> affinityFactory = new InjectedValue<>();
    private final InjectedValue<VersionedMarshallingConfiguration> marshallingConfiguration = new InjectedValue<>();
    private final InjectedValue<ScheduledExecutorService> scheduler = new InjectedValue<>();
    private final InjectedValue<Executor> executor = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<NodeFactory> nodeFactory = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Registry> registry = new InjectedValue<>();
    private final InjectedValue<CommandDispatcherFactory> dispatcherFactory = new InjectedValue<>();

    public InfinispanBeanManagerFactoryBuilder(String name, BeanContext context, BeanManagerFactoryBuilderConfiguration configuration) {
        this.name = name;
        this.context = context;
        this.configuration = configuration;
    }

    @Override
    public ServiceName getServiceName() {
        return this.context.getDeploymentUnitServiceName().append(this.context.getBeanName()).append("bean-manager");
    }

    @Override
    public ServiceBuilder<BeanManagerFactory<G, I, T, TransactionBatch>> build(ServiceTarget target) {
        String containerName = this.configuration.getContainerName();
        ServiceName deploymentUnitServiceName = this.context.getDeploymentUnitServiceName();
        return target.addService(this.getServiceName(), new ValueService<>(this))
                .addDependency(CacheServiceName.CACHE.getServiceName(containerName, InfinispanBeanManagerFactoryBuilderFactory.getCacheName(deploymentUnitServiceName)), Cache.class, this.cache)
                .addDependency(CacheContainerServiceName.AFFINITY.getServiceName(containerName), KeyAffinityServiceFactory.class, this.affinityFactory)
                .addDependency(deploymentUnitServiceName.append("marshalling"), VersionedMarshallingConfiguration.class, this.marshallingConfiguration)
                .addDependency(deploymentUnitServiceName.append(this.name, "expiration"), ScheduledExecutorService.class, this.scheduler)
                .addDependency(deploymentUnitServiceName.append(this.name, "eviction"), Executor.class, this.executor)
                .addDependency(GroupServiceName.COMMAND_DISPATCHER.getServiceName(containerName), CommandDispatcherFactory.class, this.dispatcherFactory)
                .addDependency(CacheGroupServiceName.REGISTRY.getServiceName(containerName), Registry.class, this.registry)
                .addDependency(CacheGroupServiceName.NODE_FACTORY.getServiceName(containerName), NodeFactory.class, this.nodeFactory)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;
    }

    @Override
    public BeanManagerFactory<G, I, T, TransactionBatch> getValue() {
        return new InfinispanBeanManagerFactory<>(this);
    }

    @Override
    public BeanContext getBeanContext() {
        return this.context;
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
    public VersionedMarshallingConfiguration getMarshallingConfiguration() {
        return this.marshallingConfiguration.getValue();
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return this.scheduler.getValue();
    }

    @Override
    public Executor getExecutor() {
        return this.executor.getValue();
    }

    @Override
    public BeanPassivationConfiguration getPassivationConfiguration() {
        return this.configuration;
    }

    @Override
    public NodeFactory<Address> getNodeFactory() {
        return this.nodeFactory.getValue();
    }

    @Override
    public Registry<String, ?> getRegistry() {
        return this.registry.getValue();
    }

    @Override
    public CommandDispatcherFactory getCommandDispatcherFactory() {
        return this.dispatcherFactory.getValue();
    }
}
