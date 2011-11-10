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

package org.jboss.as.ejb3.cache.impl.factory;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.cache.CacheFactoryService;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStoreSource;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStoreSourceService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Paul Ferraro
 */
public class GroupAwareCacheFactoryService<K extends Serializable, V extends Cacheable<K>> extends CacheFactoryService<K, V> {

    @SuppressWarnings("rawtypes")
    private final InjectedValue<BackingCacheEntryStoreSource> storeSource = new InjectedValue<BackingCacheEntryStoreSource>();

    public GroupAwareCacheFactoryService(String name, Set<String> aliases) {
        super(name, aliases);
    }

    public ServiceBuilder<CacheFactory<K, V>> build(ServiceTarget target, String backingCache) {
        return super.build(target).addDependency(BackingCacheEntryStoreSourceService.getServiceName(backingCache), BackingCacheEntryStoreSource.class, this.storeSource);
    }

    @Override
    protected CacheFactory<K, V> createCacheFactory() {
        @SuppressWarnings("unchecked")
        BackingCacheEntryStoreSource<K, V, UUID> source = this.storeSource.getValue();
        return new GroupAwareCacheFactory<K, V>(source);
    }
}