/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.infinispan.AdvancedCache;
import org.infinispan.commands.VisitableCommand;
import org.infinispan.config.ConfigurationException;
import org.infinispan.context.InvocationContext;
import org.infinispan.interceptors.base.CommandInterceptor;
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
 * AdvancedCache decorator that gracefully handle TCCL switching for cache commands and events.
 * @author Paul Ferraro
 */
public class ClassLoaderAwareCache<K, V> extends AbstractAdvancedCache<K, V> {

    volatile WeakReference<ClassLoader> classLoaderRef;

    public ClassLoaderAwareCache(AdvancedCache<K, V> cache, ClassLoader classLoader) {
        super(cache);
        this.classLoaderRef = new WeakReference<ClassLoader>(classLoader);
        try {
            cache.addInterceptor(new ClassLoaderAwareCommandInterceptor(this), 0);
        } catch (ConfigurationException e) {
            // This means the ClassLoaderAwareCommandInterceptor is already in the interceptor chain
        }
    }

    @Override
    protected AdvancedCache<K, V> wrap(AdvancedCache<K, V> cache) {
        return new ClassLoaderAwareCache<K, V>(cache, this.getClassLoader());
    }

    @Override
    public AdvancedCache<K, V> with(ClassLoader classLoader) {
        this.classLoaderRef = new WeakReference<ClassLoader>(classLoader);
        return this;
    }

    @Override
    public void addInterceptor(CommandInterceptor interceptor, int position) {
        // Don't let some other interceptor step in front of the ClassLoaderAwareCommandInterceptor
        super.addInterceptor(interceptor, (position > 0) ? position : 1);
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.classLoaderRef.get();
    }

    @Override
    public void addListener(Object listener) {
        super.addListener(new ClassLoaderAwareListener(listener, this));
    }

    class ClassLoaderAwareCommandInterceptor extends CommandInterceptor {
        private final AdvancedCache<?, ?> cache;
        ClassLoaderAwareCommandInterceptor(AdvancedCache<?, ?> cache) {
            this.cache = cache;
        }
        @Override
        protected Object handleDefault(InvocationContext ctx, VisitableCommand command) throws Throwable {
            ClassLoader cacheLoader = this.cache.getClassLoader();
            ClassLoader contextLoader = getContextClassLoader();
            if (cacheLoader != null) {
                setContextClassLoader(cacheLoader);
            }
            try {
                return super.handleDefault(ctx, command);
            } finally {
                if (cacheLoader != null) {
                    setContextClassLoader(contextLoader);
                }
            }
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
