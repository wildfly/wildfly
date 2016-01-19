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
package org.wildfly.clustering.server.registry;

import java.util.Map.Entry;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.infinispan.InfinispanBatcher;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceName;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.server.group.InfinispanNodeFactory;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupServiceName;

/**
 * Builds a clustered {@link RegistryFactory}.
 * @author Paul Ferraro
 */
public class CacheRegistryFactoryBuilder<K, V> extends RegistryFactoryServiceNameProvider implements Builder<RegistryFactory<K, V>>, Value<RegistryFactory<K, V>>, CacheRegistryFactoryConfiguration<K, V> {

    private final InjectedValue<Group> group = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> cache = new InjectedValue<>();
    private final InjectedValue<InfinispanNodeFactory> factory = new InjectedValue<>();

    public CacheRegistryFactoryBuilder(String containerName, String cacheName) {
        super(containerName, cacheName);
    }

    @Override
    public ServiceBuilder<RegistryFactory<K, V>> build(ServiceTarget target) {
        return new AsynchronousServiceBuilder<>(this.getServiceName(), new ValueService<>(this)).build(target)
                .addDependency(CacheGroupServiceName.NODE_FACTORY.getServiceName(this.containerName, this.cacheName), InfinispanNodeFactory.class, this.factory)
                .addDependency(CacheGroupServiceName.GROUP.getServiceName(this.containerName, this.cacheName), Group.class, this.group)
                .addDependency(CacheServiceName.CACHE.getServiceName(this.containerName, this.cacheName), Cache.class, this.cache)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public RegistryFactory<K, V> getValue() {
        return new CacheRegistryFactory<>(this);
    }

    @Override
    public Batcher<? extends Batch> getBatcher() {
        return new InfinispanBatcher(this.getCache());
    }

    @Override
    public Group getGroup() {
        return this.group.getValue();
    }

    @Override
    public Cache<Node, Entry<K, V>> getCache() {
        return this.cache.getValue();
    }

    @Override
    public NodeFactory<Address> getNodeFactory() {
        return this.factory.getValue();
    }
}
