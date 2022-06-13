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

import java.util.function.Consumer;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ejb.BeanManagerFactory;
import org.wildfly.clustering.ejb.BeanPassivationConfiguration;
import org.wildfly.clustering.ejb.StatefulBeanConfiguration;
import org.wildfly.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.service.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.service.InfinispanRequirement;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.group.Group;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.clustering.server.service.ClusteringRequirement;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public class InfinispanBeanManagerFactoryServiceConfigurator<I, T> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, InfinispanBeanManagerFactoryConfiguration {

    private final String name;
    private final String cacheName;
    private final StatefulBeanConfiguration beanConfiguration;
    private final InfinispanBeanManagementConfiguration configuration;

    private final SupplierDependency<ByteBufferMarshaller> marshaller;

    private volatile SupplierDependency<Cache<?, ?>> cache;
    private volatile SupplierDependency<KeyAffinityServiceFactory> affinityFactory;
    private volatile SupplierDependency<Group<Address>> group;
    private volatile SupplierDependency<CommandDispatcherFactory> dispatcherFactory;

    public InfinispanBeanManagerFactoryServiceConfigurator(String name, StatefulBeanConfiguration beanConfiguration, InfinispanBeanManagementConfiguration configuration) {
        super(beanConfiguration.getDeploymentUnitServiceName().append(beanConfiguration.getName()).append("bean-manager"));
        this.name = name;
        this.beanConfiguration = beanConfiguration;
        this.configuration = configuration;
        this.marshaller = beanConfiguration.getMarshallerDependency();
        this.cacheName = beanConfiguration.getBeanManagerName();
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        String containerName = this.configuration.getContainerName();
        this.cache = new ServiceSupplierDependency<>(InfinispanCacheRequirement.CACHE.getServiceName(support, containerName, this.cacheName));
        this.affinityFactory = new ServiceSupplierDependency<>(InfinispanRequirement.KEY_AFFINITY_FACTORY.getServiceName(support, containerName));
        this.dispatcherFactory = new ServiceSupplierDependency<>(ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(support, containerName));
        this.group = new ServiceSupplierDependency<>(ClusteringCacheRequirement.GROUP.getServiceName(support, containerName, this.cacheName));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        new CompositeDependency(this.cache, this.affinityFactory, this.marshaller, this.group, this.dispatcherFactory).register(builder);
        Consumer<BeanManagerFactory<I, T, TransactionBatch>> factory = builder.provides(this.getServiceName());
        Service service = Service.newInstance(factory, new InfinispanBeanManagerFactory<>(this));
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public StatefulBeanConfiguration getBeanConfiguration() {
        return this.beanConfiguration;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> Cache<K, V> getCache() {
        return (Cache<K, V>) this.cache.get();
    }

    @Override
    public KeyAffinityServiceFactory getKeyAffinityServiceFactory() {
        return this.affinityFactory.get();
    }

    @Override
    public ByteBufferMarshaller getMarshaller() {
        return this.marshaller.get();
    }

    @Override
    public BeanPassivationConfiguration getPassivationConfiguration() {
        return this.configuration;
    }

    @Override
    public Group<Address> getGroup() {
        return this.group.get();
    }

    @Override
    public CommandDispatcherFactory getCommandDispatcherFactory() {
        return this.dispatcherFactory.get();
    }
}
