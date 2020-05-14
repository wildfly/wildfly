/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
import java.util.function.Predicate;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.cache.scheduler.LocalScheduler;
import org.wildfly.clustering.ee.cache.scheduler.ScheduledEntries;
import org.wildfly.clustering.ee.cache.scheduler.LinkedScheduledEntries;
import org.wildfly.clustering.ee.cache.scheduler.SortedScheduledEntries;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.infinispan.scheduler.Scheduler;
import org.wildfly.clustering.ejb.infinispan.bean.InfinispanBeanKey;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;

/**
 * Schedules a bean for expiration.
 *
 * @author Paul Ferraro
 *
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public class BeanExpirationScheduler<I, T> implements Scheduler<I, ImmutableBeanEntry<I>>, Predicate<I> {

    private final LocalScheduler<I> scheduler;
    private final Batcher<TransactionBatch> batcher;
    private final BeanFactory<I, T> factory;
    private final ExpirationConfiguration<T> expiration;
    private final BeanRemover<I, T> remover;

    public BeanExpirationScheduler(Group group, Batcher<TransactionBatch> batcher, BeanFactory<I, T> factory, ExpirationConfiguration<T> expiration, BeanRemover<I, T> remover, Duration closeTimeout) {
        ScheduledEntries<I, Instant> entries = group.isSingleton() ? new LinkedScheduledEntries<>() : new SortedScheduledEntries<>();
        this.scheduler = new LocalScheduler<>(entries, this, closeTimeout);
        this.batcher = batcher;
        this.factory = factory;
        this.expiration = expiration;
        this.remover = remover;
    }

    @Override
    public void schedule(I id) {
        BeanEntry<I> entry = this.factory.findValue(id);
        if (entry != null) {
            this.schedule(id, entry);
        }
    }

    @Override
    public void schedule(I id, ImmutableBeanEntry<I> entry) {
        Duration timeout = this.expiration.getTimeout();
        if (!timeout.isNegative()) {
            InfinispanEjbLogger.ROOT_LOGGER.tracef("Scheduling stateful session bean %s to expire in %s", id, timeout);
            this.scheduler.schedule(id, entry.getLastAccessedTime().plus(timeout));
        }
    }

    @Override
    public void cancel(I id) {
        this.scheduler.cancel(id);
    }

    @Override
    public void cancel(Locality locality) {
        for (I id: this.scheduler) {
            if (Thread.currentThread().isInterrupted()) break;
            if (!locality.isLocal(new InfinispanBeanKey<>(id))) {
                this.scheduler.cancel(id);
            }
        }
    }

    @Override
    public void close() {
        this.scheduler.close();
    }

    @Override
    public boolean test(I id) {
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Expiring stateful session bean %s", id);
        try (Batch batch = this.batcher.createBatch()) {
            try {
                this.remover.remove(id, this.expiration.getRemoveListener());
                return true;
            } catch (RuntimeException e) {
                batch.discard();
                throw e;
            }
        } catch (RuntimeException e) {
            InfinispanEjbLogger.ROOT_LOGGER.failedToExpireBean(e, id);
            return false;
        }
    }
}
