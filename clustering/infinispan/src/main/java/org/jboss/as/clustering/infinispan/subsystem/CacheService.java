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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.ArrayList;
import java.util.List;

import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Paul Ferraro
 */
public class CacheService<K, V> implements Service<Cache<K, V>> {
    private final InjectedValue<CacheContainer> container = new InjectedValue<CacheContainer>();
    private final String name;
    private final String template;
    private volatile Cache<K, V> cache;

    public static ServiceName getServiceName(String container, String cache) {
        return EmbeddedCacheManagerService.getServiceName(container).append((cache != null) ? cache : CacheContainer.DEFAULT_CACHE_NAME);
    }

    ServiceBuilder<Cache<K, V>> build(ServiceTarget target, ServiceName containerName) {
        return target.addService(containerName.append(this.name), this)
            .addDependency(containerName, CacheContainer.class, this.container)
        ;
    }

    public CacheService(String name) {
        this(name, null);
    }

    public CacheService(String name, String template) {
        this.name = name;
        this.template = null;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.value.Value#getValue()
     */
    @Override
    public Cache<K, V> getValue() throws IllegalStateException, IllegalArgumentException {
        return this.cache;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
     */
    @Override
    public void start(StartContext context) throws StartException {
        CacheContainer container = this.container.getValue();
        if (this.template != null) {
            ((EmbeddedCacheManager) container).defineConfiguration(this.name, this.template, new Configuration());
        }
        this.cache = container.getCache(this.name);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
     */
    @Override
    public void stop(StopContext context) {
        this.cache.stop();
    }
}
