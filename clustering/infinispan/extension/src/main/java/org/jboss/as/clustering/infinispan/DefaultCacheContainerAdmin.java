/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan;

import java.util.EnumSet;

import javax.security.auth.Subject;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.manager.EmbeddedCacheManagerAdmin;

/**
 * Custom {@link EmbeddedCacheManagerAdmin} that does not use on a GlobalConfigurationManager.
 * @author Paul Ferraro
 */
public class DefaultCacheContainerAdmin implements EmbeddedCacheManagerAdmin {

    private final EmbeddedCacheManager manager;

    public DefaultCacheContainerAdmin(EmbeddedCacheManager manager) {
        this.manager = manager;
    }

    @Override
    public <K, V> Cache<K, V> createCache(String name, String template) {
        return this.createCache(name, this.manager.getCacheConfiguration(name));
    }

    @Override
    public <K, V> Cache<K, V> getOrCreateCache(String name, String template) {
        return this.getOrCreateCache(name, this.manager.getCacheConfiguration(name));
    }

    @Override
    public synchronized <K, V> Cache<K, V> createCache(String name, Configuration configuration) {
        this.manager.defineConfiguration(name, configuration);
        return this.manager.getCache(name);
    }

    @Override
    public synchronized <K, V> Cache<K, V> getOrCreateCache(String name, Configuration configuration) {
        return (this.manager.getCacheConfiguration(name) != null) ? this.createCache(name, configuration) : this.manager.getCache(name);
    }

    @Override
    public void removeCache(String name) {
        this.manager.undefineConfiguration(name);
    }

    @Override
    public EmbeddedCacheManagerAdmin withFlags(AdminFlag... flags) {
        return this;
    }

    @Override
    public EmbeddedCacheManagerAdmin withFlags(EnumSet<AdminFlag> flags) {
        return this;
    }

    @Override
    public EmbeddedCacheManagerAdmin withSubject(Subject subject) {
        return this;
    }
}
