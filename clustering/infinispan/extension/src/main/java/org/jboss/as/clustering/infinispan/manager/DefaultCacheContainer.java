/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.manager;

import static org.infinispan.util.logging.Log.CONFIG;

import java.lang.annotation.Annotation;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import javax.security.auth.Subject;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.cache.impl.AbstractDelegatingAdvancedCache;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.util.AggregatedClassLoader;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.EncodingConfiguration;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.manager.EmbeddedCacheManagerAdmin;
import org.infinispan.manager.impl.AbstractDelegatingEmbeddedCacheManager;
import org.infinispan.notifications.cachelistener.filter.CacheEventConverter;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.LocalModeAddress;
import org.jboss.as.clustering.infinispan.dataconversion.MediaTypeFactory;
import org.jboss.as.clustering.infinispan.marshalling.MarshallerFactory;
import org.jboss.as.clustering.infinispan.marshalling.UserMarshallerFactory;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.cache.infinispan.embedded.marshall.EncoderRegistry;
import org.wildfly.clustering.cache.infinispan.marshalling.MarshalledValueTranscoder;
import org.wildfly.clustering.cache.infinispan.marshalling.MediaTypes;
import org.wildfly.clustering.cache.infinispan.marshalling.UserMarshaller;
import org.wildfly.clustering.marshalling.ByteBufferMarshalledKeyFactory;
import org.wildfly.clustering.marshalling.ByteBufferMarshalledValueFactory;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.MarshalledValueFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * EmbeddedCacheManager decorator that overrides the default cache semantics of a cache manager.
 * @author Paul Ferraro
 */
public class DefaultCacheContainer extends AbstractDelegatingEmbeddedCacheManager {

    private final EmbeddedCacheManagerAdmin administrator;
    private final ModuleLoader loader;
    private final MarshallerFactory marshallerFactory;

    public DefaultCacheContainer(EmbeddedCacheManager container, ModuleLoader loader) {
        super(container);
        this.administrator = new DefaultCacheContainerAdmin(this);
        this.loader = loader;
        this.marshallerFactory = UserMarshallerFactory.forMediaType(container.getCacheManagerConfiguration().serialization().marshaller().mediaType());
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
        Cache<K, V> cache = AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public Cache<K, V> run() {
                return DefaultCacheContainer.this.cm.getCache(cacheName, createIfAbsent);
            }
        });
        if (cache == null) return null;
        Configuration configuration = cache.getCacheConfiguration();
        CacheMode mode = configuration.clustering().cacheMode();
        boolean hasStore = configuration.persistence().usingStores();
        // Bypass deployment-specific media types for heap-based local cache w/out a store or if using a non-ProtoStream marshaller
        if ((!mode.isClustered() && !hasStore && configuration.memory().storage().canStoreReferences()) || !this.cm.getCacheManagerConfiguration().serialization().marshaller().mediaType().equals(MediaTypes.WILDFLY_PROTOSTREAM.get())) {
            return new DefaultCache<>(this, cache);
        }
        ClassLoader loader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        Map.Entry<MediaType, MediaType> types = MediaTypeFactory.INSTANCE.apply(loader);
        MediaType keyType = types.getKey();
        MediaType valueType = (!mode.isInvalidation() || hasStore) ? types.getValue() : MediaType.APPLICATION_OBJECT;
        EncoderRegistry registry = (EncoderRegistry) GlobalComponentRegistry.componentOf(this.cm, org.infinispan.marshall.core.EncoderRegistry.class);
        synchronized (registry) {
            boolean registerKeyMediaType = !registry.isConversionSupported(keyType, MediaType.APPLICATION_OBJECT);
            boolean registerValueMediaType = !registry.isConversionSupported(valueType, MediaType.APPLICATION_OBJECT);
            if (registerKeyMediaType || registerValueMediaType) {
                ClassLoader managerLoader = this.cm.getCacheManagerConfiguration().classLoader();
                PrivilegedAction<ClassLoader> action = new PrivilegedAction<>() {
                    @Override
                    public ClassLoader run() {
                        return new AggregatedClassLoader(List.of(loader, managerLoader));
                    }
                };
                ByteBufferMarshaller marshaller = this.marshallerFactory.createByteBufferMarshaller(this.loader, List.of(WildFlySecurityManager.doUnchecked(action)));
                if (registerKeyMediaType) {
                    MarshalledValueFactory<ByteBufferMarshaller> keyFactory = new ByteBufferMarshalledKeyFactory(marshaller);
                    registry.registerTranscoder(new MarshalledValueTranscoder<>(keyType, keyFactory, new UserMarshaller(keyType, marshaller)));
                }
                if (registerValueMediaType) {
                    MarshalledValueFactory<ByteBufferMarshaller> valueFactory = new ByteBufferMarshalledValueFactory(marshaller);
                    registry.registerTranscoder(new MarshalledValueTranscoder<>(valueType, valueFactory, new UserMarshaller(valueType, marshaller)));
                }
            }
            AdvancedCache<K, V> advancedCache = cache.getAdvancedCache();
            EncodingConfiguration encoding = configuration.encoding();
            return new DefaultCache<>(this, encoding.keyDataType().mediaType().equals(keyType) && encoding.valueDataType().mediaType().equals(valueType) ? advancedCache : advancedCache.withMediaType(keyType, valueType)) {
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
    public void addListener(Object listener) {
        AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public Void run() {
                DefaultCacheContainer.super.addListener(listener);
                return null;
            }
        });
    }

    @Override
    public CompletionStage<Void> addListenerAsync(Object listener) {
        return AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public CompletionStage<Void> run() {
                return DefaultCacheContainer.super.addListenerAsync(listener);
            }
        });
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
        return new DefaultCacheContainer(this.cm.withSubject(subject), this.loader);
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

        @Override
        public void addListener(Object listener) {
            AccessController.doPrivileged(new PrivilegedAction<>() {
                @Override
                public Void run() {
                    DefaultCache.super.addListener(listener);
                    return null;
                }
            });
        }

        @Override
        public CompletionStage<Void> addListenerAsync(Object listener) {
            return AccessController.doPrivileged(new PrivilegedAction<>() {
                @Override
                public CompletionStage<Void> run() {
                    return DefaultCache.super.addListenerAsync(listener);
                }
            });
        }

        @Override
        public <C> void addListener(Object listener, CacheEventFilter<? super K, ? super V> filter, CacheEventConverter<? super K, ? super V, C> converter) {
            AccessController.doPrivileged(new PrivilegedAction<>() {
                @Override
                public Void run() {
                    DefaultCache.super.addListener(listener, filter, converter);
                    return null;
                }
            });
        }

        @Override
        public <C> CompletionStage<Void> addListenerAsync(Object listener, CacheEventFilter<? super K, ? super V> filter, CacheEventConverter<? super K, ? super V, C> converter) {
            return AccessController.doPrivileged(new PrivilegedAction<>() {
                @Override
                public CompletionStage<Void> run() {
                    return DefaultCache.super.addListenerAsync(listener, filter, converter);
                }
            });
        }

        @Override
        public <C> void addFilteredListener(Object listener, CacheEventFilter<? super K, ? super V> filter, CacheEventConverter<? super K, ? super V, C> converter, Set<Class<? extends Annotation>> filterAnnotations) {
            AccessController.doPrivileged(new PrivilegedAction<>() {
                @Override
                public Void run() {
                    DefaultCache.super.addFilteredListener(listener, filter, converter, filterAnnotations);
                    return null;
                }
            });
        }

        @Override
        public <C> CompletionStage<Void> addFilteredListenerAsync(Object listener, CacheEventFilter<? super K, ? super V> filter, CacheEventConverter<? super K, ? super V, C> converter, Set<Class<? extends Annotation>> filterAnnotations) {
            return AccessController.doPrivileged(new PrivilegedAction<>() {
                @Override
                public CompletionStage<Void> run() {
                    return DefaultCache.super.addFilteredListenerAsync(listener, filter, converter, filterAnnotations);
                }
            });
        }

        @Override
        public <C> void addStorageFormatFilteredListener(Object listener, CacheEventFilter<? super K, ? super V> filter, CacheEventConverter<? super K, ? super V, C> converter, Set<Class<? extends Annotation>> filterAnnotations) {
            AccessController.doPrivileged(new PrivilegedAction<>() {
                @Override
                public Void run() {
                    DefaultCache.super.addStorageFormatFilteredListener(listener, filter, converter, filterAnnotations);
                    return null;
                }
            });
        }

        @Override
        public <C> CompletionStage<Void> addStorageFormatFilteredListenerAsync(Object listener, CacheEventFilter<? super K, ? super V> filter, CacheEventConverter<? super K, ? super V, C> converter, Set<Class<? extends Annotation>> filterAnnotations) {
            return AccessController.doPrivileged(new PrivilegedAction<>() {
                @Override
                public CompletionStage<Void> run() {
                    return DefaultCache.super.addStorageFormatFilteredListenerAsync(listener, filter, converter, filterAnnotations);
                }
            });
        }
    }
}
