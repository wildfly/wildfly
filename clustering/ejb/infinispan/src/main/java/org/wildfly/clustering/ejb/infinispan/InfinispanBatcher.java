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

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.wildfly.clustering.ejb.BatchContext;
import org.wildfly.clustering.ejb.Batcher;

/**
 * A {@link Batcher} implementation based on Infinispan's {@link org.infinispan.batch.BatchContainer}, except that its transaction reference
 * is stored within the returned Batch object instead of a ThreadLocal.  This also allows the user to call {@link Batch#close()} from a
 * different thread than the one that created the {@link Batch}.  In this case, however, the user must first resume the batch
 * via {@link #resume(TransactionBatch)}.
 * @author Paul Ferraro
 */
public class InfinispanBatcher implements Batcher<TransactionBatch> {

    private final TransactionManager tm;

    public InfinispanBatcher(Cache<?, ?> cache) {
        this(cache.getAdvancedCache().getTransactionManager());
    }

    public InfinispanBatcher(TransactionManager tm) {
        this.tm = tm;
    }

    @Override
    public TransactionBatch startBatch() {
        try {
            Transaction tx = this.tm.getTransaction();
            // Consolidate nested batches into a single operational batch
            return (tx == null) ? new NewTransactionBatch(this.tm) : new ExistingTransactionBatch(tx);
        } catch (SystemException e) {
            throw new CacheException(e);
        }
    }

    @Override
    public BatchContext resume(TransactionBatch batch) {
        try {
            Transaction tx = batch.getTransaction();
            return new InfinispanBatchContext(this.tm, tx);
        } catch (SystemException | InvalidTransactionException e) {
            throw new CacheException(e);
        }
    }

    private static class InfinispanBatchContext implements BatchContext {
        private final TransactionManager tm;
        private final Transaction existingTx;
        private final Transaction tx;

        InfinispanBatchContext(TransactionManager tm, Transaction tx) throws SystemException, InvalidTransactionException {
            this.tm = tm;
            this.tx = tx;
            // Switch transaction context
            this.existingTx = this.tm.suspend();
            this.tm.resume(this.tx);
        }

        @Override
        public void close() {
            // Restore previous transaction context, if necessary
            if ((this.existingTx != null) && !this.existingTx.equals(this.tx)) {
                try {
                    this.tm.resume(this.existingTx);
                } catch (InvalidTransactionException | SystemException e) {
                    throw new CacheException(e);
                }
            }
        }
    }

    private static class ExistingTransactionBatch implements TransactionBatch {
        private final Transaction tx;

        ExistingTransactionBatch(Transaction tx) {
            this.tx = tx;
        }

        @Override
        public Transaction getTransaction() {
            return this.tx;
        }

        @Override
        public void close() {
        }

        @Override
        public void discard() {
        }
    }

    private static class NewTransactionBatch extends ExistingTransactionBatch {
        private final TransactionManager tm;

        NewTransactionBatch(TransactionManager tm) throws SystemException {
            this(tm, begin(tm));
        }

        private static Transaction begin(TransactionManager tm) throws SystemException {
            try {
                tm.begin();
                return tm.getTransaction();
            } catch (NotSupportedException e) {
                throw new CacheException(e);
            }
        }

        private NewTransactionBatch(TransactionManager tm, Transaction tx) {
            super(tx);
            this.tm = tm;
        }

        @Override
        public void close() {
            try {
                this.tm.commit();
            } catch (RollbackException | HeuristicMixedException | HeuristicRollbackException | SystemException e) {
                throw new CacheException(e);
            }
        }

        @Override
        public void discard() {
            try {
                this.tm.rollback();
            } catch (SystemException e) {
                throw new CacheException(e);
            }
        }
    }
}
