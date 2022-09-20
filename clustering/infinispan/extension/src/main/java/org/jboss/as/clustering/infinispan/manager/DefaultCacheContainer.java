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

package org.jboss.as.clustering.infinispan.manager;

import static org.infinispan.util.logging.Log.CONFIG;

import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.security.auth.Subject;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.cache.impl.AbstractDelegatingAdvancedCache;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.internal.PrivateGlobalConfiguration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.manager.EmbeddedCacheManagerAdmin;
import org.infinispan.manager.impl.AbstractDelegatingEmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.LocalModeAddress;
import org.jboss.as.clustering.infinispan.dataconversion.MediaTypeFactory;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.infinispan.marshall.EncoderRegistry;
import org.wildfly.clustering.infinispan.marshalling.ByteBufferMarshallerFactory;
import org.wildfly.clustering.infinispan.marshalling.MarshalledValueTranscoder;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledKeyFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledValueFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * EmbeddedCacheManager decorator that overrides the default cache semantics of a cache manager.
 * @author Paul Ferraro
 */
public class DefaultCacheContainer extends AbstractDelegatingEmbeddedCacheManager {

    private final EmbeddedCacheManagerAdmin administrator;
    private final Function<ClassLoader, ByteBufferMarshaller> marshallerFactory;

    public DefaultCacheContainer(EmbeddedCacheManager container, ModuleLoader loader) {
        this(container, new ByteBufferMarshallerFactory(container.getCacheManagerConfiguration().serialization().marshaller().mediaType(), loader));
    }

    private DefaultCacheContainer(EmbeddedCacheManager container, Function<ClassLoader, ByteBufferMarshaller> marshallerFactory) {
        super(container);
        this.administrator = new DefaultCacheContainerAdmin(this);
        this.marshallerFactory = marshallerFactory;
    }

    @Override
    public Address getAddress() {
        Address address = super.getAddress();
        return (address != null) ? address : LocalModeAddress.INSTANCE;
    }

    @Override
    public Address getCoordinator() {
        Address coordinator = super.getCoordinator();
        return (coordinator != null) ? coordinator : LocalModeAddress.INSTANCE;
    }

    @Override
    public List<Address> getMembers() {
        List<Address> members = super.getMembers();
        return (members != null) ? members : Collections.singletonList(LocalModeAddress.INSTANCE);
    }

    @Override
    public void start() {
        // No-op.  Lifecycle is managed by container
    }

    @Override
    public void stop() {
        // No-op.  Lifecycle is managed by container
    }

    @Override
    public <K, V> Cache<K, V> getCache() {
        return this.getCache(this.cm.getCacheManagerConfiguration().defaultCacheName().orElseThrow(CONFIG::noDefaultCache));
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName) {
        return this.getCache(cacheName, true);
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName, boolean createIfAbsent) {
        Cache<K, V> cache = this.cm.getCache(cacheName, createIfAbsent);
        if (cache == null) return null;
        Configuration configuration = cache.getCacheConfiguration();
        CacheMode mode = configuration.clustering().cacheMode();
        boolean hasStore = configuration.persistence().usingStores();
        // Bypass deployment-specific media types for local cache w/out a store or for hibernate 2LC
        if ((!mode.isClustered() && !hasStore) || !this.cm.getCacheManagerConfiguration().module(PrivateGlobalConfiguration.class).isServerMode()) {
            return new DefaultCache<>(this, cache);
        }
        ClassLoader loader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        Map.Entry<MediaType, MediaType> types = MediaTypeFactory.INSTANCE.apply(loader);
        MediaType keyType = types.getKey();
        MediaType valueType = (!mode.isInvalidation() || hasStore) ? types.getValue() : MediaType.APPLICATION_OBJECT;
        @SuppressWarnings("deprecation")
        EncoderRegistry registry = (EncoderRegistry) this.cm.getGlobalComponentRegistry().getComponent(org.infinispan.marshall.core.EncoderRegistry.class);
        synchronized (registry) {
            boolean registerKeyMediaType = !registry.isConversionSupported(keyType, MediaType.APPLICATION_OBJECT);
            boolean registerValueMediaType = !registry.isConversionSupported(valueType, MediaType.APPLICATION_OBJECT);
            if (registerKeyMediaType || registerValueMediaType) {
                ByteBufferMarshaller marshaller = this.marshallerFactory.apply(loader);
                if (registerKeyMediaType) {
                    MarshalledValueFactory<ByteBufferMarshaller> keyFactory = new ByteBufferMarshalledKeyFactory(marshaller);
                    registry.registerTranscoder(new MarshalledValueTranscoder<>(keyType, keyFactory));
                }
                if (registerValueMediaType) {
                    MarshalledValueFactory<ByteBufferMarshaller> valueFactory = new ByteBufferMarshalledValueFactory(marshaller);
                    registry.registerTranscoder(new MarshalledValueTranscoder<>(valueType, valueFactory));
                }
            }
            return new DefaultCache<>(this, cache.getAdvancedCache().withMediaType(keyType, valueType)) {
                @Override
                public void stop() {
                    super.stop();
                    if (registerKeyMediaType) {
                        registry.unregisterTranscoder(keyType);
                    }
                    if (registerValueMediaType) {
                        registry.unregisterTranscoder(valueType);
                    }
                }
            };
        }
    }

    @Override
    public Configuration defineConfiguration(String cacheName, Configuration configuration) {
        EmbeddedCacheManager manager = this.cm;
        PrivilegedAction<Configuration> action = new PrivilegedAction<>() {
            @Override
            public Configuration run() {
                return manager.defineConfiguration(cacheName, configuration);
            }
        };
        return WildFlySecurityManager.doUnchecked(action);
    }

    @Override
    public Configuration defineConfiguration(String cacheName, String templateCacheName, Configuration configurationOverride) {
        EmbeddedCacheManager manager = this.cm;
        PrivilegedAction<Configuration> action = new PrivilegedAction<>() {
            @Override
            public Configuration run() {
                return manager.defineConfiguration(cacheName, templateCacheName, configurationOverride);
            }
        };
        return WildFlySecurityManager.doUnchecked(action);
    }

    @Override
    public EmbeddedCacheManager startCaches(String... cacheNames) {
        super.startCaches(cacheNames);
        return this;
    }

    @Override
    public EmbeddedCacheManagerAdmin administration() {
        return this.administrator;
    }

    @Override
    public synchronized <K, V> Cache<K, V> createCache(String name, Configuration configuration) {
        return this.administrator.createCache(name, configuration);
    }

    @Override
    public EmbeddedCacheManager withSubject(Subject subject) {
        return new DefaultCacheContainer(this.cm.withSubject(subject), this.marshallerFactory);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof EmbeddedCacheManager)) return false;
        return this.toString().equals(object.toString());
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public String toString() {
        return this.getCacheManagerConfiguration().cacheManagerName();
    }

    private static class DefaultCache<K, V> extends AbstractDelegatingAdvancedCache<K, V> {
        private final EmbeddedCacheManager manager;

        DefaultCache(EmbeddedCacheManager manager, Cache<K, V> cache) {
            this(manager, cache.getAdvancedCache());
        }

        DefaultCache(EmbeddedCacheManager manager, AdvancedCache<K, V> cache) {
            super(cache);
            this.manager = manager;
        }

        @Override
        public EmbeddedCacheManager getCacheManager() {
            return this.manager;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Cache)) return false;
            Cache<?, ?> cache = (Cache<?, ?>) object;
            return this.manager.equals(cache.getCacheManager()) && this.getName().equals(cache.getName());
        }

        @Override
        public int hashCode() {
            return this.cache.hashCode();
        }

        @SuppressWarnings("unchecked")
        @Override
        public AdvancedCache rewrap(AdvancedCache delegate) {
            return new DefaultCache<>(this.manager, delegate);
        }
    }
}
