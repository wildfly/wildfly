/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan;

import java.io.IOException;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Marshalling;
import org.jboss.stdio.NullOutputStream;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.ejb.PassivationListener;
import org.wildfly.clustering.ejb.infinispan.bean.InfinispanBeanKey;
import org.wildfly.clustering.ejb.infinispan.group.InfinispanBeanGroup;
import org.wildfly.clustering.ejb.infinispan.group.InfinispanBeanGroupKey;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;

/**
 * Scheduler for eager eviction of a bean.
 * @author Paul Ferraro
 */
public class EagerEvictionScheduler<I, T> implements Scheduler<I>, BeanGroupEvictor<I>, Consumer<I> {

    private final Map<I, Future<?>> evictionFutures = new ConcurrentHashMap<>();

    private final Cache<BeanKey<I>, BeanEntry<I>> beanCache;
    private final ScheduledExecutorService executor;
    private final Duration idleTimeout;

    private final CommandDispatcher<BeanGroupEvictor<I>> dispatcher;

    public EagerEvictionScheduler(Cache<BeanKey<I>, BeanEntry<I>> beanCache, Cache<BeanGroupKey<I>, BeanGroupEntry<I, T>> groupCache, Predicate<Map.Entry<? super BeanKey<I>, ? super BeanEntry<I>>> beanFilter, MarshallingContext context, PassivationListener<T> passivationListener, ScheduledExecutorService executor, Duration idleTimeout, CommandDispatcherFactory dispatcherFactory, String dispatcherName) {
        this.executor = executor;
        this.idleTimeout = idleTimeout;
        this.beanCache = beanCache;
        BeanGroupEvictor<I> evictor = new BeanGroupEvictor<I>() {
            @Override
            public void evict(I id) {
                BeanKey<I> beanKey = new InfinispanBeanKey<>(id);
                BeanEntry<I> beanEntry = beanCache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD).get(beanKey);
                if ((beanEntry != null) && beanFilter.test(new AbstractMap.SimpleImmutableEntry<>(beanKey, beanEntry))) {
                    I groupId = beanEntry.getGroupId();
                    BeanGroupKey<I> groupKey = new InfinispanBeanGroupKey<>(groupId);
                    BeanGroupEntry<I, T> groupEntry = groupCache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD).get(groupKey);
                    if (groupEntry != null) {
                        try (BeanGroup<I, T> group = new InfinispanBeanGroup<>(groupId, groupEntry, context, Mutator.PASSIVE, new Remover<I>() {
                            @Override
                            public boolean remove(I id) {
                                return false;
                            }
                        })) {
                            // Verify that beans will serialize
                            try (Marshaller marshaller = context.createMarshaller(context.getCurrentVersion())) {
                                Collection<T> beans = groupEntry.getBeans().get(context).values();
                                for (I beanId : group.getBeans()) {
                                    group.prePassivate(beanId, passivationListener);
                                }
                                try {
                                    marshaller.start(Marshalling.createByteOutput(NullOutputStream.getInstance()));
                                    marshaller.writeObject(beans);
                                    marshaller.finish();

                                    // If bean group serializes successfully, perform passivation
                                    groupCache.getAdvancedCache().withFlags(Flag.SKIP_LISTENER_NOTIFICATION).evict(groupKey);
                                    // Cascade eviction to individual bean entries
                                    for (I beanId : group.getBeans()) {
                                        InfinispanEjbLogger.ROOT_LOGGER.tracef("Passivating bean %s", beanKey);
                                        // Cascade evict to bean entry
                                        beanCache.evict(new InfinispanBeanKey<>(beanId));
                                    }
                                } catch (Exception e) {
                                    // If beans failed to serialize, abort passivation
                                    InfinispanEjbLogger.ROOT_LOGGER.failedToPassivateBeanGroup(e, groupId);
                                    // Restore state of beans
                                    for (I beanId : group.getBeans()) {
                                        group.postActivate(beanId, passivationListener);
                                    }
                                }
                            } catch (IOException | ClassNotFoundException e) {
                                InfinispanEjbLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                            }
                        }
                    }
                }
            }
        };
        this.dispatcher = dispatcherFactory.createCommandDispatcher(dispatcherName + "/eviction", evictor);
    }

    @Override
    public void accept(I id) {
        this.evictionFutures.remove(id);
    }

    @Override
    public void schedule(I id) {
        BeanEntry<I> entry = this.beanCache.get(new InfinispanBeanKey<>(id));
        if (entry != null) {
            InfinispanEjbLogger.ROOT_LOGGER.tracef("Scheduling stateful session bean %s to passivate in %s", id, this.idleTimeout);
            Runnable task = new EvictionTask<>(this, id, entry.getGroupId(), this);
            // Make sure the map insertion happens before map removal (during task execution).
            synchronized (task) {
                this.evictionFutures.put(id, this.executor.schedule(task, this.idleTimeout.toMillis(), TimeUnit.MILLISECONDS));
            }
        }
    }

    @Override
    public void cancel(I id) {
        Future<?> future = this.evictionFutures.remove(id);
        if (future != null) {
            future.cancel(false);
        }
    }

    @Override
    public void cancel(Locality locality) {
        for (I id: this.evictionFutures.keySet()) {
            if (Thread.currentThread().isInterrupted()) break;
            if (!locality.isLocal(id)) {
                this.cancel(id);
            }
        }
    }

    @Override
    public void close() {
        for (Future<?> future: this.evictionFutures.values()) {
            future.cancel(false);
        }
        for (Future<?> future: this.evictionFutures.values()) {
            if (!future.isDone()) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    // Ignore
                }
            }
        }
        this.evictionFutures.clear();
        this.dispatcher.close();
    }

    @Override
    public void evict(I id) {
        try {
            // Cache eviction is a local operation, so we need to broadcast this to the cluster
            this.dispatcher.executeOnGroup(new EvictCommand<>(id));
        } catch (CommandDispatcherException e) {
            InfinispanEjbLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }

    static class EvictCommand<I> implements Command<Void, BeanGroupEvictor<I>> {
        private static final long serialVersionUID = -7382608648983713382L;

        private final I id;

        EvictCommand(I id) {
            this.id = id;
        }

        @Override
        public Void execute(BeanGroupEvictor<I> evictor) throws Exception {
            InfinispanEjbLogger.ROOT_LOGGER.tracef("Passivating stateful session bean %s", this.id);
            evictor.evict(this.id);
            return null;
        }
    }

    private static class EvictionTask<I> implements Runnable {
        private final BeanGroupEvictor<I> evictor;
        private final I beanId;
        private final I groupId;
        private final Consumer<I> finalizer;

        EvictionTask(BeanGroupEvictor<I> evictor, I beanId, I groupId, Consumer<I> finalizer) {
            this.evictor = evictor;
            this.beanId = beanId;
            this.groupId = groupId;
            this.finalizer = finalizer;
        }

        @Override
        public void run() {
            try {
                this.evictor.evict(this.groupId);
            } finally {
                synchronized (this) {
                    this.finalizer.accept(this.beanId);
                }
            }
        }
    }
}
