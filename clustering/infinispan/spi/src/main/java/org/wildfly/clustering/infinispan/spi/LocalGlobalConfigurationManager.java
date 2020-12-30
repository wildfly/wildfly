/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

import org.infinispan.Cache;
import org.infinispan.commons.api.CacheContainerAdmin.AdminFlag;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.SurvivesRestarts;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.globalstate.GlobalConfigurationManager;
import org.infinispan.globalstate.ScopedState;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * Local implementation of {@link GlobalConfigurationManager}.
 * @author Paul Ferraro
 */
@Scope(Scopes.GLOBAL)
@SurvivesRestarts
public class LocalGlobalConfigurationManager implements GlobalConfigurationManager {

    @Inject EmbeddedCacheManager manager;

    @Override
    public CompletableFuture<Configuration> createCache(String cacheName, Configuration configuration, EnumSet<AdminFlag> flags) {
        return CompletableFuture.completedFuture(this.manager.defineConfiguration(cacheName, configuration));
    }

    @Override
    public CompletableFuture<Configuration> getOrCreateCache(String cacheName, Configuration configuration, EnumSet<AdminFlag> flags) {
        Configuration existing = this.manager.getCacheConfiguration(cacheName);
        return CompletableFuture.completedFuture((existing == null) ? this.manager.defineConfiguration(cacheName, configuration) : existing);
    }

    @Override
    public CompletableFuture<Configuration> createCache(String cacheName, String template, EnumSet<AdminFlag> flags) {
        Configuration config = this.manager.getCacheConfiguration(template);
        return CompletableFuture.completedFuture(this.manager.defineConfiguration(cacheName, config));
    }

    @Override
    public CompletableFuture<Configuration> getOrCreateCache(String cacheName, String template, EnumSet<AdminFlag> flags) {
        Configuration config = this.manager.getCacheConfiguration(template);
        return CompletableFuture.completedFuture((config == null) ? this.manager.defineConfiguration(cacheName, config) : config);
    }

    @Override
    public CompletableFuture<Void> removeCache(String cacheName, EnumSet<AdminFlag> flags) {
        this.manager.undefineConfiguration(cacheName);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> createTemplate(String name, Configuration configuration, EnumSet<AdminFlag> flags) {
        this.manager.defineConfiguration(name, configuration);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Configuration> getOrCreateTemplate(String name, Configuration configuration, EnumSet<AdminFlag> flags) {
        Configuration config = this.manager.defineConfiguration(name, configuration);
        return CompletableFuture.completedFuture(config);
    }

    @Override
    public Cache<ScopedState, Object> getStateCache() {
        return this.manager.getCache(CONFIG_STATE_CACHE_NAME);
    }

    @Override
    public CompletableFuture<Void> removeTemplate(String name, EnumSet<AdminFlag> flags) {
        this.manager.undefineConfiguration(name);
        return CompletableFuture.completedFuture(null);
    }
}
