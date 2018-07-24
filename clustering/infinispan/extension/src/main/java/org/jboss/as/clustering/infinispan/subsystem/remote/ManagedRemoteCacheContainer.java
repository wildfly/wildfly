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

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.RemoteCacheManagerAdmin;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.marshall.Marshaller;
import org.wildfly.clustering.infinispan.spi.RemoteCacheContainer;

import java.util.Set;

import javax.transaction.TransactionManager;

/**
 * Default implementation of container managed {@link RemoteCacheContainer}.
 *
 * @author Radoslav Husar
 */
public class ManagedRemoteCacheContainer implements RemoteCacheContainer {

    private final String name;
    private final RemoteCacheManager remoteCacheManager;

    public ManagedRemoteCacheContainer(String name, RemoteCacheManager remoteCacheManager) {
        this.name = name;
        this.remoteCacheManager = remoteCacheManager;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public RemoteCacheManagerAdmin administration() {
        return this.remoteCacheManager.administration();
    }

    @Override
    public <K, V> BasicCache<K, V> getCache() {
        return this.remoteCacheManager.getCache();
    }

    @Override
    public <K, V> BasicCache<K, V> getCache(String cacheName) {
        return this.remoteCacheManager.getCache(cacheName);
    }

    @Override
    public <K, V> RemoteCache<K, V> getCache(String cacheName, TransactionMode transactionMode, TransactionManager transactionManager) {
        return this.remoteCacheManager.getCache(cacheName, transactionMode, transactionManager);
    }

    @Override
    public <K, V> RemoteCache<K, V> getCache(String cacheName, boolean forceReturnValue, TransactionMode transactionMode, TransactionManager transactionManager) {
        return this.remoteCacheManager.getCache(cacheName, forceReturnValue, transactionMode, transactionManager);
    }

    @Override
    public Configuration getConfiguration() {
        return this.remoteCacheManager.getConfiguration();
    }

    @Override
    public boolean isStarted() {
        return this.remoteCacheManager.isStarted();
    }

    @Override
    public boolean switchToCluster(String clusterName) {
        return this.remoteCacheManager.switchToCluster(clusterName);
    }

    @Override
    public boolean switchToDefaultCluster() {
        return this.remoteCacheManager.switchToDefaultCluster();
    }

    @Override
    public Marshaller getMarshaller() {
        return this.remoteCacheManager.getMarshaller();
    }

    @Override
    public Set<String> getCacheNames() {
        return this.remoteCacheManager.getCacheNames();
    }

    @Override
    public void start() {
        // no-op - lifecycle controller by the container
    }

    @Override
    public void stop() {
        // no-op - lifecycle controller by the container
    }
}
