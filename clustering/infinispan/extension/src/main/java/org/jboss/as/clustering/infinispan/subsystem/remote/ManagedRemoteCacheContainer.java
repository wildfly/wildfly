/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.Set;
import java.util.function.Function;

import javax.transaction.TransactionManager;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManagerAdmin;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.infinispan.client.hotrod.event.impl.ClientListenerNotifier;
import org.infinispan.client.hotrod.near.NearCacheService;
import org.infinispan.commons.marshall.Marshaller;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.manager.RemoteCacheManager;

/**
 * Container managed {@link RemoteCacheContainer} decorator, whose lifecycle methods are no-ops.
 *
 * @author Radoslav Husar
 * @author Paul Ferraro
 */
public class ManagedRemoteCacheContainer implements RemoteCacheContainer {

    private final RemoteCacheManager manager;

    public ManagedRemoteCacheContainer(RemoteCacheManager container) {
        this.manager = container;
    }

    @Override
    public String getName() {
        return this.manager.getName();
    }

    @Override
    public RemoteCacheManagerAdmin administration() {
        return this.manager.administration();
    }

    @Override
    public <K, V> NearCacheRegistration registerNearCacheFactory(String cacheName, Function<ClientListenerNotifier, NearCacheService<K, V>> factory) {
        return this.manager.registerNearCacheFactory(cacheName, factory);
    }

    @Override
    public <K, V> RemoteCache<K, V> getCache(String cacheName, boolean forceReturnValue, TransactionMode transactionMode, TransactionManager transactionManager) {
        return this.manager.getCache(cacheName, forceReturnValue, transactionMode, transactionManager);
    }

    @Override
    public Configuration getConfiguration() {
        return this.manager.getConfiguration();
    }

    @Override
    public boolean isStarted() {
        return this.manager.isStarted();
    }

    @Override
    public boolean switchToCluster(String clusterName) {
        return this.manager.switchToCluster(clusterName);
    }

    @Override
    public boolean switchToDefaultCluster() {
        return this.manager.switchToDefaultCluster();
    }

    @Override
    public Marshaller getMarshaller() {
        return this.manager.getMarshaller();
    }

    @Override
    public Set<String> getCacheNames() {
        return this.manager.getCacheNames();
    }

    @Override
    public void start() {
        // no-op - lifecycle controller by the container
    }

    @Override
    public void stop() {
        // no-op - lifecycle controller by the container
    }

    @Override
    public String[] getServers() {
        return this.manager.getServers();
    }

    @Override
    public int getActiveConnectionCount() {
        return this.manager.getActiveConnectionCount();
    }

    @Override
    public int getConnectionCount() {
        return this.manager.getConnectionCount();
    }

    @Override
    public int getIdleConnectionCount() {
        return this.manager.getIdleConnectionCount();
    }

    @Override
    public long getRetries() {
        return this.manager.getRetries();
    }
}
