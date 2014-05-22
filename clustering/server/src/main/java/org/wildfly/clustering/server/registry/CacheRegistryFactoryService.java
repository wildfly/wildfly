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
import org.jboss.as.clustering.infinispan.invoker.BatchCacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.server.group.CacheNodeFactory;
import org.wildfly.clustering.spi.CacheServiceNames;

/**
 * Service that provides a clustered {@link RegistryFactory}.
 * @author Paul Ferraro
 */
@org.infinispan.notifications.Listener
public class CacheRegistryFactoryService<K, V> implements Service<RegistryFactory<K, V>>, CacheRegistryFactoryConfiguration<K, V> {

    public static <K, V> ServiceBuilder<RegistryFactory<K, V>> build(ServiceTarget target, ServiceName name, String containerName, String cacheName) {
        CacheRegistryFactoryService<K, V> service = new CacheRegistryFactoryService<>();
        return AsynchronousService.addService(target, name, service)
                .addDependency(CacheServiceNames.NODE_FACTORY.getServiceName(containerName, cacheName), CacheNodeFactory.class, service.factory)
                .addDependency(CacheServiceNames.GROUP.getServiceName(containerName, cacheName), Group.class, service.group)
                .addDependency(CacheService.getServiceName(containerName, cacheName), Cache.class, service.cache)
        ;
    }

    private final CacheInvoker invoker = new BatchCacheInvoker();
    private final InjectedValue<Group> group = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> cache = new InjectedValue<>();
    private final InjectedValue<CacheNodeFactory> factory = new InjectedValue<>();

    private volatile CacheRegistryFactory<K, V> value = null;

    private CacheRegistryFactoryService() {
        // Hide
    }

    @Override
    public RegistryFactory<K, V> getValue() {
        return this.value;
    }

    @Override
    public void start(StartContext context) {
        this.value = new CacheRegistryFactory<>(this);
    }

    @Override
    public void stop(StopContext context) {
        this.value = null;
    }

    @Override
    public CacheInvoker getCacheInvoker() {
        return this.invoker;
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
