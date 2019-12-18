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

import org.infinispan.commons.api.CacheContainerAdmin.AdminFlag;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.globalstate.GlobalConfigurationManager;

/**
 * Local implementation of {@link GlobalConfigurationManager}.
 * @author Paul Ferraro
 */
@Scope(Scopes.GLOBAL)
public class LocalGlobalConfigurationManager implements GlobalConfigurationManager {

    @Override
    public CompletableFuture<Configuration> createCache(String cacheName, Configuration configuration, EnumSet<AdminFlag> flags) {
        return CompletableFuture.completedFuture(configuration);
    }

    @Override
    public CompletableFuture<Configuration> getOrCreateCache(String cacheName, Configuration configuration, EnumSet<AdminFlag> flags) {
        return CompletableFuture.completedFuture(configuration);
    }

    @Override
    public CompletableFuture<Configuration> createCache(String cacheName, String template, EnumSet<AdminFlag> flags) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Configuration> getOrCreateCache(String cacheName, String template, EnumSet<AdminFlag> flags) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> removeCache(String cacheName, EnumSet<AdminFlag> flags) {
        return CompletableFuture.completedFuture(null);
    }
}
