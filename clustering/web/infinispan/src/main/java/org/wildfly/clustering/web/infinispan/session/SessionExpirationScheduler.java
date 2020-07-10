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
package org.wildfly.clustering.web.infinispan.session;

import java.time.Duration;
import java.util.function.Predicate;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.ee.cache.scheduler.LocalScheduler;
import org.wildfly.clustering.ee.cache.scheduler.SortedScheduledEntries;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.infinispan.GroupedKey;
import org.wildfly.clustering.ee.infinispan.scheduler.Scheduler;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;
import org.wildfly.clustering.web.cache.session.ImmutableSessionMetaDataFactory;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * Session expiration scheduler that eagerly expires sessions as soon as they are eligible.
 * If/When Infinispan implements expiration notifications (ISPN-694), this will be obsolete.
 * @author Paul Ferraro
 */
public class SessionExpirationScheduler<MV> implements Scheduler<String, ImmutableSessionMetaData>, Predicate<String> {

    private final LocalScheduler<String> scheduler;
    private final Batcher<TransactionBatch> batcher;
    private final Remover<String> remover;
    private final ImmutableSessionMetaDataFactory<MV> metaDataFactory;

    public SessionExpirationScheduler(Batcher<TransactionBatch> batcher, ImmutableSessionMetaDataFactory<MV> metaDataFactory, Remover<String> remover, Duration closeTimeout) {
        this.scheduler = new LocalScheduler<>(new SortedScheduledEntries<>(), this, closeTimeout);
        this.batcher = batcher;
        this.metaDataFactory = metaDataFactory;
        this.remover = remover;
    }

    @Override
    public void schedule(String sessionId) {
        MV value = this.metaDataFactory.findValue(sessionId);
        if (value != null) {
            ImmutableSessionMetaData metaData = this.metaDataFactory.createImmutableSessionMetaData(sessionId, value);
            this.schedule(sessionId, metaData);
        }
    }

    @Override
    public void schedule(String sessionId, ImmutableSessionMetaData metaData) {
        Duration maxInactiveInterval = metaData.getMaxInactiveInterval();
        if (!maxInactiveInterval.isZero()) {
            this.scheduler.schedule(sessionId, metaData.getLastAccessedTime().plus(maxInactiveInterval));
        }
    }

    @Override
    public void cancel(String sessionId) {
        this.scheduler.cancel(sessionId);
    }

    @Override
    public void cancel(Locality locality) {
        for (String sessionId : this.scheduler) {
            if (Thread.currentThread().isInterrupted()) break;
            if (!locality.isLocal(new GroupedKey<>(sessionId))) {
                this.cancel(sessionId);
            }
        }
    }

    @Override
    public void close() {
        this.scheduler.close();
    }

    @Override
    public boolean test(String sessionId) {
        InfinispanWebLogger.ROOT_LOGGER.debugf("Expiring web session %s", sessionId);
        try (Batch batch = this.batcher.createBatch()) {
            try {
                this.remover.remove(sessionId);
                return true;
            } catch (RuntimeException e) {
                batch.discard();
                throw e;
            }
        } catch (RuntimeException e) {
            InfinispanWebLogger.ROOT_LOGGER.failedToExpireSession(e, sessionId);
            return false;
        }
    }
}
