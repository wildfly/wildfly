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

import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.wildfly.clustering.ee.BatchContext;
import org.wildfly.clustering.ee.Batcher;

/**
 * A {@link Batcher} implementation based on Infinispan's {@link org.infinispan.batch.BatchContainer}, except that its transaction reference
 * is stored within the returned Batch object instead of a ThreadLocal.  This also allows the user to call {@link Batch#close()} from a
 * different thread than the one that created the {@link Batch}.  In this case, however, the user must first resume the batch
 * via {@link #resumeBatch(TransactionBatch)}.
 * @author Paul Ferraro
 */
public class InfinispanBatcher implements Batcher<TransactionBatch> {

    private static final BatchContext PASSIVE_BATCH_CONTEXT = new BatchContext() {
        @Override
        public void close() {
            // Do nothing
        }
    };

    private static final TransactionBatch NON_TX_BATCH = new TransactionBatch() {
        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void close() {
            // No-op
        }

        @Override
        public void discard() {
            // No-op
        }

        @Override
        public Transaction getTransaction() {
            return null;
        }
    };

    private final TransactionManager tm;

    public InfinispanBatcher(Cache<?, ?> cache) {
        this(cache.getAdvancedCache().getTransactionManager());
    }

    public InfinispanBatcher(TransactionManager tm) {
        this.tm = tm;
    }

    @SuppressWarnings("resource")
    @Override
    public TransactionBatch createBatch() {
        if (this.tm == null) return NON_TX_BATCH;
        try {
            Transaction tx = this.tm.getTransaction();
            // Consolidate nested batches into a single operational batch
            return (tx == null) ? new NewTransactionBatch(this.tm) : new NestedTransactionBatch(tx);
        } catch (SystemException e) {
            throw new CacheException(e);
        }
    }

    @Override
    public BatchContext resumeBatch(TransactionBatch batch) {
        if (batch == null) return PASSIVE_BATCH_CONTEXT;
        try {
            Transaction tx = batch.getTransaction();
            return (tx != null) ? new InfinispanBatchContext(this.tm, tx) : PASSIVE_BATCH_CONTEXT;
        } catch (SystemException | InvalidTransactionException e) {
            throw new CacheException(e);
        }
    }

    @Override
    public TransactionBatch suspendBatch() {
        if (this.tm == null) return null;
        try {
            Transaction tx = this.tm.suspend();
            return (tx != null) ? new ActiveTransactionBatch(this.tm, tx) : null;
        } catch (SystemException e) {
            throw new CacheException(e);
        }
    }
}
