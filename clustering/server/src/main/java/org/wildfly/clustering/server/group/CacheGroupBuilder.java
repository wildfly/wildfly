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

import java.util.function.Supplier;

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.clustering.function.Consumers;
import org.jboss.as.clustering.function.Functions;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.Address;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.SuppliedValueService;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.clustering.spi.NodeFactory;

/**
 * Builds a {@link Group} service for a cache.
 * @author Paul Ferraro
 */
public class CacheGroupBuilder implements CapabilityServiceBuilder<Group>, CacheGroupConfiguration, Supplier<CacheGroup> {

    private final ServiceName name;
    private final String containerName;
    private final String cacheName;

    private volatile ValueDependency<Cache<?, ?>> cache;
    private volatile ValueDependency<NodeFactory<Address>> factory;

    public CacheGroupBuilder(ServiceName name, String containerName, String cacheName) {
        this.name = name;
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public CacheGroup get() {
        return new CacheGroup(this);
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder<Group> configure(CapabilityServiceSupport support) {
        this.cache = new InjectedValueDependency<>(InfinispanCacheRequirement.CACHE.getServiceName(support, this.containerName, this.cacheName), (Class<Cache<?, ?>>) (Class<?>) Cache.class);
        this.factory = new InjectedValueDependency<>(ClusteringRequirement.GROUP.getServiceName(support, this.containerName), (Class<NodeFactory<Address>>) (Class<?>) NodeFactory.class);
        return this;
    }

    @Override
    public ServiceBuilder<Group> build(ServiceTarget target) {
        Service<Group> service = new SuppliedValueService<>(Functions.identity(), this, Consumers.close());
        ServiceBuilder<Group> builder = target.addService(this.name, service).setInitialMode(ServiceController.Mode.ON_DEMAND);
        return new CompositeDependency(this.cache, this.factory).register(builder);
    }

    @Override
    public Cache<?, ?> getCache() {
        return this.cache.getValue();
    }

    @Override
    public NodeFactory<Address> getMemberFactory() {
        return this.factory.getValue();
    }
}
