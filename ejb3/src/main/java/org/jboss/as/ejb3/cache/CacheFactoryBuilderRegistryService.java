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
package org.jboss.as.ejb3.cache;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CacheFactoryBuilderRegistryService<K, V extends Identifiable<K>> implements Service<CacheFactoryBuilderRegistry<K, V>>, CacheFactoryBuilderRegistry<K, V> {
    public static final ServiceName SERVICE_NAME = CacheFactoryBuilderService.BASE_CACHE_SERVICE_NAME.append("registry");

    private final Map<String, CacheFactoryBuilder<K, V>> builders = new ConcurrentHashMap<>();

    @Override
    public CacheFactoryBuilderRegistry<K, V> getValue() {
        return this;
    }

    @Override
    public void start(StartContext context) {
        // Do nothing
    }

    @Override
    public void stop(StopContext context) {
        this.builders.clear();
    }

    @Override
    public Set<String> getBuilderNames() {
        return this.builders.keySet();
    }

    @Override
    public Collection<CacheFactoryBuilder<K, V>> getBuilders() {
        return this.builders.values();
    }

    @Override
    public void add(String name, CacheFactoryBuilder<K, V> builder) {
        this.builders.put(name, builder);
    }

    @Override
    public void remove(String name) {
        this.builders.remove(name);
    }
}
