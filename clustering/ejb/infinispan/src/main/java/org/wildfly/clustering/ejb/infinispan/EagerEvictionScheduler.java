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

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.cache.scheduler.LinkedScheduledEntries;
import org.wildfly.clustering.ee.cache.scheduler.LocalScheduler;
import org.wildfly.clustering.ee.cache.scheduler.SortedScheduledEntries;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.infinispan.scheduler.Scheduler;
import org.wildfly.clustering.ejb.infinispan.bean.InfinispanBeanKey;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;
import org.wildfly.clustering.spi.dispatcher.CommandDispatcherFactory;

/**
 * Scheduler for eager eviction of a bean.
 * @author Paul Ferraro
 */
public class EagerEvictionScheduler<I, T> implements Scheduler<I, ImmutableBeanEntry<I>>, Predicate<I> {

    private final LocalScheduler<I> scheduler;
    private final Batcher<TransactionBatch> batcher;
    private final Map<I, I> beanGroups = new ConcurrentHashMap<>();
    private final BeanFactory<I, T> factory;
    private final Duration idleTimeout;

    private final CommandDispatcher<BeanGroupEvictor<I>> dispatcher;

    public EagerEvictionScheduler(Group group, Batcher<TransactionBatch> batcher, BeanFactory<I, T> factory, BeanGroupEvictor<I> evictor, Duration idleTimeout, CommandDispatcherFactory dispatcherFactory, String dispatcherName, Duration closeTimeout) {
        this.scheduler = new LocalScheduler<>(group.isSingleton() ? new LinkedScheduledEntries<>() : new SortedScheduledEntries<>(), this, closeTimeout);
        this.batcher = batcher;
        this.factory = factory;
        this.idleTimeout = idleTimeout;
        this.dispatcher = dispatcherFactory.createCommandDispatcher(dispatcherName + "/eviction", evictor, this.getClass().getClassLoader());
    }

    @Override
    public void schedule(I id) {
        try (Batch batch = this.batcher.createBatch()) {
            BeanEntry<I> entry = this.factory.findValue(id);
            if (entry != null) {
                this.schedule(id, entry);
            }
        }
    }

    @Override
    public void schedule(I id, ImmutableBeanEntry<I> entry) {
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Scheduling stateful session bean %s to passivate in %s", id, this.idleTimeout);
        this.beanGroups.put(id, entry.getGroupId());
        this.scheduler.schedule(id, Instant.now().plus(this.idleTimeout));
    }

    @Override
    public void cancel(I id) {
        this.scheduler.cancel(id);
        this.beanGroups.remove(id);
    }

    @Override
    public void cancel(Locality locality) {
        for (I id: this.scheduler) {
            if (Thread.currentThread().isInterrupted()) break;
            if (!locality.isLocal(new InfinispanBeanKey<>(id))) {
                this.cancel(id);
            }
        }
    }

    @Override
    public void close() {
        this.scheduler.close();
        this.dispatcher.close();
    }

    @Override
    public boolean test(I id) {
        InfinispanEjbLogger.ROOT_LOGGER.debugf("Evicting stateful session bean %s", id);
        try {
            // Cache eviction is a local operation, so we need to broadcast this to the cluster
            this.dispatcher.executeOnGroup(new EvictCommand<>(id));
            this.beanGroups.remove(id);
            return true;
        } catch (CommandDispatcherException e) {
            InfinispanEjbLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
            return false;
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
}
