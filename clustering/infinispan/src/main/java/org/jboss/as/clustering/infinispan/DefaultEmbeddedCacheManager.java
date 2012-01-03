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

package org.jboss.as.clustering.infinispan;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.commands.VisitableCommand;
import org.infinispan.config.ConfigurationException;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.manager.AbstractDelegatingEmbeddedCacheManager;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryActivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryInvalidated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryLoaded;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryVisited;
import org.infinispan.notifications.cachelistener.event.Event;

/**
 * @author Paul Ferraro
 */
public class DefaultEmbeddedCacheManager extends AbstractDelegatingEmbeddedCacheManager {
    private final String defaultCache;

    public DefaultEmbeddedCacheManager(EmbeddedCacheManager container, String defaultCache) {
        super(container);
        this.defaultCache = defaultCache;
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#defineConfiguration(java.lang.String, org.infinispan.config.Configuration)
     */
    @Deprecated
    @Override
    public org.infinispan.config.Configuration defineConfiguration(String cacheName, org.infinispan.config.Configuration configurationOverride) {
        return this.cm.defineConfiguration(this.getCacheName(cacheName), configurationOverride);
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#defineConfiguration(java.lang.String, java.lang.String, org.infinispan.config.Configuration)
     */
    @Deprecated
    @Override
    public org.infinispan.config.Configuration defineConfiguration(String cacheName, String templateCacheName, org.infinispan.config.Configuration configurationOverride) {
        return this.cm.defineConfiguration(this.getCacheName(cacheName), this.getCacheName(templateCacheName), configurationOverride);
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#defineConfiguration(String, org.infinispan.configuration.cache.Configuration)
     */
    @SuppressWarnings("deprecation")
    @Override
    public Configuration defineConfiguration(String cacheName, Configuration configuration) {
        this.cm.defineConfiguration(this.getCacheName(cacheName), new LegacyConfigurationAdapter().adapt(configuration));
        return configuration;
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.CacheContainer#getCache()
     */
    @Override
    public <K, V> Cache<K, V> getCache() {
        return this.getCache(this.defaultCache);
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.CacheContainer#getCache(java.lang.String)
     */
    @Override
    public <K, V> Cache<K, V> getCache(String cacheName) {
        return this.getCache(cacheName, true);
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#getCache(java.lang.String, boolean)
     */
    @Override
    public <K, V> Cache<K, V> getCache(String cacheName, boolean start) {
        Cache<K, V> cache = this.cm.<K, V>getCache(this.getCacheName(cacheName), start);
        return (cache != null) ? new DelegatingCache<K, V>(cache) : null;
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#getCacheNames()
     */
    @Override
    public Set<String> getCacheNames() {
        Set<String> names = new HashSet<String>(this.cm.getCacheNames());
        names.add(this.defaultCache);
        return names;
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#isDefaultRunning()
     */
    @Override
    public boolean isDefaultRunning() {
        return this.cm.isRunning(this.defaultCache);
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#isRunning(String)
     */
    @Override
    public boolean isRunning(String cacheName) {
        return this.cm.isRunning(this.getCacheName(cacheName));
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#cacheExists(java.lang.String)
     */
    @Override
    public boolean cacheExists(String cacheName) {
        return this.cm.cacheExists(this.getCacheName(cacheName));
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#removeCache(java.lang.String)
     */
    @Override
    public void removeCache(String cacheName) {
        this.cm.removeCache(this.getCacheName(cacheName));
    }

    @Override
    public EmbeddedCacheManager startCaches(String... names) {
        Set<String> cacheNames = new LinkedHashSet<String>();
        for (String name: names) {
            cacheNames.add(this.getCacheName(name));
        }
        this.cm.startCaches(cacheNames.toArray(new String[cacheNames.size()]));
        return this;
    }

    private String getCacheName(String name) {
        return ((name == null) || name.equals(CacheContainer.DEFAULT_CACHE_NAME)) ? this.defaultCache : name;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.cm.getGlobalConfiguration().getCacheManagerName();
    }

    class DelegatingCache<K, V> extends AbstractAdvancedCache<K, V> {
        DelegatingCache(AdvancedCache<K, V> cache) {
            super(cache);
        }

        DelegatingCache(Cache<K, V> cache) {
            this(cache.getAdvancedCache());
        }

        /**
         * Workaround for ISPN-1682
         */
        @Override
        public Configuration getCacheConfiguration() {
            return new LegacyConfigurationAdapter().adapt(this.getConfiguration());
        }

        @Override
        protected AdvancedCache<K, V> wrap(AdvancedCache<K, V> cache) {
            return new DelegatingCache<K, V>(cache);
        }

        @Override
        public EmbeddedCacheManager getCacheManager() {
            return DefaultEmbeddedCacheManager.this;
        }

        @Override
        public void addListener(Object listener) {
            super.addListener(new ClassLoaderAwareListener(listener, this));
        }

        @Override
        public AdvancedCache<K, V> with(ClassLoader classLoader) {
            AdvancedCache<K, V> cache = super.with(classLoader);
            CommandInterceptor interceptor = new ClassLoaderAwareCommandInterceptor(cache);
            synchronized (this) {
                try {
                    this.cache.addInterceptor(interceptor, 0);
                } catch (ConfigurationException e) {
                    this.cache.removeInterceptor(interceptor.getClass());
                    this.cache.addInterceptor(interceptor, 0);
                }
            }
            return cache;
        }

        @Override
        public synchronized void addInterceptor(CommandInterceptor interceptor, int position) {
            super.addInterceptor(interceptor, (position > 0) ? position : 1);
        }

        @Override
        public synchronized boolean addInterceptorAfter(CommandInterceptor i, Class<? extends CommandInterceptor> afterInterceptor) {
            return super.addInterceptorAfter(i, afterInterceptor);
        }

        @Override
        public synchronized boolean addInterceptorBefore(CommandInterceptor i, Class<? extends CommandInterceptor> beforeInterceptor) {
            return super.addInterceptorBefore(i, beforeInterceptor);
        }

        @Override
        public synchronized void removeInterceptor(int position) {
            super.removeInterceptor(position);
        }

        @Override
        public synchronized void removeInterceptor(Class<? extends CommandInterceptor> interceptorType) {
            super.removeInterceptor(interceptorType);
        }

        @Override
        public boolean equals(Object object) {
            return (object == this) || (object == this.cache);
        }

        @Override
        public int hashCode() {
            return this.cache.hashCode();
        }
    }

    static final Map<Event.Type, Class<? extends Annotation>> events = new EnumMap<Event.Type, Class<? extends Annotation>>(Event.Type.class);

    static {
        events.put(Event.Type.CACHE_ENTRY_ACTIVATED, CacheEntryActivated.class);
        events.put(Event.Type.CACHE_ENTRY_CREATED, CacheEntryCreated.class);
        events.put(Event.Type.CACHE_ENTRY_INVALIDATED, CacheEntryInvalidated.class);
        events.put(Event.Type.CACHE_ENTRY_LOADED, CacheEntryLoaded.class);
        events.put(Event.Type.CACHE_ENTRY_MODIFIED, CacheEntryModified.class);
        events.put(Event.Type.CACHE_ENTRY_PASSIVATED, CacheEntryPassivated.class);
        events.put(Event.Type.CACHE_ENTRY_REMOVED, CacheEntryRemoved.class);
        events.put(Event.Type.CACHE_ENTRY_VISITED, CacheEntryVisited.class);
    }

    @Listener
    public static class ClassLoaderAwareListener {
        private final Object listener;
        private final Map<Event.Type, List<Method>> methods = new EnumMap<Event.Type, List<Method>>(Event.Type.class);
        private final AdvancedCache<?, ?> cache;

        public ClassLoaderAwareListener(Object listener, AdvancedCache<?, ?> cache) {
            this.listener = listener;
            this.cache = cache;
            for (Method method : listener.getClass().getMethods()) {
                for (Map.Entry<Event.Type, Class<? extends Annotation>> entry : events.entrySet()) {
                    Class<? extends Annotation> annotation = entry.getValue();
                    if (method.isAnnotationPresent(annotation)) {
                        List<Method> methods = this.methods.get(entry.getValue());
                        if (methods == null) {
                            methods = new LinkedList<Method>();
                            this.methods.put(entry.getKey(), methods);
                        }
                        methods.add(method);
                    }
                }
            }
        }

        @CacheEntryActivated
        @CacheEntryCreated
        @CacheEntryInvalidated
        @CacheEntryLoaded
        @CacheEntryModified
        @CacheEntryPassivated
        @CacheEntryRemoved
        @CacheEntryVisited
        public <K, V> void event(Event<K, V> event) throws Throwable {
            List<Method> methods = this.methods.get(event.getType());
            if (methods != null) {
                ClassLoader cacheLoader = this.cache.getClassLoader();
                ClassLoader contextLoader = getContextClassLoader();
                if (cacheLoader != null) {
                    setContextClassLoader(cacheLoader);
                }
                try {
                    for (Method method : this.methods.get(event.getType())) {
                        try {
                            method.invoke(this.listener, event);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    }
                } finally {
                    if (cacheLoader != null) {
                        setContextClassLoader(contextLoader);
                    }
                }
            }
        }

        public int hashCode() {
            return this.listener.hashCode();
        }

        public boolean equals(Object object) {
            if (object == null)
                return false;
            if (object instanceof ClassLoaderAwareListener) {
                ClassLoaderAwareListener listener = (ClassLoaderAwareListener) object;
                return this.listener.equals(listener.listener);
            }
            return this.listener.equals(object);
        }
    }

    private class ClassLoaderAwareCommandInterceptor extends CommandInterceptor {
        private final AdvancedCache<?, ?> cache;

        ClassLoaderAwareCommandInterceptor(AdvancedCache<?, ?> cache) {
            this.cache = cache;
        }

        @Override
        protected Object handleDefault(InvocationContext ctx, VisitableCommand command) throws Throwable {
            ClassLoader classLoader = this.cache.getClassLoader();
            ClassLoader contextLoader = getContextClassLoader();
            if (classLoader != null) {
                setContextClassLoader(classLoader);
            }
            try {
                return super.handleDefault(ctx, command);
            } finally {
                if (classLoader != null) {
                    setContextClassLoader(contextLoader);
                }
            }
        }
    }

    static ClassLoader getContextClassLoader() {
        PrivilegedAction<ClassLoader> action = new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        };
        return AccessController.doPrivileged(action);
    }

    static void setContextClassLoader(final ClassLoader loader) {
        PrivilegedAction<Void> action = new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                Thread.currentThread().setContextClassLoader(loader);
                return null;
            }
        };
        AccessController.doPrivileged(action);
    }
}
