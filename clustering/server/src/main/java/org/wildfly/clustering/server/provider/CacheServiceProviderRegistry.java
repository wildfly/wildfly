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

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.context.Flag;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.concurrent.CompletableFutures;
import org.jboss.as.clustering.context.DefaultExecutorService;
import org.jboss.as.clustering.context.DefaultThreadFactory;
import org.jboss.as.clustering.context.ExecutorServiceFactory;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Invoker;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.infinispan.InfinispanCacheProperties;
import org.wildfly.clustering.ee.infinispan.retry.RetryingInvoker;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.spi.distribution.ConsistentHashLocality;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;
import org.wildfly.clustering.provider.ServiceProviderRegistration;
import org.wildfly.clustering.provider.ServiceProviderRegistration.Listener;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;
import org.wildfly.clustering.spi.group.Group;
import org.wildfly.common.function.ExceptionRunnable;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Infinispan {@link Cache} based {@link ServiceProviderRegistry}.
 * This factory can create multiple {@link ServiceProviderRegistration} instance,
 * all of which share the same {@link Cache} instance.
 * @author Paul Ferraro
 * @param <T> the service identifier type
 */
@org.infinispan.notifications.Listener
public class CacheServiceProviderRegistry<T> implements ServiceProviderRegistry<T>, AutoCloseable {

    private final ExecutorService topologyChangeExecutor = Executors.newSingleThreadExecutor(new DefaultThreadFactory(this.getClass()));
    private final Batcher<? extends Batch> batcher;
    private final ConcurrentMap<T, Map.Entry<Listener, ExecutorService>> listeners = new ConcurrentHashMap<>();
    private final Cache<T, Set<Address>> cache;
    private final Group<Address> group;
    private final Invoker invoker;
    private final CacheProperties properties;

    public CacheServiceProviderRegistry(CacheServiceProviderRegistryConfiguration<T> config) {
        this.group = config.getGroup();
        this.cache = config.getCache();
        this.batcher = config.getBatcher();
        this.cache.addListener(this);
        this.invoker = new RetryingInvoker(this.cache);
        this.properties = new InfinispanCacheProperties(this.cache.getCacheConfiguration());
    }

    @Override
    public void close() {
        this.cache.removeListener(this);
        this.shutdown(this.topologyChangeExecutor);
        // Cleanup any unclosed registrations
        for (Map.Entry<Listener, ExecutorService> entry : this.listeners.values()) {
            ExecutorService executor = entry.getValue();
            if (executor != null) {
                this.shutdown(executor);
            }
        }
        this.listeners.clear();
    }

    private void shutdown(ExecutorService executor) {
        WildFlySecurityManager.doUnchecked(executor, DefaultExecutorService.SHUTDOWN_ACTION);
        try {
            executor.awaitTermination(this.cache.getCacheConfiguration().transaction().cacheStopTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public org.wildfly.clustering.group.Group getGroup() {
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
                newEntry.setValue(new DefaultExecutorService(listener.getClass(), ExecutorServiceFactory.SINGLE_THREAD));
            }
            return newEntry;
        });
        if (entry != newEntry) {
            throw new IllegalArgumentException(service.toString());
        }
        this.invoker.invoke(new RegisterLocalServiceTask(service));
        return new SimpleServiceProviderRegistration<>(service, this, () -> {
            Address localAddress = this.group.getAddress(this.group.getLocalMember());
            try (Batch batch = this.batcher.createBatch()) {
                this.cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS, Flag.IGNORE_RETURN_VALUES).compute(service, this.properties.isTransactional() ? new CopyOnWriteAddressSetRemoveFunction(localAddress) : new ConcurrentAddressSetRemoveFunction(localAddress));
            } finally {
                Map.Entry<Listener, ExecutorService> oldEntry = this.listeners.remove(service);
                if (oldEntry != null) {
                    ExecutorService executor = oldEntry.getValue();
                    if (executor != null) {
                        this.shutdown(executor);
                    }
                }
            }
        });
    }

    void registerLocal(T service) {
        try (Batch batch = this.batcher.createBatch()) {
            this.register(this.group.getAddress(this.group.getLocalMember()), service);
        }
    }

    void register(Address address, T service) {
        this.cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS, Flag.IGNORE_RETURN_VALUES).compute(service, this.properties.isTransactional() ? new CopyOnWriteAddressSetAddFunction(address) : new ConcurrentAddressSetAddFunction(address));
    }

    @Override
    public Set<Node> getProviders(final T service) {
        Set<Address> addresses = this.cache.get(service);
        if (addresses == null) return Collections.emptySet();
        Set<Node> members = new TreeSet<>();
        for (Address address : addresses) {
            members.add(this.group.createNode(address));
        }
        return Collections.unmodifiableSet(members);
    }

    @Override
    public Set<T> getServices() {
        return this.cache.keySet();
    }

    @TopologyChanged
    public void topologyChanged(TopologyChangedEvent<T, Set<Address>> event) {
        if (!event.isPre()) {
            ConsistentHash previousHash = event.getWriteConsistentHashAtStart();
            List<Address> previousMembers = previousHash.getMembers();
            ConsistentHash hash = event.getWriteConsistentHashAtEnd();
            List<Address> members = hash.getMembers();

            if (!members.equals(previousMembers)) {
                Cache<T, Set<Address>> cache = event.getCache().getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS);
                Address localAddress = cache.getCacheManager().getAddress();

                // Determine which nodes have left the cache view
                Set<Address> leftMembers = new HashSet<>(previousMembers);
                leftMembers.removeAll(members);

                if (!leftMembers.isEmpty()) {
                    Locality locality = new ConsistentHashLocality(cache, hash);
                    // We're only interested in the entries for which we are the primary owner
                    Iterator<Address> addresses = leftMembers.iterator();
                    while (addresses.hasNext()) {
                        if (!locality.isLocal(addresses.next())) {
                            addresses.remove();
                        }
                    }
                }

                // If this is a merge after cluster split: Re-assert services for local member
                Set<T> localServices = !previousMembers.contains(localAddress) ? this.listeners.keySet() : Collections.emptySet();

                if (!leftMembers.isEmpty() || !localServices.isEmpty()) {
                    Batcher<? extends Batch> batcher = this.batcher;
                    Invoker invoker = this.invoker;
                    try {
                        this.topologyChangeExecutor.submit(new Runnable() {
                            @Override
                            public void run() {
                                if (!leftMembers.isEmpty()) {
                                    try (Batch batch = batcher.createBatch()) {
                                        try (CloseableIterator<Map.Entry<T, Set<Address>>> entries = cache.getAdvancedCache().withFlags(Flag.FORCE_WRITE_LOCK).entrySet().iterator()) {
                                            while (entries.hasNext()) {
                                                Map.Entry<T, Set<Address>> entry = entries.next();
                                                Set<Address> addresses = entry.getValue();
                                                if (addresses.removeAll(leftMembers)) {
                                                    entry.setValue(addresses);
                                                }
                                            }
                                        }
                                    }
                                }
                                if (!localServices.isEmpty()) {
                                    for (T localService : localServices) {
                                        invoker.invoke(new RegisterLocalServiceTask(localService));
                                    }
                                }
                            }
                        });
                    } catch (RejectedExecutionException e) {
                        // Executor is shutdown
                    }
                }
            }
        }
    }

    @CacheEntryCreated
    @CacheEntryModified
    public CompletionStage<Void> modified(CacheEntryEvent<T, Set<Address>> event) {
        if (!event.isPre()) {
            Map.Entry<Listener, ExecutorService> entry = this.listeners.get(event.getKey());
            if (entry != null) {
                Listener listener = entry.getKey();
                if (listener != null) {
                    ExecutorService executor = entry.getValue();
                    try {
                        executor.submit(() -> {
                            Set<Node> members = new TreeSet<>();
                            for (Address address : event.getValue()) {
                                members.add(this.group.createNode(address));
                            }
                            try {
                                listener.providersChanged(members);
                            } catch (Throwable e) {
                                ClusteringServerLogger.ROOT_LOGGER.serviceProviderRegistrationListenerFailed(e, this.cache.getCacheManager().getCacheManagerConfiguration().cacheManagerName(), this.cache.getName(), members);
                            }
                        });
                    } catch (RejectedExecutionException e) {
                        // Executor was shutdown
                    }
                }
            }
        }
        return CompletableFutures.completedNull();
    }

    private class RegisterLocalServiceTask implements ExceptionRunnable<CacheException> {
        private final T localService;

        RegisterLocalServiceTask(T localService) {
            this.localService = localService;
        }

        @Override
        public void run() {
            CacheServiceProviderRegistry.this.registerLocal(this.localService);
        }
    }
}
