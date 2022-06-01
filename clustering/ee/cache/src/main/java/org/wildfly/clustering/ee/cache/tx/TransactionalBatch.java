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
package org.wildfly.clustering.ee.cache.tx;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;

/**
 * Abstract {@link TransactionBatch} that associates and exposes the underlying transaction.
 * @author Paul Ferraro
 */
public class TransactionalBatch<E extends RuntimeException> implements TransactionBatch {

    private final Function<Throwable, E> exceptionTransformer;
    private final Transaction tx;
    private final AtomicInteger count = new AtomicInteger(0);

    private volatile boolean active = true;

    public TransactionalBatch(Transaction tx, Function<Throwable, E> exceptionTransformer) {
        this.tx = tx;
        this.exceptionTransformer = exceptionTransformer;
    }

    @Override
    public Transaction getTransaction() {
        return this.tx;
    }

    @Override
    public TransactionBatch interpose() {
        this.count.incrementAndGet();
        return this;
    }

    @Override
    public void discard() {
        // Allow additional cache operations prior to close, rather than call tx.setRollbackOnly()
        this.active = false;
    }

    @Override
    public State getState() {
        try {
            switch (this.tx.getStatus()) {
                case Status.STATUS_ACTIVE: {
                    if (this.active) {
                        return State.ACTIVE;
                    }
                    // Otherwise fall through
                }
                case Status.STATUS_MARKED_ROLLBACK: {
                    return State.DISCARDED;
                }
                default: {
                    return State.CLOSED;
                }
            }
        } catch (SystemException e) {
            throw this.exceptionTransformer.apply(e);
        }
    }

    @Override
    public void close() {
        if (this.count.getAndDecrement() == 0) {
            try {
                switch (this.tx.getStatus()) {
                    case Status.STATUS_ACTIVE: {
                        if (this.active) {
                            try {
                                this.tx.commit();
                                break;
                            } catch (RollbackException e) {
                                throw new IllegalStateException(e);
                            } catch (HeuristicMixedException | HeuristicRollbackException e) {
                                throw this.exceptionTransformer.apply(e);
                            }
                        }
                        // Otherwise fall through
                    }
                    case Status.STATUS_MARKED_ROLLBACK: {
                        this.tx.rollback();
                        break;
                    }
                }
            } catch (SystemException e) {
                throw this.exceptionTransformer.apply(e);
            }
        }
    }

    @Override
    public int hashCode() {
        return this.tx.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof TransactionalBatch)) return false;
        TransactionalBatch<?> batch = (TransactionalBatch<?>) object;
        return this.tx.equals(batch.tx);
    }

    @Override
    public String toString() {
        return String.format("%s[%d]", this.tx, this.count.get());
    }
}