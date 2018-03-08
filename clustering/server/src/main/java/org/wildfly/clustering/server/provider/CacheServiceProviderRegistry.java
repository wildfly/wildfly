/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.server.provider;

import java.security.PrivilegedAction;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.context.Flag;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.GroupListener;
import org.wildfly.clustering.group.Membership;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.provider.ServiceProviderRegistration;
import org.wildfly.clustering.provider.ServiceProviderRegistration.Listener;
import org.wildfly.clustering.service.concurrent.ClassLoaderThreadFactory;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;

/**
 * Infinispan {@link Cache} based {@link ServiceProviderRegistry}.
 * This factory can create multiple {@link ServiceProviderRegistration} instance,
 * all of which share the same {@link Cache} instance.
 * @author Paul Ferraro
 */
@org.infinispan.notifications.Listener(sync = false)
public class CacheServiceProviderRegistry<T> implements ServiceProviderRegistry<T>, GroupListener, AutoCloseable {

    private static ThreadFactory createThreadFactory(Class<?> targetClass) {
        PrivilegedAction<ThreadFactory> action = () -> new ClassLoaderThreadFactory(new JBossThreadFactory(new ThreadGroup(targetClass.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null), targetClass.getClassLoader());
        return WildFlySecurityManager.doUnchecked(action);
    }

    private final ConcurrentMap<T, Map.Entry<Listener, ExecutorService>> listeners = new ConcurrentHashMap<>();
    private final Batcher<? extends Batch> batcher;
    private final Cache<T, Set<Node>> cache;
    private final Group group;
    private final Registration groupRegistration;
    private final CommandDispatcher<Set<T>> dispatcher;

    public CacheServiceProviderRegistry(CacheServiceProviderRegistryConfiguration<T> config) {
        this.group = config.getGroup();
        this.cache = config.getCache();
        this.batcher = config.getBatcher();
        this.dispatcher = config.getCommandDispatcherFactory().createCommandDispatcher(config.getId(), this.listeners.keySet());
        this.cache.addListener(this);
        this.groupRegistration = this.group.register(this);
    }

    @Override
    public void close() {
        this.groupRegistration.close();
        this.cache.removeListener(this);
        this.dispatcher.close();
        // Cleanup any unclosed registrations
        for (Map.Entry<Listener, ExecutorService> entry : this.listeners.values()) {
            ExecutorService executor = entry.getValue();
            if (executor != null) {
                PrivilegedAction<List<Runnable>> action = () -> executor.shutdownNow();
                WildFlySecurityManager.doUnchecked(action);
            }
        }
        this.listeners.clear();
    }

    @Override
    public Group getGroup() {
        return this.group;
    }

    @Override
    public ServiceProviderRegistration<T> register(T service) {
        return this.register(service, null);
    }

    @Override
    public ServiceProviderRegistration<T> register(T service, Listener listener) {
        Map.Entry<Listener, ExecutorService> newEntry = new AbstractMap.SimpleEntry<>(listener, null);
        // Only create executor for new registrations
        Map.Entry<Listener, ExecutorService> entry = this.listeners.computeIfAbsent(service, key -> {
            if (listener != null) {
                newEntry.setValue(Executors.newSingleThreadExecutor(createThreadFactory(listener.getClass())));
            }
            return newEntry;
        });
        if (entry != newEntry) {
            throw new IllegalArgumentException(service.toString());
        }
        try (Batch batch = this.batcher.createBatch()) {
            this.register(this.group.getLocalMember(), service);
        }
        return new SimpleServiceProviderRegistration<>(service, this, () -> {
            Node node = this.getGroup().getLocalMember();
            try (Batch batch = this.batcher.createBatch()) {
                Set<Node> nodes = this.cache.getAdvancedCache().withFlags(Flag.FORCE_WRITE_LOCK).get(service);
                if ((nodes != null) && nodes.remove(node)) {
                    Cache<T, Set<Node>> cache = this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES);
                    if (nodes.isEmpty()) {
                        cache.remove(service);
                    } else {
                        cache.replace(service, nodes);
                    }
                }
            } finally {
                Map.Entry<Listener, ExecutorService> oldEntry = this.listeners.remove(service);
                if (oldEntry != null) {
                    ExecutorService executor = oldEntry.getValue();
                    if (executor != null) {
                        PrivilegedAction<List<Runnable>> action = () -> executor.shutdownNow();
                        WildFlySecurityManager.doUnchecked(action);
                        try {
                            executor.awaitTermination(this.cache.getCacheConfiguration().transaction().cacheStopTimeout(), TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        });
    }

    void register(Node node, T service) {
        Set<Node> nodes = this.cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS, Flag.FORCE_WRITE_LOCK).computeIfAbsent(service, key -> new CopyOnWriteArraySet<>(Collections.singleton(node)));
        if (nodes.add(node)) {
            this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).replace(service, nodes);
        }
    }

    @Override
    public Set<Node> getProviders(final T service) {
        Set<Node> nodes = this.cache.get(service);
        return (nodes != null) ? Collections.unmodifiableSet(nodes) : Collections.emptySet();
    }

    @Override
    public Set<T> getServices() {
        return this.cache.keySet();
    }

    @Override
    public void membershipChanged(Membership previousMembership, Membership membership, final boolean merged) {
        if (membership.isCoordinator()) {
            List<Node> previousMembers = previousMembership.getMembers();
            List<Node> members = membership.getMembers();
            Set<Node> leftMembers = new HashSet<>(previousMembers);
            leftMembers.removeAll(members);
            Set<Node> joinedMembers = new HashSet<>(members);
            joinedMembers.removeAll(previousMembers);
            if (!leftMembers.isEmpty()) {
                try (Batch batch = this.batcher.createBatch()) {
                    try (CloseableIterator<Map.Entry<T, Set<Node>>> entries = this.cache.entrySet().iterator()) {
                        while (entries.hasNext()) {
                            Map.Entry<T, Set<Node>> entry = entries.next();
                            Set<Node> nodes = entry.getValue();
                            if (nodes.removeAll(leftMembers)) {
                                entry.setValue(nodes);
                            }
                        }
                    }
                }
            }
            if (merged) {
                // Re-assert services for new members following merge since these may have been lost following split
                for (Node joinedMember : joinedMembers) {
                    try {
                        Collection<T> services = this.dispatcher.executeOnNode(new GetLocalServicesCommand<>(), joinedMember).get();
                        try (Batch batch = this.batcher.createBatch()) {
                            for (T service : services) {
                                this.register(joinedMember, service);
                            }
                        }
                    } catch (Exception e) {
                        ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                    }
                }
            }
        }
    }

    @CacheEntryCreated
    @CacheEntryModified
    public void modified(CacheEntryEvent<T, Set<Node>> event) {
        if (event.isPre()) return;
        Map.Entry<Listener, ExecutorService> entry = this.listeners.get(event.getKey());
        if (entry != null) {
            Listener listener = entry.getKey();
            if (listener != null) {
                ExecutorService executor = entry.getValue();
                try {
                    executor.submit(() -> {
                        try {
                            listener.providersChanged(event.getValue());
                        } catch (Throwable e) {
                            ClusteringServerLogger.ROOT_LOGGER.serviceProviderRegistrationListenerFailed(e, this.cache.getCacheManager().getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName(), this.cache.getName(), event.getValue());
                        }
                    });
                } catch (RejectedExecutionException e) {
                    // Executor was shutdown
                }
            }
        }
    }
}
