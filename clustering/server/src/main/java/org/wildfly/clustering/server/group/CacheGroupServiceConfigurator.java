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
package org.wildfly.clustering.server.group;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.function.Consumers;
import org.jboss.as.clustering.function.Functions;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.Address;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.clustering.spi.NodeFactory;

/**
 * Builds a {@link Group} service for a cache.
 * @author Paul Ferraro
 */
public class CacheGroupServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, CacheGroupConfiguration, Supplier<CacheGroup> {

    private final String containerName;
    private final String cacheName;

    private volatile SupplierDependency<Cache<?, ?>> cache;
    private volatile SupplierDependency<NodeFactory<Address>> factory;

    public CacheGroupServiceConfigurator(ServiceName name, String containerName, String cacheName) {
        super(name);
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public CacheGroup get() {
        return new CacheGroup(this);
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.cache = new ServiceSupplierDependency<>(InfinispanCacheRequirement.CACHE.getServiceName(support, this.containerName, this.cacheName));
        this.factory = new ServiceSupplierDependency<>(ClusteringRequirement.GROUP.getServiceName(support, this.containerName));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<Group> group = new CompositeDependency(this.cache, this.factory).register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(group, Functions.identity(), this, Consumers.close());
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public Cache<?, ?> getCache() {
        return this.cache.get();
    }

    @Override
    public NodeFactory<Address> getMemberFactory() {
        return this.factory.get();
    }
}
