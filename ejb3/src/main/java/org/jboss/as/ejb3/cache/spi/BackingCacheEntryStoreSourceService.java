/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.cache.spi;

import java.io.Serializable;

import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.CacheFactoryService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * @author Paul Ferraro
 */
public abstract class BackingCacheEntryStoreSourceService<K extends Serializable, V extends Cacheable<K>, G extends Serializable, S extends BackingCacheEntryStoreSource<K, V, G>> implements Service<S> {

    private static final ServiceName BASE_CACHE_STORE_SERVICE_NAME = CacheFactoryService.BASE_CACHE_SERVICE_NAME.append("store");

    public static ServiceName getServiceName(String name) {
        return BASE_CACHE_STORE_SERVICE_NAME.append(name);
    }

    private final String name;
    private final S source;

    public BackingCacheEntryStoreSourceService(String name, S source) {
        this.name = name;
        this.source = source;
    }

    public ServiceBuilder<S> build(ServiceTarget target) {
        ServiceBuilder<S> builder = target.addService(getServiceName(this.name), this);
        this.source.addDependencies(target, builder);
        return builder;
    }

    @Override
    public S getValue() {
        return this.source;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
     */
    @Override
    public void start(StartContext context) {
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
     */
    @Override
    public void stop(StopContext context) {
    }
}
