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

package org.wildfly.clustering.ejb.infinispan;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.junit.Test;
import org.wildfly.clustering.ejb.BatchContext;
import org.wildfly.clustering.ejb.Batcher;

/**
 * @author Paul Ferraro
 */
@SuppressWarnings("resource")
public class InfinispanBatcherTestCase {
    private final TransactionManager tm = mock(TransactionManager.class);

    private final Batcher<TransactionBatch> batcher = new InfinispanBatcher(this.tm);

    @Test
    public void commit() throws Exception {
        Transaction tx = mock(Transaction.class);

        when(this.tm.getTransaction()).thenReturn(null, tx);

        TransactionBatch batch = this.batcher.startBatch();

        verify(this.tm).begin();

        assertSame(tx, batch.getTransaction());

        batch.close();

        verify(this.tm).commit();
    }

    @Test
    public void rollback() throws Exception {
        Transaction tx = mock(Transaction.class);

        when(this.tm.getTransaction()).thenReturn(null, tx);

        TransactionBatch batch = this.batcher.startBatch();

        verify(this.tm).begin();

        assertSame(tx, batch.getTransaction());

        batch.discard();

        verify(this.tm).rollback();
    }

    @Test
    public void existing() throws Exception {
        Transaction tx = mock(Transaction.class);

        when(this.tm.getTransaction()).thenReturn(tx);

        TransactionBatch batch = this.batcher.startBatch();

        verify(this.tm, never()).begin();

        assertSame(tx, batch.getTransaction());

        batch.close();

        verify(this.tm, never()).commit();

        batch.discard();

        verify(this.tm, never()).rollback();
    }

    @Test
    public void activate() throws Exception {
        Transaction tx = mock(Transaction.class);

        when(this.tm.getTransaction()).thenReturn(tx);

        TransactionBatch batch = this.batcher.startBatch();

        assertSame(tx, batch.getTransaction());

        Transaction existing = mock(Transaction.class);

        when(this.tm.suspend()).thenReturn(existing);

        BatchContext context = this.batcher.resume(batch);

        verify(this.tm).resume(same(tx));

        context.close();

        verify(this.tm).resume(same(existing));
    }
}
