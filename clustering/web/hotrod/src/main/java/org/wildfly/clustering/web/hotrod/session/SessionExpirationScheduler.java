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
package org.wildfly.clustering.web.hotrod.session;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.cache.scheduler.LocalScheduler;
import org.wildfly.clustering.ee.cache.scheduler.SortedScheduledEntries;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.web.hotrod.logging.Logger;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * Session expiration scheduler that eagerly expires sessions as soon as they are eligible.
 * If/When Infinispan implements expiration notifications (ISPN-694), this will be obsolete.
 * @author Paul Ferraro
 */
public class SessionExpirationScheduler implements Scheduler<String, ImmutableSessionMetaData>, Predicate<String> {

    private final Scheduler<String, Instant> scheduler;
    private final Batcher<TransactionBatch> batcher;
    private final Remover<String> remover;

    public SessionExpirationScheduler(Batcher<TransactionBatch> batcher, Remover<String> remover, Duration closeTimeout) {
        this.scheduler = new LocalScheduler<>(new SortedScheduledEntries<>(), this, closeTimeout);
        this.batcher = batcher;
        this.remover = remover;
    }

    @Override
    public void cancel(String sessionId) {
        this.scheduler.cancel(sessionId);
    }

    @Override
    public void schedule(String sessionId, ImmutableSessionMetaData metaData) {
        Duration maxInactiveInterval = metaData.getMaxInactiveInterval();
        if (!maxInactiveInterval.isZero()) {
            Logger.ROOT_LOGGER.tracef("Session %s will expire in %s", sessionId, maxInactiveInterval);
            this.scheduler.schedule(sessionId, metaData.getLastAccessedTime().plus(maxInactiveInterval));
        }
    }

    @Override
    public void close() {
        this.scheduler.close();
    }

    @Override
    public boolean test(String sessionId) {
        Logger.ROOT_LOGGER.debugf("Expiring web session %s", sessionId);
        try (Batch batch = this.batcher.createBatch()) {
            try {
                this.remover.remove(sessionId);
                return true;
            } catch (RuntimeException e) {
                batch.discard();
                throw e;
            }
        } catch (RuntimeException e) {
            Logger.ROOT_LOGGER.failedToExpireSession(e, sessionId);
            return false;
        }
    }
}
