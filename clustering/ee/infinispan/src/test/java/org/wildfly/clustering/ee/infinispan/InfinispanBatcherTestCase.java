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
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.junit.Test;
import org.wildfly.clustering.ee.BatchContext;
import org.wildfly.clustering.ee.Batcher;

/**
 * Unit test for {@link InfinispanBatcher}.
 * @author Paul Ferraro
 */
public class InfinispanBatcherTestCase {
    private final TransactionManager tm = mock(TransactionManager.class);
    private final Batcher<TransactionBatch> batcher = new InfinispanBatcher(this.tm);

    @Test
    public void createNewBatchClose() throws Exception {
        Transaction tx = mock(Transaction.class);

        when(this.tm.getTransaction()).thenReturn(null, tx);

        try (TransactionBatch batch = this.batcher.createBatch()) {
            verify(this.tm).begin();

            assertSame(tx, batch.getTransaction());
        }

        verify(this.tm).commit();
    }

    @Test
    public void createNewBatchDiscard() throws Exception {
        Transaction tx = mock(Transaction.class);

        when(this.tm.getTransaction()).thenReturn(null, tx);

        TransactionBatch batch = this.batcher.createBatch();
        try {
            verify(this.tm).begin();

            assertSame(tx, batch.getTransaction());
        } finally {
            batch.discard();
        }

        verify(this.tm).rollback();
    }

    @Test
    public void createExistingActiveBatchClose() throws Exception {
        Transaction tx = mock(Transaction.class);

        when(this.tm.getTransaction()).thenReturn(tx);
        when(tx.getStatus()).thenReturn(Status.STATUS_ACTIVE);

        try (TransactionBatch batch = this.batcher.createBatch()) {
            verify(this.tm, never()).begin();

            assertSame(tx, batch.getTransaction());
        }

        verify(this.tm, never()).commit();
    }

    @Test
    public void createExistingNonActiveBatchClose() throws Exception {
        Transaction tx = mock(Transaction.class);

        when(this.tm.getTransaction()).thenReturn(tx);
        when(tx.getStatus()).thenReturn(Status.STATUS_COMMITTED);

        try (TransactionBatch batch = this.batcher.createBatch()) {
            verify(this.tm).suspend();
            verify(this.tm).begin();

            assertSame(tx, batch.getTransaction());
        }

        verify(this.tm).commit();
    }

    @Test
    public void createExistingActiveBatchDiscard() throws Exception {
        Transaction tx = mock(Transaction.class);

        when(this.tm.getTransaction()).thenReturn(tx);
        when(tx.getStatus()).thenReturn(Status.STATUS_ACTIVE);

        TransactionBatch batch = this.batcher.createBatch();
        try {
            verify(this.tm, never()).begin();

            assertSame(tx, batch.getTransaction());
        } finally {
            batch.discard();
        }

        verify(this.tm, never()).rollback();
    }

    @Test
    public void createExistingNonActiveBatchDiscard() throws Exception {
        Transaction tx = mock(Transaction.class);

        when(this.tm.getTransaction()).thenReturn(tx);
        when(tx.getStatus()).thenReturn(Status.STATUS_COMMITTED);

        TransactionBatch batch = this.batcher.createBatch();
        try {
            verify(this.tm).suspend();
            verify(this.tm).begin();

            assertSame(tx, batch.getTransaction());
        } finally {
            batch.discard();
        }

        verify(this.tm).rollback();
    }

    @Test
    public void resumeBatch() throws Exception {
        TransactionBatch batch = mock(TransactionBatch.class);
        Transaction batchTx = mock(Transaction.class);

        when(batch.getTransaction()).thenReturn(batchTx);
        when(this.tm.suspend()).thenReturn(batchTx);

        try (BatchContext context = this.batcher.resumeBatch(batch)) {
            verify(this.tm).resume(batchTx);
            reset(this.tm);
        }

        verifyZeroInteractions(this.tm);
    }

    @Test
    public void resumeBatchWithExisting() throws Exception {
        TransactionBatch batch = mock(TransactionBatch.class);
        Transaction batchTx = mock(Transaction.class);
        Transaction otherTx = mock(Transaction.class);

        when(batch.getTransaction()).thenReturn(batchTx);
        when(this.tm.suspend()).thenReturn(otherTx);

        try (BatchContext context = this.batcher.resumeBatch(batch)) {
            verify(this.tm).resume(batchTx);
            reset(this.tm);
        }

        verify(this.tm).resume(otherTx);
    }

    @Test
    public void suspendBatch() throws Exception {
        Transaction batchTx = mock(Transaction.class);

        when(this.tm.suspend()).thenReturn(batchTx);

        TransactionBatch result = this.batcher.suspendBatch();

        assertSame(batchTx, result.getTransaction());
    }

    @Test
    public void suspendNoBatch() throws Exception {

        when(this.tm.suspend()).thenReturn(null);

        TransactionBatch result = this.batcher.suspendBatch();

        assertNull(result);
    }
}
