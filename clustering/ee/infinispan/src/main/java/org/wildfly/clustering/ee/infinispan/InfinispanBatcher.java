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
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
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

    private static final BatchContext PASSIVE_BATCH_CONTEXT = () -> {
        // Do nothing
    };

    private static final TransactionBatch NON_TX_BATCH = new TransactionBatch() {
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

        @Override
        public TransactionBatch interpose() {
            return this;
        }
    };

    // Used to coalesce interposed transactions
    static final ThreadLocal<TransactionBatch> CURRENT_BATCH = new ThreadLocal<>();

    private static final Synchronization CURRENT_BATCH_REMOVER = new Synchronization() {
        @Override
        public void beforeCompletion() {
        }

        @Override
        public void afterCompletion(int status) {
            CURRENT_BATCH.remove();
        }
    };

    private final TransactionManager tm;

    public InfinispanBatcher(Cache<?, ?> cache) {
        this(cache.getAdvancedCache().getTransactionManager());
    }

    public InfinispanBatcher(TransactionManager tm) {
        this.tm = tm;
    }

    @Override
    public TransactionBatch createBatch() {
        if (this.tm == null) return NON_TX_BATCH;
        TransactionBatch batch = CURRENT_BATCH.get();
        if (batch != null) {
            return batch.interpose();
        }
        try {
            this.tm.suspend();
            this.tm.begin();
            Transaction tx = this.tm.getTransaction();
            tx.registerSynchronization(CURRENT_BATCH_REMOVER);
            batch = new InfinispanBatch(tx);
            CURRENT_BATCH.set(batch);
            return batch;
        } catch (RollbackException | SystemException | NotSupportedException e) {
            throw new CacheException(e);
        }
    }

    @Override
    public BatchContext resumeBatch(TransactionBatch batch) {
        TransactionBatch existingBatch = CURRENT_BATCH.get();
        // Trivial case - nothing to suspend/resume
        if (batch == existingBatch) return PASSIVE_BATCH_CONTEXT;
        Transaction tx = (batch != null) ? batch.getTransaction() : null;
        // Non-tx case, just swap thread local
        if ((batch == null) || (tx == null)) {
            CURRENT_BATCH.set(batch);
            return () -> {
                CURRENT_BATCH.set(existingBatch);
            };
        }
        try {
            if (existingBatch != null) {
                Transaction existingTx = this.tm.suspend();
                if (existingBatch.getTransaction() != existingTx) {
                    throw new IllegalStateException();
                }
            }
            this.tm.resume(tx);
            CURRENT_BATCH.set(batch);
            return () -> {
                try {
                    this.tm.suspend();
                    if (existingBatch != null) {
                        try {
                            this.tm.resume(existingBatch.getTransaction());
                            CURRENT_BATCH.set(existingBatch);
                        } catch (InvalidTransactionException e) {
                            throw new CacheException(e);
                        }
                    } else {
                        CURRENT_BATCH.remove();
                    }
                } catch (SystemException e) {
                    throw new CacheException(e);
                }
            };
        } catch (SystemException | InvalidTransactionException e) {
            throw new CacheException(e);
        }
    }

    @Override
    public TransactionBatch suspendBatch() {
        if (this.tm == null) return NON_TX_BATCH;
        TransactionBatch batch = CURRENT_BATCH.get();
        if (batch != null) {
            try {
                Transaction tx = this.tm.suspend();
                if (batch.getTransaction() != tx) {
                    throw new IllegalStateException();
                }
            } catch (SystemException e) {
                throw new CacheException(e);
            } finally {
                CURRENT_BATCH.remove();
            }
        }
        return batch;
    }
}
