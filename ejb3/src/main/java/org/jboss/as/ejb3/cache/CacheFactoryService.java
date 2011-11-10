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

package org.jboss.as.ejb3.cache;

import java.io.Serializable;
import java.util.Set;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * @author Paul Ferraro
 */
public abstract class CacheFactoryService<K extends Serializable, V extends Identifiable<K>> implements Service<CacheFactory<K, V>> {

    public static final ServiceName BASE_CACHE_SERVICE_NAME = ServiceName.JBOSS.append("ejb", "cache");
    public static final ServiceName DEFAULT_SFSB_CACHE_SERVICE_NAME = BASE_CACHE_SERVICE_NAME.append("sfsb-default");
    public static final ServiceName DEFAULT_CLUSTERED_SFSB_CACHE_SERVICE_NAME = BASE_CACHE_SERVICE_NAME.append("clustered-sfsb-default");

    private static final ServiceName BASE_CACHE_FACTORY_SERVICE_NAME = BASE_CACHE_SERVICE_NAME.append("factory");

    public static ServiceName getServiceName(String name) {
        return BASE_CACHE_FACTORY_SERVICE_NAME.append(name);
    }

    private final String name;
    private final Set<String> aliases;

    private volatile CacheFactory<K, V> factory;

    public CacheFactoryService(String name, Set<String> aliases) {
        this.name = name;
        this.aliases = aliases;
    }

    public ServiceBuilder<CacheFactory<K, V>> build(ServiceTarget target) {
        ServiceBuilder<CacheFactory<K, V>> builder = target.addService(getServiceName(this.name), this);
        for (String alias: this.aliases) {
            builder.addAliases(getServiceName(alias));
        }
        return builder;
    }

    @Override
    public CacheFactory<K, V> getValue() throws IllegalStateException, IllegalArgumentException {
        return this.factory;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
     */
    @Override
    public void start(StartContext context) {
        this.factory = this.createCacheFactory();
    }

    protected abstract CacheFactory<K, V> createCacheFactory();

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
     */
    @Override
    public void stop(StopContext context) {
        this.factory = null;
    }
}
