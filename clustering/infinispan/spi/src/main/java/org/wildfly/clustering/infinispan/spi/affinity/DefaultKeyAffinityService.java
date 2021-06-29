/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi.affinity;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.distribution.ch.KeyPartitioner;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.concurrent.CompletableFutures;
import org.jboss.as.clustering.context.DefaultExecutorService;
import org.jboss.as.clustering.context.DefaultThreadFactory;
import org.jboss.logging.Logger;
import org.wildfly.clustering.infinispan.spi.distribution.ConsistentHashKeyDistribution;
import org.wildfly.clustering.infinispan.spi.distribution.KeyDistribution;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A custom key affinity service implementation with the following distinct characteristics (as compared to {@link org.infinispan.affinity.impl.KeyAffinityServiceImpl}):
 * <ul>
 * <li>{@link #getKeyForAddress(Address)} will return a random key (instead of throwing an ISE) if the specified address does not own any segments.</li>
 * <li>Uses a worker thread per address for which to generate keys.</li>
 * <li>Minimal CPU utilization when key queues are full.</li>
 * <li>Non-blocking topology change event handler.</li>
 * <li>{@link #getKeyForAddress(Address)} calls will block during topology change events.</li>
 * </ul>
 * @author Paul Ferraro
 */
@Listener
public class DefaultKeyAffinityService<K> implements KeyAffinityService<K>, Supplier<BlockingQueue<K>> {

    private static final Logger LOGGER = Logger.getLogger(DefaultKeyAffinityService.class);

    private final Cache<? extends K, ?> cache;
    private final KeyGenerator<? extends K> generator;
    private final AtomicReference<KeyAffinityState<K>> currentState = new AtomicReference<>();
    private final KeyPartitioner partitioner;
    private final Predicate<Address> filter;

    private volatile int queueSize = 100;
    private volatile Duration timeout = Duration.ofMillis(100L);
    private volatile ExecutorService executor;

    private interface KeyAffinityState<K> {
        KeyDistribution getDistribution();
        KeyRegistry<K> getRegistry();
        Iterable<Future<?>> getFutures();
    }

    /**
     * Constructs a key affinity service that generates keys hashing to the members matching the specified filter.
     * @param cache the target cache
     * @param generator a key generator
     */
    @SuppressWarnings("deprecation")
    public DefaultKeyAffinityService(Cache<? extends K, ?> cache, KeyGenerator<? extends K> generator, Predicate<Address> filter) {
        this(cache, cache.getAdvancedCache().getComponentRegistry().getLocalComponent(KeyPartitioner.class), generator, filter);
    }

    DefaultKeyAffinityService(Cache<? extends K, ?> cache, KeyPartitioner partitioner, KeyGenerator<? extends K> generator, Predicate<Address> filter) {
        this.cache = cache;
        this.partitioner = partitioner;
        this.generator = generator;
        this.filter = filter;
    }

    /**
     * Overrides the maximum number of keys with affinity to a given member to pre-generate.
     * @param size a queue size threshold
     */
    public void setQueueSize(int size) {
        this.queueSize = size;
    }

    /**
     * Overrides the duration of time for which calls to {@link #getKeyForAddress(Address)} will wait for an available pre-generated key,
     * after which a random key will be returned.
     * @param timeout a queue poll timeout
     */
    public void setPollTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    @Override
    public BlockingQueue<K> get() {
        return new ArrayBlockingQueue<>(this.queueSize);
    }

    @Override
    public boolean isStarted() {
        ExecutorService executor = this.executor;
        return (executor != null) && !executor.isShutdown();
    }

    @Override
    public void start() {
        this.executor = Executors.newCachedThreadPool(new DefaultThreadFactory(this.getClass()));
        this.accept(this.cache.getAdvancedCache().getDistributionManager().getCacheTopology().getWriteConsistentHash());
        this.cache.addListener(this);
    }

    @Override
    public void stop() {
        this.cache.removeListener(this);
        WildFlySecurityManager.doUnchecked(this.executor, DefaultExecutorService.SHUTDOWN_NOW_ACTION);
    }

    @Override
    public K getCollocatedKey(K otherKey) {
        KeyAffinityState<K> currentState = this.currentState.get();
        if (currentState == null) {
            // Not yet started!
            throw new IllegalStateException();
        }
        return this.getCollocatedKey(currentState, otherKey);
    }

    private K getCollocatedKey(KeyAffinityState<K> state, K otherKey) {
        K key = this.poll(state.getRegistry(), state.getDistribution().getPrimaryOwner(otherKey));
        if (key != null) {
            return key;
        }
        KeyAffinityState<K> currentState = this.currentState.get();
        // If state is out-dated, retry
        if (state != currentState) {
            return this.getCollocatedKey(currentState, otherKey);
        }
        LOGGER.debugf("Could not obtain pre-generated key with same affinity as %s -- generating random key", otherKey);
        return this.generator.getKey();
    }

    @Override
    public K getKeyForAddress(Address address) {
        if (!this.filter.test(address)) {
            throw new IllegalArgumentException(address.toString());
        }
        KeyAffinityState<K> currentState = this.currentState.get();
        if (currentState == null) {
            // Not yet started!
            throw new IllegalStateException();
        }
        return this.getKeyForAddress(currentState, address);
    }

    private K getKeyForAddress(KeyAffinityState<K> state, Address address) {
        K key = this.poll(state.getRegistry(), address);
        if (key != null) {
            return key;
        }
        KeyAffinityState<K> currentState = this.currentState.get();
        // If state is out-dated, retry
        if (state != currentState) {
            return this.getKeyForAddress(currentState, address);
        }
        LOGGER.debugf("Could not obtain pre-generated key with affinity for %s -- generating random key", address);
        return this.generator.getKey();
    }

    private K poll(KeyRegistry<K> registry, Address address) {
        BlockingQueue<K> keys = registry.getKeys(address);
        if (keys != null) {
            Duration timeout = this.timeout;
            long nanos = (timeout.getSeconds() == 0) ? timeout.getNano() : timeout.toNanos();
            try {
                return keys.poll(nanos, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }

    @TopologyChanged
    public CompletionStage<Void> viewChanged(TopologyChangedEvent<?, ?> event) {
        if (!event.isPre() && !this.getSegments(event.getWriteConsistentHashAtStart()).equals(this.getSegments(event.getWriteConsistentHashAtEnd()))) {
            LOGGER.debugf("Restarting key generation based on new consistent hash for topology %d", event.getNewTopologyId());
            this.accept(event.getWriteConsistentHashAtEnd());
        }
        return CompletableFutures.completedNull();
    }

    private Map<Address, Set<Integer>> getSegments(ConsistentHash hash) {
        Map<Address, Set<Integer>> segments = new TreeMap<>();
        for (Address address : hash.getMembers()) {
            if (this.filter.test(address)) {
                segments.put(address, hash.getPrimarySegmentsForOwner(address));
            }
        }
        return segments;
    }

    private void accept(ConsistentHash hash) {
        KeyDistribution distribution = new ConsistentHashKeyDistribution(this.partitioner, hash);
        KeyRegistry<K> registry = new ConsistentHashKeyRegistry<>(hash, this.filter, this);
        Set<Address> addresses = registry.getAddresses();
        List<Future<?>> futures = !addresses.isEmpty() ? new ArrayList<>(addresses.size()) : Collections.emptyList();
        try {
            for (Address address : addresses) {
                BlockingQueue<K> keys = registry.getKeys(address);
                futures.add(this.executor.submit(new GenerateKeysTask<>(this.generator, distribution, address, keys)));
            }
            KeyAffinityState<K> previousState = this.currentState.getAndSet(new KeyAffinityState<K>() {
                @Override
                public KeyDistribution getDistribution() {
                    return distribution;
                }

                @Override
                public KeyRegistry<K> getRegistry() {
                    return registry;
                }

                @Override
                public Iterable<Future<?>> getFutures() {
                    return futures;
                }
            });
            if (previousState != null) {
                for (Future<?> future : previousState.getFutures()) {
                    future.cancel(true);
                }
            }
        } catch (RejectedExecutionException e) {
            // Executor was shutdown. Cancel any tasks that were already submitted
            for (Future<?> future : futures) {
                future.cancel(true);
            }
        }
    }

    private static class GenerateKeysTask<K> implements Runnable {
        private final KeyGenerator<? extends K> generator;
        private final KeyDistribution distribution;
        private final Address address;
        private final BlockingQueue<K> keys;

        GenerateKeysTask(KeyGenerator<? extends K> generator, KeyDistribution distribution, Address address, BlockingQueue<K> keys) {
            this.generator = generator;
            this.distribution = distribution;
            this.address = address;
            this.keys = keys;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                K key = this.generator.getKey();
                if (this.distribution.getPrimaryOwner(key).equals(this.address)) {
                    try {
                        this.keys.put(key);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
}
