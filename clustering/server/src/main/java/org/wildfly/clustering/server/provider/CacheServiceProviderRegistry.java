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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.context.Flag;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.context.DefaultExecutorService;
import org.jboss.as.clustering.context.ExecutorServiceFactory;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Invoker;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.cache.function.ConcurrentSetAddFunction;
import org.wildfly.clustering.ee.cache.function.ConcurrentSetRemoveFunction;
import org.wildfly.clustering.ee.cache.function.CopyOnWriteSetAddFunction;
import org.wildfly.clustering.ee.cache.function.CopyOnWriteSetRemoveFunction;
import org.wildfly.clustering.ee.infinispan.InfinispanCacheProperties;
import org.wildfly.clustering.ee.infinispan.retry.RetryingInvoker;
import org.wildfly.clustering.group.GroupListener;
import org.wildfly.clustering.group.Membership;
import org.wildfly.clustering.group.Node;
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
 */
@org.infinispan.notifications.Listener
public class CacheServiceProviderRegistry<T> implements ServiceProviderRegistry<T>, GroupListener, AutoCloseable {

    final Batcher<? extends Batch> batcher;
    private final ConcurrentMap<T, Map.Entry<Listener, ExecutorService>> listeners = new ConcurrentHashMap<>();
    private final Cache<T, Set<Address>> cache;
    private final Group<Address> group;
    private final Registration groupRegistration;
    private final CommandDispatcher<Set<T>> dispatcher;
    private final Invoker invoker;
    private final CacheProperties properties;

    public CacheServiceProviderRegistry(CacheServiceProviderRegistryConfiguration<T> config) {
        this.group = config.getGroup();
        this.cache = config.getCache();
        this.batcher = config.getBatcher();
        this.dispatcher = config.getCommandDispatcherFactory().createCommandDispatcher(config.getId(), this.listeners.keySet(), WildFlySecurityManager.getClassLoaderPrivileged(this.getClass()));
        this.cache.addListener(this);
        this.groupRegistration = this.group.register(this);
        this.invoker = new RetryingInvoker(this.cache);
        this.properties = new InfinispanCacheProperties(this.cache.getCacheConfiguration());
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
                this.shutdown(executor);
            }
        }
        this.listeners.clear();
    }

    private void shutdown(ExecutorService executor) {
        WildFlySecurityManager.doUnchecked(executor, DefaultExecutorService.SHUTDOWN_NOW_ACTION);
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
        ExceptionRunnable<CacheException> registerAction = new ExceptionRunnable<CacheException>() {
            @Override
            public void run() throws CacheException {
                CacheServiceProviderRegistry.this.registerLocal(service);
            }
        };
        this.invoker.invoke(registerAction);
        return new SimpleServiceProviderRegistration<>(service, this, () -> {
            Address localAddress = this.group.getAddress(this.group.getLocalMember());
            try (Batch batch = this.batcher.createBatch()) {
                this.cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS, Flag.IGNORE_RETURN_VALUES).compute(service, this.properties.isTransactional() ? new CopyOnWriteSetRemoveFunction<>(localAddress) : new ConcurrentSetRemoveFunction<>(localAddress));
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
        this.cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS, Flag.IGNORE_RETURN_VALUES).compute(service, this.properties.isTransactional() ? new CopyOnWriteSetAddFunction<>(address) : new ConcurrentSetAddFunction<>(address));
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

    @Override
    public void membershipChanged(Membership previousMembership, Membership membership, final boolean merged) {
        if (membership.isCoordinator()) {
            Set<Node> previousMembers = new HashSet<>(previousMembership.getMembers());
            Set<Node> members = new HashSet<>(membership.getMembers());
            List<Address> leftMembers = new ArrayList<>(previousMembers.size());
            for (Node previousMember : previousMembers) {
                if (!members.contains(previousMember)) {
                    leftMembers.add(this.group.getAddress(previousMember));
                }
            }
            List<Address> joinedMembers = new ArrayList<>(members.size());
            for (Node member : members) {
                if (!previousMembers.contains(member)) {
                    joinedMembers.add(this.group.getAddress(member));
                }
            }
            if (!leftMembers.isEmpty()) {
                try (Batch batch = this.batcher.createBatch()) {
                    try (CloseableIterator<Map.Entry<T, Set<Address>>> entries = this.cache.entrySet().iterator()) {
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
            if (merged) {
                // Re-assert services for new members following merge since these may have been lost following split
                Command<Collection<T>, Set<T>> command = new GetLocalServicesCommand<>();
                for (Address joinedMember : joinedMembers) {
                    BiConsumer<Collection<T>, Throwable> completionHandler = new BiConsumer<Collection<T>, Throwable>() {
                        @Override
                        public void accept(Collection<T> services, Throwable exception) {
                            if (services != null) {
                                try (Batch batch = CacheServiceProviderRegistry.this.batcher.createBatch()) {
                                    for (T service : services) {
                                        CacheServiceProviderRegistry.this.register(joinedMember, service);
                                    }
                                }
                            } else if (exception != null) {
                                ClusteringServerLogger.ROOT_LOGGER.warn(exception.getLocalizedMessage(), exception);
                            }
                        }
                    };
                    try {
                        this.dispatcher.executeOnMember(command, this.group.createNode(joinedMember)).whenComplete(completionHandler);
                    } catch (CommandDispatcherException e) {
                        ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                    }
                }
            }
        }
    }

    @CacheEntryCreated
    @CacheEntryModified
    public void modified(CacheEntryEvent<T, Set<Address>> event) {
        if (event.isPre()) return;
        Map.Entry<Listener, ExecutorService> entry = this.listeners.get(event.getKey());
        if (entry != null) {
            Listener listener = entry.getKey();
            if (listener != null) {
                ExecutorService executor = entry.getValue();
                Set<Node> members = new TreeSet<>();
                for (Address address : event.getValue()) {
                    members.add(this.group.createNode(address));
                }
                try {
                    executor.submit(() -> {
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
}
