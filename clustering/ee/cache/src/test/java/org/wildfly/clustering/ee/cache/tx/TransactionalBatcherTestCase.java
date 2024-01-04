/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ee.cache.tx;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

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
public class TransactionalBatcherTestCase {
    private final TransactionManager tm = mock(TransactionManager.class);
    private final Batcher<TransactionBatch> batcher = new TransactionalBatcher<>(this.tm, RuntimeException::new);

    @After
    public void destroy() {
        TransactionalBatcher.setCurrentBatch(null);
    }

    @Test
    public void createExistingActiveBatch() throws Exception {
        TransactionBatch existingBatch = mock(TransactionBatch.class);

        TransactionalBatcher.setCurrentBatch(existingBatch);
        when(existingBatch.getState()).thenReturn(Batch.State.ACTIVE);
        when(existingBatch.interpose()).thenReturn(existingBatch);

        TransactionBatch result = this.batcher.createBatch();

        verify(existingBatch).interpose();
        verifyNoInteractions(this.tm);

        assertSame(existingBatch, result);
    }

    @Test
    public void createExistingClosedBatch() throws Exception {
        TransactionBatch existingBatch = mock(TransactionBatch.class);
        Transaction tx = mock(Transaction.class);
        ArgumentCaptor<Synchronization> capturedSync = ArgumentCaptor.forClass(Synchronization.class);

        TransactionalBatcher.setCurrentBatch(existingBatch);
        when(existingBatch.getState()).thenReturn(Batch.State.CLOSED);

        when(this.tm.getTransaction()).thenReturn(tx);

        try (TransactionBatch batch = this.batcher.createBatch()) {
            verify(this.tm).begin();
            verify(tx).registerSynchronization(capturedSync.capture());

            assertSame(tx, batch.getTransaction());
            assertSame(batch, TransactionalBatcher.getCurrentBatch());
        } finally {
            capturedSync.getValue().afterCompletion(Status.STATUS_COMMITTED);
        }

        verify(tx).commit();

        assertNull(TransactionalBatcher.getCurrentBatch());
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

        assertNull(TransactionalBatcher.getCurrentBatch());
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

        assertNull(TransactionalBatcher.getCurrentBatch());
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

        assertNull(TransactionalBatcher.getCurrentBatch());
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

        assertNull(TransactionalBatcher.getCurrentBatch());
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

        assertNull(TransactionalBatcher.getCurrentBatch());
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

        assertNull(TransactionalBatcher.getCurrentBatch());
    }

    @Test
    public void resumeNullBatch() throws Exception {
        TransactionBatch batch = mock(TransactionBatch.class);
        TransactionalBatcher.setCurrentBatch(batch);

        try (BatchContext context = this.batcher.resumeBatch(null)) {
            verifyNoInteractions(this.tm);
            assertNull(TransactionalBatcher.getCurrentBatch());
        }
        verifyNoInteractions(this.tm);
        assertSame(batch, TransactionalBatcher.getCurrentBatch());
    }

    @Test
    public void resumeNonTxBatch() throws Exception {
        TransactionBatch existingBatch = mock(TransactionBatch.class);
        TransactionalBatcher.setCurrentBatch(existingBatch);
        TransactionBatch batch = mock(TransactionBatch.class);

        try (BatchContext context = this.batcher.resumeBatch(batch)) {
            verifyNoInteractions(this.tm);
            assertSame(batch, TransactionalBatcher.getCurrentBatch());
        }
        verifyNoInteractions(this.tm);
        assertSame(existingBatch, TransactionalBatcher.getCurrentBatch());
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

            assertSame(batch, TransactionalBatcher.getCurrentBatch());
        }

        verify(this.tm).suspend();
        verify(this.tm, never()).resume(any());

        assertNull(TransactionalBatcher.getCurrentBatch());
    }

    @Test
    public void resumeBatchExisting() throws Exception {
        TransactionBatch existingBatch = mock(TransactionBatch.class);
        Transaction existingTx = mock(Transaction.class);
        TransactionalBatcher.setCurrentBatch(existingBatch);
        TransactionBatch batch = mock(TransactionBatch.class);
        Transaction tx = mock(Transaction.class);

        when(existingBatch.getTransaction()).thenReturn(existingTx);
        when(batch.getTransaction()).thenReturn(tx);
        when(this.tm.suspend()).thenReturn(existingTx);

        try (BatchContext context = this.batcher.resumeBatch(batch)) {
            verify(this.tm).resume(tx);
            reset(this.tm);

            assertSame(batch, TransactionalBatcher.getCurrentBatch());

            when(this.tm.suspend()).thenReturn(tx);
        }

        verify(this.tm).resume(existingTx);

        assertSame(existingBatch, TransactionalBatcher.getCurrentBatch());
    }

    @Test
    public void suspendBatch() throws Exception {
        TransactionBatch batch = mock(TransactionBatch.class);
        TransactionalBatcher.setCurrentBatch(batch);

        TransactionBatch result = this.batcher.suspendBatch();

        verify(this.tm).suspend();

        assertSame(batch, result);
        assertNull(TransactionalBatcher.getCurrentBatch());
    }

    @Test
    public void suspendNoBatch() throws Exception {
        TransactionBatch result = this.batcher.suspendBatch();

        verify(this.tm, never()).suspend();

        assertNull(result);
    }
}
