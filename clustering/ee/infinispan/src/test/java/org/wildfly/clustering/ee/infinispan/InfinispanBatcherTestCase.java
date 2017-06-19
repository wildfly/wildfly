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
package org.wildfly.clustering.ee.infinispan;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.BatchContext;
import org.wildfly.clustering.ee.Batcher;

/**
 * Unit test for {@link InfinispanBatcher}.
 * @author Paul Ferraro
 */
public class InfinispanBatcherTestCase {
    private final TransactionManager tm = mock(TransactionManager.class);
    private final Batcher<TransactionBatch> batcher = new InfinispanBatcher(this.tm);

    @After
    public void destroy() {
        InfinispanBatcher.setCurrentBatch(null);
    }

    @Test
    public void createExistingActiveBatch() throws Exception {
        TransactionBatch existingBatch = mock(TransactionBatch.class);

        InfinispanBatcher.setCurrentBatch(existingBatch);
        when(existingBatch.getState()).thenReturn(Batch.State.ACTIVE);
        when(existingBatch.interpose()).thenReturn(existingBatch);

        TransactionBatch result = this.batcher.createBatch();

        verify(existingBatch).interpose();
        verifyZeroInteractions(this.tm);

        assertSame(existingBatch, result);
    }

    @Test
    public void createExistingClosedBatch() throws Exception {
        TransactionBatch existingBatch = mock(TransactionBatch.class);
        Transaction tx = mock(Transaction.class);
        ArgumentCaptor<Synchronization> capturedSync = ArgumentCaptor.forClass(Synchronization.class);

        InfinispanBatcher.setCurrentBatch(existingBatch);
        when(existingBatch.getState()).thenReturn(Batch.State.CLOSED);

        when(this.tm.getTransaction()).thenReturn(tx);

        try (TransactionBatch batch = this.batcher.createBatch()) {
            verify(this.tm).begin();
            verify(tx).registerSynchronization(capturedSync.capture());

            assertSame(tx, batch.getTransaction());
            assertSame(batch, InfinispanBatcher.getCurrentBatch());
        } finally {
            capturedSync.getValue().afterCompletion(Status.STATUS_COMMITTED);
        }

        verify(tx).commit();

        assertNull(InfinispanBatcher.getCurrentBatch());
    }


    @Test
    public void createBatchClose() throws Exception {
        Transaction tx = mock(Transaction.class);
        ArgumentCaptor<Synchronization> capturedSync = ArgumentCaptor.forClass(Synchronization.class);

        when(this.tm.getTransaction()).thenReturn(tx);

        try (TransactionBatch batch = this.batcher.createBatch()) {
            verify(this.tm).begin();
            verify(tx).registerSynchronization(capturedSync.capture());

            assertSame(tx, batch.getTransaction());
        } finally {
            capturedSync.getValue().afterCompletion(Status.STATUS_COMMITTED);
        }

        verify(tx).commit();

        assertNull(InfinispanBatcher.getCurrentBatch());
    }

    @Test
    public void createBatchDiscard() throws Exception {
        Transaction tx = mock(Transaction.class);
        ArgumentCaptor<Synchronization> capturedSync = ArgumentCaptor.forClass(Synchronization.class);

        when(this.tm.getTransaction()).thenReturn(tx);

        try (TransactionBatch batch = this.batcher.createBatch()) {
            verify(this.tm).begin();
            verify(tx).registerSynchronization(capturedSync.capture());

            assertSame(tx, batch.getTransaction());

            batch.discard();
        } finally {
            capturedSync.getValue().afterCompletion(Status.STATUS_ROLLEDBACK);
        }

        verify(tx, never()).commit();
        verify(tx).rollback();

        assertNull(InfinispanBatcher.getCurrentBatch());
    }

    @Test
    public void createNestedBatchClose() throws Exception {
        Transaction tx = mock(Transaction.class);
        ArgumentCaptor<Synchronization> capturedSync = ArgumentCaptor.forClass(Synchronization.class);

        when(this.tm.getTransaction()).thenReturn(tx);

        try (TransactionBatch outerBatch = this.batcher.createBatch()) {
            verify(this.tm).begin();
            verify(tx).registerSynchronization(capturedSync.capture());
            reset(this.tm);

            assertSame(tx, outerBatch.getTransaction());

            when(this.tm.getTransaction()).thenReturn(tx);

            try (TransactionBatch innerBatch = this.batcher.createBatch()) {
                verify(this.tm, never()).begin();
                verify(this.tm, never()).suspend();
            }

            verify(tx, never()).rollback();
            verify(tx, never()).commit();
        } finally {
            capturedSync.getValue().afterCompletion(Status.STATUS_COMMITTED);
        }

        verify(tx, never()).rollback();
        verify(tx).commit();

        assertNull(InfinispanBatcher.getCurrentBatch());
    }

    @Test
    public void createNestedBatchDiscard() throws Exception {
        Transaction tx = mock(Transaction.class);
        ArgumentCaptor<Synchronization> capturedSync = ArgumentCaptor.forClass(Synchronization.class);

        when(this.tm.getTransaction()).thenReturn(tx);

        try (TransactionBatch outerBatch = this.batcher.createBatch()) {
            verify(this.tm).begin();
            verify(tx).registerSynchronization(capturedSync.capture());
            reset(this.tm);

            assertSame(tx, outerBatch.getTransaction());

            when(tx.getStatus()).thenReturn(Status.STATUS_ACTIVE);
            when(this.tm.getTransaction()).thenReturn(tx);

            try (TransactionBatch innerBatch = this.batcher.createBatch()) {
                verify(this.tm, never()).begin();

                innerBatch.discard();
            }

            verify(tx, never()).commit();
            verify(tx, never()).rollback();
        } finally {
            capturedSync.getValue().afterCompletion(Status.STATUS_ROLLEDBACK);
        }

        verify(tx).rollback();
        verify(tx, never()).commit();

        assertNull(InfinispanBatcher.getCurrentBatch());
    }

    @SuppressWarnings("resource")
    @Test
    public void createOverlappingBatchClose() throws Exception {
        Transaction tx = mock(Transaction.class);
        ArgumentCaptor<Synchronization> capturedSync = ArgumentCaptor.forClass(Synchronization.class);

        when(this.tm.getTransaction()).thenReturn(tx);

        TransactionBatch batch = this.batcher.createBatch();

        verify(this.tm).begin();
        verify(tx).registerSynchronization(capturedSync.capture());
        reset(this.tm);

        try {
            assertSame(tx, batch.getTransaction());

            when(this.tm.getTransaction()).thenReturn(tx);
            when(tx.getStatus()).thenReturn(Status.STATUS_ACTIVE);

            try (TransactionBatch innerBatch = this.batcher.createBatch()) {
                verify(this.tm, never()).begin();

                batch.close();

                verify(tx, never()).rollback();
                verify(tx, never()).commit();
            }
        } finally {
            capturedSync.getValue().afterCompletion(Status.STATUS_COMMITTED);
        }

        verify(tx, never()).rollback();
        verify(tx).commit();

        assertNull(InfinispanBatcher.getCurrentBatch());
    }

    @SuppressWarnings("resource")
    @Test
    public void createOverlappingBatchDiscard() throws Exception {
        Transaction tx = mock(Transaction.class);
        ArgumentCaptor<Synchronization> capturedSync = ArgumentCaptor.forClass(Synchronization.class);

        when(this.tm.getTransaction()).thenReturn(tx);

        TransactionBatch batch = this.batcher.createBatch();

        verify(this.tm).begin();
        verify(tx).registerSynchronization(capturedSync.capture());
        reset(this.tm);

        try {
            assertSame(tx, batch.getTransaction());

            when(this.tm.getTransaction()).thenReturn(tx);
            when(tx.getStatus()).thenReturn(Status.STATUS_ACTIVE);

            try (TransactionBatch innerBatch = this.batcher.createBatch()) {
                verify(this.tm, never()).begin();

                innerBatch.discard();

                batch.close();

                verify(tx, never()).commit();
                verify(tx, never()).rollback();
            }
        } finally {
            capturedSync.getValue().afterCompletion(Status.STATUS_ROLLEDBACK);
        }

        verify(tx).rollback();
        verify(tx, never()).commit();

        assertNull(InfinispanBatcher.getCurrentBatch());
    }

    @Test
    public void resumeNullBatch() throws Exception {
        TransactionBatch batch = mock(TransactionBatch.class);
        InfinispanBatcher.setCurrentBatch(batch);

        try (BatchContext context = this.batcher.resumeBatch(null)) {
            verifyZeroInteractions(this.tm);
            assertNull(InfinispanBatcher.getCurrentBatch());
        }
        verifyZeroInteractions(this.tm);
        assertSame(batch, InfinispanBatcher.getCurrentBatch());
    }

    @Test
    public void resumeNonTxBatch() throws Exception {
        TransactionBatch existingBatch = mock(TransactionBatch.class);
        InfinispanBatcher.setCurrentBatch(existingBatch);
        TransactionBatch batch = mock(TransactionBatch.class);

        try (BatchContext context = this.batcher.resumeBatch(batch)) {
            verifyZeroInteractions(this.tm);
            assertSame(batch, InfinispanBatcher.getCurrentBatch());
        }
        verifyZeroInteractions(this.tm);
        assertSame(existingBatch, InfinispanBatcher.getCurrentBatch());
    }

    @Test
    public void resumeBatch() throws Exception {
        TransactionBatch batch = mock(TransactionBatch.class);
        Transaction tx = mock(Transaction.class);

        when(batch.getTransaction()).thenReturn(tx);

        try (BatchContext context = this.batcher.resumeBatch(batch)) {
            verify(this.tm, never()).suspend();
            verify(this.tm).resume(tx);
            reset(this.tm);

            assertSame(batch, InfinispanBatcher.getCurrentBatch());
        }

        verify(this.tm).suspend();
        verify(this.tm, never()).resume(any());

        assertNull(InfinispanBatcher.getCurrentBatch());
    }

    @Test
    public void resumeBatchExisting() throws Exception {
        TransactionBatch existingBatch = mock(TransactionBatch.class);
        Transaction existingTx = mock(Transaction.class);
        InfinispanBatcher.setCurrentBatch(existingBatch);
        TransactionBatch batch = mock(TransactionBatch.class);
        Transaction tx = mock(Transaction.class);

        when(existingBatch.getTransaction()).thenReturn(existingTx);
        when(batch.getTransaction()).thenReturn(tx);
        when(this.tm.suspend()).thenReturn(existingTx);

        try (BatchContext context = this.batcher.resumeBatch(batch)) {
            verify(this.tm).resume(tx);
            reset(this.tm);

            assertSame(batch, InfinispanBatcher.getCurrentBatch());

            when(this.tm.suspend()).thenReturn(tx);
        }

        verify(this.tm).resume(existingTx);

        assertSame(existingBatch, InfinispanBatcher.getCurrentBatch());
    }

    @Test
    public void suspendBatch() throws Exception {
        TransactionBatch batch = mock(TransactionBatch.class);
        InfinispanBatcher.setCurrentBatch(batch);

        TransactionBatch result = this.batcher.suspendBatch();

        verify(this.tm).suspend();

        assertSame(batch, result);
        assertNull(InfinispanBatcher.getCurrentBatch());
    }

    @Test
    public void suspendNoBatch() throws Exception {
        TransactionBatch result = this.batcher.suspendBatch();

        verify(this.tm, never()).suspend();

        assertNull(result);
    }
}
