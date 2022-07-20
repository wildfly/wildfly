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
import org.wildfly.clustering.ee.infinispan.scheduler.AbstractCacheEntryScheduler;
import org.wildfly.clustering.web.cache.session.ImmutableSessionMetaDataFactory;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.SessionExpirationMetaData;

/**
 * Session expiration scheduler that eagerly expires sessions as soon as they are eligible.
 * If/When Infinispan implements expiration notifications (ISPN-694), this will be obsolete.
 * @author Paul Ferraro
 */
public class SessionExpirationScheduler<MV> extends AbstractCacheEntryScheduler<String, SessionExpirationMetaData> {

    private final ImmutableSessionMetaDataFactory<MV> metaDataFactory;

    public SessionExpirationScheduler(Batcher<TransactionBatch> batcher, ImmutableSessionMetaDataFactory<MV> metaDataFactory, Remover<String> remover, Duration closeTimeout) {
        super(new LocalScheduler<>(new SortedScheduledEntries<>(), new SessionRemoveTask(batcher, remover), closeTimeout), SessionExpirationMetaData::getMaxInactiveInterval, Duration::isZero, SessionExpirationMetaData::getLastAccessEndTime);
        this.metaDataFactory = metaDataFactory;
    }

    @Override
    public void schedule(String sessionId) {
        MV value = this.metaDataFactory.findValue(sessionId);
        if (value != null) {
            ImmutableSessionMetaData metaData = this.metaDataFactory.createImmutableSessionMetaData(sessionId, value);
            this.schedule(sessionId, metaData);
        }
    }

    private static class SessionRemoveTask implements Predicate<String> {
        private final Batcher<TransactionBatch> batcher;
        private final Remover<String> remover;

        SessionRemoveTask(Batcher<TransactionBatch> batcher, Remover<String> remover) {
            this.batcher = batcher;
            this.remover = remover;
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
}
