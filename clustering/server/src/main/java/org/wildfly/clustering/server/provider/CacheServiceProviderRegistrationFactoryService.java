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
package org.wildfly.clustering.server.provider;

import java.util.Set;

import org.infinispan.Cache;
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
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.provider.ServiceProviderRegistrationFactory;
import org.wildfly.clustering.spi.CacheServiceNames;
import org.wildfly.clustering.spi.ChannelServiceNames;

/**
 * Service provider registration factory implementation.
 * @author Paul Ferraro
 */
public class CacheServiceProviderRegistrationFactoryService implements Service<ServiceProviderRegistrationFactory>, CacheServiceProviderRegistrationFactoryConfiguration {

    public static ServiceBuilder<ServiceProviderRegistrationFactory> build(ServiceTarget target, ServiceName name, String containerName, String cacheName) {
        CacheServiceProviderRegistrationFactoryService service = new CacheServiceProviderRegistrationFactoryService(name);
        return AsynchronousService.addService(target, name, service)
                .addDependency(CacheService.getServiceName(containerName, cacheName), Cache.class, service.cache)
                .addDependency(CacheServiceNames.GROUP.getServiceName(containerName, cacheName), Group.class, service.group)
                .addDependency(ChannelServiceNames.COMMAND_DISPATCHER.getServiceName(containerName), CommandDispatcherFactory.class, service.dispatcherFactory)
        ;
    }

    private final Object id;
    private final CacheInvoker invoker = new BatchCacheInvoker();

    private final InjectedValue<CommandDispatcherFactory> dispatcherFactory = new InjectedValue<>();
    private final InjectedValue<Group> group = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> cache = new InjectedValue<>();

    private volatile CacheServiceProviderRegistrationFactory factory = null;

    private CacheServiceProviderRegistrationFactoryService(Object id) {
        this.id = id;
    }

    @Override
    public Object getId() {
        return this.id;
    }

    @Override
    public Group getGroup() {
        return this.group.getValue();
    }

    @Override
    public Cache<Object, Set<Node>> getCache() {
        return this.cache.getValue();
    }

    @Override
    public CommandDispatcherFactory getCommandDispatcherFactory() {
        return this.dispatcherFactory.getValue();
    }

    @Override
    public CacheInvoker getCacheInvoker() {
        return this.invoker;
    }

    @Override
    public ServiceProviderRegistrationFactory getValue() {
        return this.factory;
    }

    @Override
    public void start(StartContext context) {
        this.factory = new CacheServiceProviderRegistrationFactory(this);
    }

    @Override
    public void stop(StopContext context) {
        this.factory.close();
        this.factory = null;
    }
}
