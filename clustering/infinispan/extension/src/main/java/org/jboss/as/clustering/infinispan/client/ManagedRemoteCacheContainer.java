/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.client;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.infinispan.client.hotrod.DataFormat;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.RemoteCacheManagerAdmin;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.impl.operations.OperationsFactory;
import org.infinispan.client.hotrod.impl.operations.PingResponse;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.marshall.Marshaller;
import org.jboss.as.clustering.infinispan.dataconversion.MediaTypeFactory;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.Registrar;
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
 *
 */
public class ManagedRemoteCacheContainer implements RemoteCacheContainer {

    private final RemoteCacheManager container;
    private final OperationsFactory factory;
    private final String name;
    private final Function<ClassLoader, ByteBufferMarshaller> marshallerFactory;
    private final Registrar<String> registrar;

    public ManagedRemoteCacheContainer(RemoteCacheManager container, String name, ModuleLoader loader, Registrar<String> registrar) {
        this.container = container;
        this.name = name;
        this.registrar = registrar;
        this.marshallerFactory = new ByteBufferMarshallerFactory(container.getConfiguration().marshaller().mediaType(), loader);
        this.factory = new OperationsFactory(container.getChannelFactory(), null, container.getConfiguration());
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public RemoteCacheManagerAdmin administration() {
        return this.container.administration();
    }

    @Override
    public CompletionStage<Boolean> isAvailable() {
        return this.factory.newFaultTolerantPingOperation().execute().thenApply(PingResponse::isSuccess);
    }

    @Override
    public <K, V> RemoteCache<K, V> getCache() {
        return this.getCache("");
    }

    @Override
    public <K, V> RemoteCache<K, V> getCache(String cacheName) {
        RemoteCache<K, V> cache = this.container.getCache(cacheName);
        if (cache == null) return null;
        Marshaller defaultMarshaller = this.container.getMarshaller();
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
        return new ManagedRemoteCache<>(this, this.container, cache.withDataFormat(builder.build()), this.registrar);
    }

    @Override
    public Configuration getConfiguration() {
        return this.container.getConfiguration();
    }

    @Override
    public boolean isStarted() {
        return this.container.isStarted();
    }

    @Override
    public String getCurrentClusterName() {
        return this.container.getCurrentClusterName();
    }

    @Override
    public boolean switchToCluster(String clusterName) {
        return this.container.switchToCluster(clusterName);
    }

    @Override
    public boolean switchToDefaultCluster() {
        return this.container.switchToDefaultCluster();
    }

    @Override
    public Marshaller getMarshaller() {
        return this.container.getMarshaller();
    }

    @Override
    public boolean isTransactional(String cacheName) {
        return this.container.isTransactional(cacheName);
    }

    @Override
    public Set<String> getCacheNames() {
        return this.container.getCacheNames();
    }

    @Override
    public void start() {
        // Do nothing
    }

    @Override
    public void stop() {
        // Do nothing
    }

    @Override
    public String[] getServers() {
        return this.container.getServers();
    }

    @Override
    public int getActiveConnectionCount() {
        return this.container.getActiveConnectionCount();
    }

    @Override
    public int getConnectionCount() {
        return this.container.getConnectionCount();
    }

    @Override
    public int getIdleConnectionCount() {
        return this.container.getIdleConnectionCount();
    }

    @Override
    public long getRetries() {
        return this.container.getRetries();
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof RemoteCacheContainer)) return false;
        RemoteCacheContainer container = (RemoteCacheContainer) object;
        return this.name.equals(container.getName());
    }

    @Override
    public String toString() {
        return this.name;
    }
}
