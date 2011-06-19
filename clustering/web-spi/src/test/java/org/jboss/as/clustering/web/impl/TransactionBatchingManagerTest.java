/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.web.impl;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.as.clustering.web.BatchingManager;
import org.junit.After;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class TransactionBatchingManagerTest {
    private final TransactionManager tm = mock(TransactionManager.class);
    private final BatchingManager bm = new TransactionBatchingManager(this.tm);

    @After
    public void resetMocks() {
        reset(this.tm);
    }

    @Test
    public void isBatchInProgress() throws Exception {
        when(this.tm.getTransaction()).thenReturn(null);

        assertFalse(this.bm.isBatchInProgress());

        Transaction transaction = mock(Transaction.class);

        when(this.tm.getTransaction()).thenReturn(transaction);

        assertTrue(this.bm.isBatchInProgress());
    }

    @Test
    public void setRollbackOnly() throws Exception {
        this.bm.setBatchRollbackOnly();

        verify(this.tm).setRollbackOnly();
    }

    @Test
    public void startBatch() throws Exception {
        this.bm.startBatch();

        verify(this.tm).begin();
    }

    @Test
    public void endBatch() throws Exception {
        this.commitBatch(Status.STATUS_ACTIVE);
        this.commitBatch(Status.STATUS_COMMITTED);
        this.commitBatch(Status.STATUS_COMMITTING);
        this.rollbackBatch(Status.STATUS_MARKED_ROLLBACK);
        this.commitBatch(Status.STATUS_NO_TRANSACTION);
        this.commitBatch(Status.STATUS_PREPARED);
        this.commitBatch(Status.STATUS_PREPARING);
        this.commitBatch(Status.STATUS_ROLLEDBACK);
        this.commitBatch(Status.STATUS_ROLLING_BACK);
        this.commitBatch(Status.STATUS_UNKNOWN);
    }

    private void commitBatch(int status) throws Exception {
        Transaction transaction = mock(Transaction.class);

        when(this.tm.getTransaction()).thenReturn(transaction);
        when(transaction.getStatus()).thenReturn(status);

        this.bm.endBatch();

        verify(this.tm).commit();

        reset(this.tm);
    }

    private void rollbackBatch(int status) throws Exception {
        Transaction transaction = mock(Transaction.class);

        when(this.tm.getTransaction()).thenReturn(transaction);
        when(transaction.getStatus()).thenReturn(status);

        this.bm.endBatch();

        verify(this.tm).rollback();
    }
}
