/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ejb.hotrod.bean;

import java.util.function.Consumer;

import org.infinispan.client.hotrod.RemoteCache;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ejb.bean.BeanConfiguration;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanManagerFactory;
import org.wildfly.clustering.ejb.bean.BeanPassivationConfiguration;
import org.wildfly.clustering.ejb.cache.bean.BeanGroupManager;
import org.wildfly.clustering.ejb.cache.bean.BeanGroupManagerServiceNameProvider;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.infinispan.client.service.InfinispanClientRequirement;
import org.wildfly.clustering.server.service.ClusteringDefaultRequirement;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Configures a service that provides a {@link HotRodBeanManagerFactory} for a component of a bean deployment.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class HotRodBeanManagerFactoryServiceConfigurator<K, V extends BeanInstance<K>> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, HotRodBeanManagerFactoryConfiguration<K, V> {

    private final BeanConfiguration beanConfiguration;
    private final HotRodBeanManagementConfiguration configuration;

    private final SupplierDependency<BeanGroupManager<K, V>> groupManager;

    @SuppressWarnings("rawtypes")
    private volatile SupplierDependency<RemoteCache> cache;
    private volatile SupplierDependency<Group> group;

    public HotRodBeanManagerFactoryServiceConfigurator(BeanConfiguration beanConfiguration, HotRodBeanManagementConfiguration configuration) {
        super(beanConfiguration.getDeploymentServiceName().append(beanConfiguration.getName()).append("bean-manager"));
        this.beanConfiguration = beanConfiguration;
        this.configuration = configuration;
        this.groupManager = new ServiceSupplierDependency<>(new BeanGroupManagerServiceNameProvider(beanConfiguration));
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        String containerName = this.configuration.getContainerName();
        String cacheName = this.beanConfiguration.getDeploymentName();
        this.cache = new ServiceSupplierDependency<>(ServiceNameFactory.parseServiceName(InfinispanClientRequirement.REMOTE_CONTAINER.getName()).append(containerName, cacheName));
        this.group = new ServiceSupplierDependency<>(ClusteringDefaultRequirement.GROUP.getServiceName(support));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        new CompositeDependency(this.cache, this.group, this.groupManager).register(builder);
        Consumer<BeanManagerFactory<K, V, TransactionBatch>> factory = builder.provides(this.getServiceName());
        Service service = Service.newInstance(factory, new HotRodBeanManagerFactory<>(this));
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public BeanConfiguration getBeanConfiguration() {
        return this.beanConfiguration;
    }

    @Override
    public BeanPassivationConfiguration getPassivationConfiguration() {
        return this.configuration;
    }

    @Override
    public Group getGroup() {
        return this.group.get();
    }

    @Override
    public <KK, VV> RemoteCache<KK, VV> getCache() {
        return this.cache.get();
    }

    @Override
    public BeanGroupManager<K, V> getBeanGroupManager() {
        return this.groupManager.get();
    }
}
