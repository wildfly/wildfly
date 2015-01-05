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

import org.infinispan.Cache;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceName;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupServiceName;

/**
 * Builds a {@link Group} service for a cache.
 * @author Paul Ferraro
 */
public class CacheGroupBuilder extends CacheGroupServiceNameProvider implements Builder<Group>, Service<Group>, CacheGroupConfiguration {

    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> cache = new InjectedValue<>();
    private final InjectedValue<InfinispanNodeFactory> factory = new InjectedValue<>();

    private volatile CacheGroup group;

    public CacheGroupBuilder(String containerName, String cacheName) {
        super(containerName, cacheName);
    }

    @Override
    public ServiceBuilder<Group> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), this)
                .addDependency(CacheServiceName.CACHE.getServiceName(this.containerName, this.cacheName), Cache.class, this.cache)
                .addDependency(CacheGroupServiceName.NODE_FACTORY.getServiceName(this.containerName, this.cacheName), InfinispanNodeFactory.class, this.factory)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;
    }

    @Override
    public Cache<?, ?> getCache() {
        return this.cache.getValue();
    }

    @Override
    public Group getValue() {
        return this.group;
    }

    @Override
    public void start(StartContext context) {
        this.group = new CacheGroup(this);
    }

    @Override
    public void stop(StopContext context) {
        this.group.close();
        this.group = null;
    }

    @Override
    public InfinispanNodeFactory getNodeFactory() {
        return this.factory.getValue();
    }
}
