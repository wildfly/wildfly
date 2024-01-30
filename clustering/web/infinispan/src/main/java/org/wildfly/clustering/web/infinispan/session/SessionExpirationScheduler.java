/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.wildfly.clustering.ee.infinispan.expiration.AbstractExpirationScheduler;
import org.wildfly.clustering.web.cache.session.metadata.ImmutableSessionMetaDataFactory;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * Session expiration scheduler that eagerly expires sessions as soon as they are eligible.
 * If/When Infinispan implements expiration notifications (ISPN-694), this will be obsolete.
 * @author Paul Ferraro
 * @param <MV> the meta data value type
 */
public class SessionExpirationScheduler<MV> extends AbstractExpirationScheduler<String> {

    private final ImmutableSessionMetaDataFactory<MV> metaDataFactory;

    public SessionExpirationScheduler(Batcher<TransactionBatch> batcher, ImmutableSessionMetaDataFactory<MV> metaDataFactory, Remover<String> remover, Duration closeTimeout) {
        super(new LocalScheduler<>(new SortedScheduledEntries<>(), new SessionRemoveTask(batcher, remover), closeTimeout));
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
