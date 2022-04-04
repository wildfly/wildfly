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

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.infinispan.client.hotrod.DataFormat;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManagerAdmin;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.marshall.Marshaller;
import org.jboss.as.clustering.infinispan.client.RemoteCacheManager;
import org.jboss.as.clustering.infinispan.dataconversion.MediaTypeFactory;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.infinispan.client.NearCacheFactory;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.marshalling.ByteBufferMarshallerFactory;
import org.wildfly.clustering.infinispan.marshalling.UserMarshaller;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Container managed {@link RemoteCacheManager} decorator, whose lifecycle methods are no-ops.
 *
 * @author Radoslav Husar
 * @author Paul Ferraro
 */
public class ManagedRemoteCacheContainer implements RemoteCacheContainer {

    private final RemoteCacheManager manager;
    private final Function<ClassLoader, ByteBufferMarshaller> marshallerFactory;

    public ManagedRemoteCacheContainer(RemoteCacheManager manager, ModuleLoader loader) {
        this.manager = manager;
        this.marshallerFactory = new ByteBufferMarshallerFactory(manager.getConfiguration().marshaller().mediaType(), loader);
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
    public <K, V> NearCacheRegistration registerNearCacheFactory(String cacheName, NearCacheFactory<K, V> factory) {
        return this.manager.registerNearCacheFactory(cacheName, factory);
    }

    @Override
    public <K, V> RemoteCache<K, V> getCache() {
        return this.getCache("");
    }

    @Override
    public <K, V> RemoteCache<K, V> getCache(String cacheName) {
        RemoteCache<K, V> cache = this.manager.getCache(cacheName);
        if (cache == null) return null;
        Marshaller defaultMarshaller = this.manager.getMarshaller();
        DataFormat.Builder builder = DataFormat.builder().from(cache.getDataFormat())
                .keyType(defaultMarshaller.mediaType())
                .valueType(defaultMarshaller.mediaType())
                ;
        ClassLoader loader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        Map.Entry<MediaType, MediaType> types = MediaTypeFactory.INSTANCE.apply(loader);
        boolean overrideKeyMarshaller = !types.getKey().equals(MediaType.APPLICATION_OBJECT);
        boolean overrideValueMarshaller = !types.getValue().equals(MediaType.APPLICATION_OBJECT);
        if (overrideKeyMarshaller || overrideValueMarshaller) {
            Marshaller marshaller = new UserMarshaller(defaultMarshaller.mediaType(), this.marshallerFactory.apply(loader));
            if (overrideKeyMarshaller) {
                builder.keyType(marshaller.mediaType());
                builder.keyMarshaller(marshaller);
            }
            if (overrideValueMarshaller) {
                builder.valueType(marshaller.mediaType());
                builder.valueMarshaller(marshaller);
            }
        }
        return cache.withDataFormat(builder.build());
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

    @Override
    public boolean isTransactional(String cacheName) {
        return this.manager.isTransactional(cacheName);
    }
}
