/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.jakarta;

import java.util.function.Function;

/**
 * Adapts a Jakarta EE9 transaction manager to a Jakarta EE8 transaction.
 * @author Paul Ferraro
 */
public class TransactionManagerAdapter implements javax.transaction.TransactionManager {
    private static final Function<String, javax.transaction.InvalidTransactionException> INVALID_TRANSACTION_EXCEPTION_FACTORY = javax.transaction.InvalidTransactionException::new;
    private static final Function<String, javax.transaction.NotSupportedException> NOT_SUPPORTED_EXCEPTION_FACTORY = javax.transaction.NotSupportedException::new;

    private static javax.transaction.InvalidTransactionException adapt(jakarta.transaction.InvalidTransactionException source) {
        return ExceptionAdapter.adapt(source, INVALID_TRANSACTION_EXCEPTION_FACTORY);
    }

    private static javax.transaction.NotSupportedException adapt(jakarta.transaction.NotSupportedException source) {
        return ExceptionAdapter.adapt(source, NOT_SUPPORTED_EXCEPTION_FACTORY);
    }

    private final jakarta.transaction.TransactionManager tm;

    public TransactionManagerAdapter(jakarta.transaction.TransactionManager tm) {
        this.tm = tm;
    }

    @Override
    public void begin() throws javax.transaction.NotSupportedException, javax.transaction.SystemException {
        try {
            this.tm.begin();
        } catch (jakarta.transaction.NotSupportedException e) {
            throw adapt(e);
        } catch (jakarta.transaction.SystemException e) {
            throw ExceptionAdapter.adapt(e);
        }
    }

    @Override
    public void commit() throws javax.transaction.RollbackException, javax.transaction.HeuristicMixedException, javax.transaction.HeuristicRollbackException, javax.transaction.SystemException {
        try {
            this.tm.commit();
        } catch (jakarta.transaction.RollbackException e) {
            throw ExceptionAdapter.adapt(e);
        } catch (jakarta.transaction.HeuristicMixedException e) {
            throw ExceptionAdapter.adapt(e);
        } catch (jakarta.transaction.HeuristicRollbackException e) {
            throw ExceptionAdapter.adapt(e);
        } catch (jakarta.transaction.SystemException e) {
            throw ExceptionAdapter.adapt(e);
        }
    }

    @Override
    public int getStatus() throws javax.transaction.SystemException {
        try {
            return this.tm.getStatus();
        } catch (jakarta.transaction.SystemException e) {
            throw ExceptionAdapter.adapt(e);
        }
    }

    @Override
    public javax.transaction.Transaction getTransaction() throws javax.transaction.SystemException {
        try {
            return new TransactionAdapter(this.tm.getTransaction());
        } catch (jakarta.transaction.SystemException e) {
            throw ExceptionAdapter.adapt(e);
        }
    }

    @Override
    public void resume(javax.transaction.Transaction tx) throws javax.transaction.InvalidTransactionException, javax.transaction.SystemException {
        if (!(tx instanceof TransactionAdapter)) {
            throw new javax.transaction.InvalidTransactionException(tx.getClass().getName());
        }
        try {
            this.tm.resume(((TransactionAdapter) tx).unwrap());
        } catch (jakarta.transaction.InvalidTransactionException e) {
            throw adapt(e);
        } catch (jakarta.transaction.SystemException e) {
            throw ExceptionAdapter.adapt(e);
        }
    }

    @Override
    public void rollback() throws javax.transaction.SystemException {
        try {
            this.tm.rollback();
        } catch (jakarta.transaction.SystemException e) {
            throw ExceptionAdapter.adapt(e);
        }
    }

    @Override
    public void setRollbackOnly() throws javax.transaction.SystemException {
        try {
            this.tm.setRollbackOnly();
        } catch (jakarta.transaction.SystemException e) {
            throw ExceptionAdapter.adapt(e);
        }
    }

    @Override
    public void setTransactionTimeout(int seconds) throws javax.transaction.SystemException {
        try {
            this.tm.setTransactionTimeout(seconds);
        } catch (jakarta.transaction.SystemException e) {
            throw ExceptionAdapter.adapt(e);
        }
    }

    @Override
    public javax.transaction.Transaction suspend() throws javax.transaction.SystemException {
        try {
            return new TransactionAdapter(this.tm.suspend());
        } catch (jakarta.transaction.SystemException e) {
            throw ExceptionAdapter.adapt(e);
        }
    }
}
