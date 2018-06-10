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

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.ejb.BeanContext;
import org.wildfly.clustering.ejb.BeanManagerFactory;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorConfiguration;
import org.wildfly.clustering.ejb.BeanPassivationConfiguration;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationRepository;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.clustering.spi.NodeFactory;

/**
 * @author Paul Ferraro
 */
public class InfinispanBeanManagerFactoryServiceConfigurator<I, T> extends SimpleServiceNameProvider implements ServiceConfigurator, InfinispanBeanManagerFactoryConfiguration {

    private final BeanContext context;
    private final BeanManagerFactoryServiceConfiguratorConfiguration configuration;

    @SuppressWarnings("rawtypes")
    private final SupplierDependency<Cache> cache;
    private final SupplierDependency<KeyAffinityServiceFactory> affinityFactory;
    private final SupplierDependency<MarshallingConfigurationRepository> repository;
    private final SupplierDependency<ScheduledExecutorService> scheduler;
    private final SupplierDependency<NodeFactory<Address>> group;
    private final SupplierDependency<Registry<String, ?>> registry;
    private final SupplierDependency<CommandDispatcherFactory> dispatcherFactory;

    public InfinispanBeanManagerFactoryServiceConfigurator(CapabilityServiceSupport support, String name, BeanContext context, BeanManagerFactoryServiceConfiguratorConfiguration configuration) {
        super(context.getDeploymentUnitServiceName().append(context.getBeanName()).append("bean-manager"));
        this.context = context;
        this.configuration = configuration;
        ServiceName deploymentUnitServiceName = context.getDeploymentUnitServiceName();
        String containerName = configuration.getContainerName();
        this.cache = new ServiceSupplierDependency<>(InfinispanCacheRequirement.CACHE.getServiceName(support, containerName, InfinispanBeanManagerFactoryServiceConfiguratorFactory.getCacheName(deploymentUnitServiceName)));
        this.affinityFactory = new ServiceSupplierDependency<>(InfinispanRequirement.KEY_AFFINITY_FACTORY.getServiceName(support, containerName));
        this.repository = new ServiceSupplierDependency<>(deploymentUnitServiceName.append("marshalling"));
        this.scheduler = new ServiceSupplierDependency<>(deploymentUnitServiceName.append(name, "expiration"));
        this.dispatcherFactory = new ServiceSupplierDependency<>(ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(support, containerName));
        this.registry = new ServiceSupplierDependency<>(ClusteringCacheRequirement.REGISTRY.getServiceName(support, containerName, BeanManagerFactoryServiceConfiguratorConfiguration.CLIENT_MAPPINGS_CACHE_NAME));
        this.group = new ServiceSupplierDependency<>(ClusteringCacheRequirement.GROUP.getServiceName(support, containerName, BeanManagerFactoryServiceConfiguratorConfiguration.CLIENT_MAPPINGS_CACHE_NAME));
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        new CompositeDependency(this.cache, this.affinityFactory, this.repository, this.scheduler, this.group, this.registry, this.dispatcherFactory).register(builder);
        Consumer<BeanManagerFactory<I, T, TransactionBatch>> factory = builder.provides(this.getServiceName());
        Service service = Service.newInstance(factory, new InfinispanBeanManagerFactory<>(this));
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public BeanContext getBeanContext() {
        return this.context;
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
    public MarshallingConfigurationRepository getMarshallingConfigurationRepository() {
        return this.repository.get();
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return this.scheduler.get();
    }

    @Override
    public BeanPassivationConfiguration getPassivationConfiguration() {
        return this.configuration;
    }

    @Override
    public NodeFactory<Address> getNodeFactory() {
        return this.group.get();
    }

    @Override
    public Registry<String, ?> getRegistry() {
        return this.registry.get();
    }

    @Override
    public CommandDispatcherFactory getCommandDispatcherFactory() {
        return this.dispatcherFactory.get();
    }
}
