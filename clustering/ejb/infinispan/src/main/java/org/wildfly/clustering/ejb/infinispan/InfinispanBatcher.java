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
import org.wildfly.clustering.ejb.Batch;
import org.wildfly.clustering.ejb.Batcher;

/**
 * A "thread-safe" {@link Batcher} implementation, whose batches can be closed by a different thread than the thread that started the batch.
 * Infinispan's BatchContainer relies on ThreadLocals, so we must use the TransactionManager directly.
 * @author Paul Ferraro
 */
public class InfinispanBatcher implements Batcher {
    private static final Batch NULL_BATCH = new Batch() {
        @Override
        public void close() {
        }

        @Override
        public void discard() {
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
    public Batch startBatch() {
        try {
            Transaction existingTx = this.tm.getTransaction();
            return (existingTx == null) ? new TransactionBatch(this.tm) : NULL_BATCH;
        } catch (SystemException e) {
            throw new CacheException(e);
        }
    }

    private static class TransactionBatch implements Batch {

        private final TransactionManager tm;
        private final Transaction tx;

        TransactionBatch(TransactionManager tm) throws SystemException {
            this.tm = tm;
            try {
                this.tm.begin();
                this.tx = this.tm.suspend();
            } catch (NotSupportedException e) {
                throw new CacheException(e);
            }
        }

        @Override
        public void close() {
            this.end(true);
        }

        @Override
        public void discard() {
            this.end(false);
        }

        private void end(boolean success) {
            try {
                // We can't just commit/rollback the Transaction instance
                // Infinispan's DummyTransaction implementation assumes that the transaction is associated with the current thread
                Transaction existingTx = this.tm.suspend();
                this.tm.resume(this.tx);
                try {
                    if (success) {
                        try {
                            this.tm.commit();
                        } catch (RollbackException | HeuristicMixedException | HeuristicRollbackException e) {
                            throw new CacheException(e);
                        }
                    } else {
                        this.tm.rollback();
                    }
                } finally {
                    if ((existingTx != null) && !existingTx.equals(this.tx)) {
                        this.tm.resume(existingTx);
                    }
                }
            } catch (SystemException | InvalidTransactionException e) {
                throw new CacheException(e);
            }
        }
    }
}
