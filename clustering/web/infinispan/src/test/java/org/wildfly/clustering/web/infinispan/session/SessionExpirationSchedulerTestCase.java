/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.infinispan.session;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.expiration.ExpirationMetaData;
import org.wildfly.clustering.web.cache.session.metadata.ImmutableSessionMetaDataFactory;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * Unit test for {@link SessionExpirationScheduler}.
 *
 * @author Paul Ferraro
 */
public class SessionExpirationSchedulerTestCase {
    @Test
    public void test() throws InterruptedException {
        Batcher<TransactionBatch> batcher = mock(Batcher.class);
        TransactionBatch batch = mock(TransactionBatch.class);
        Remover<String> remover = mock(Remover.class);
        ImmutableSessionMetaDataFactory<Object> metaDataFactory = mock(ImmutableSessionMetaDataFactory.class);
        ImmutableSessionMetaData immortalSessionMetaData = mock(ImmutableSessionMetaData.class);
        ImmutableSessionMetaData expiringSessionMetaData = mock(ImmutableSessionMetaData.class);
        ImmutableSessionMetaData canceledSessionMetaData = mock(ImmutableSessionMetaData.class);
        String immortalSessionId = "immortal";
        String expiringSessionId = "expiring";
        String canceledSessionId = "canceled";

        when(batcher.createBatch()).thenReturn(batch);

        when(immortalSessionMetaData.isImmortal()).thenReturn(true);
        when(expiringSessionMetaData.isImmortal()).thenReturn(false);
        when(canceledSessionMetaData.isImmortal()).thenReturn(false);
        when(expiringSessionMetaData.getTimeout()).thenReturn(Duration.ofMillis(1L));
        when(canceledSessionMetaData.getTimeout()).thenReturn(Duration.ofSeconds(100L));

        Instant now = Instant.now();
        doCallRealMethod().when(expiringSessionMetaData).getLastAccessTime();
        doReturn(now).when(expiringSessionMetaData).getLastAccessEndTime();
        doCallRealMethod().when(canceledSessionMetaData).getLastAccessTime();
        doReturn(now).when(canceledSessionMetaData).getLastAccessEndTime();
        when(remover.remove(expiringSessionId)).thenReturn(true);

        try (Scheduler<String, ExpirationMetaData> scheduler = new SessionExpirationScheduler<>(batcher, metaDataFactory, remover, Duration.ZERO)) {
            scheduler.schedule(immortalSessionId, immortalSessionMetaData);
            scheduler.schedule(canceledSessionId, canceledSessionMetaData);
            scheduler.schedule(expiringSessionId, expiringSessionMetaData);

            scheduler.cancel(canceledSessionId);

            TimeUnit.MILLISECONDS.sleep(500);
        }

        verify(remover, never()).remove(immortalSessionId);
        verify(remover, never()).remove(canceledSessionId);
        verify(batch).close();
    }

}
