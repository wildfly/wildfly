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

import javax.transaction.xa.XAResource;

/**
 * Adapts a Jakarta EE9 transaction to a Jakarta EE8 transaction.
 * @author Paul Ferraro
 */
public class TransactionAdapter implements javax.transaction.Transaction {

    private final jakarta.transaction.Transaction tx;

    TransactionAdapter(jakarta.transaction.Transaction tx) {
        this.tx = tx;
    }

    jakarta.transaction.Transaction unwrap() {
        return this.tx;
    }

    @Override
    public void commit() throws javax.transaction.RollbackException, javax.transaction.HeuristicMixedException, javax.transaction.HeuristicRollbackException, javax.transaction.SystemException {
        try {
            this.tx.commit();
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
    public boolean delistResource(XAResource resource, int flag) throws javax.transaction.SystemException {
        try {
            return this.tx.delistResource(resource, flag);
        } catch (jakarta.transaction.SystemException e) {
            throw ExceptionAdapter.adapt(e);
        }
    }

    @Override
    public boolean enlistResource(XAResource resource) throws javax.transaction.RollbackException, javax.transaction.SystemException {
        try {
            return this.tx.enlistResource(resource);
        } catch (jakarta.transaction.RollbackException e) {
            throw ExceptionAdapter.adapt(e);
        } catch (jakarta.transaction.SystemException e) {
            throw ExceptionAdapter.adapt(e);
        }
    }

    @Override
    public int getStatus() throws javax.transaction.SystemException {
        try {
            return this.tx.getStatus();
        } catch (jakarta.transaction.SystemException e) {
            throw ExceptionAdapter.adapt(e);
        }
    }

    @Override
    public void registerSynchronization(javax.transaction.Synchronization sync) throws javax.transaction.RollbackException, javax.transaction.SystemException {
        try {
            this.tx.registerSynchronization(new SynchronizationAdapter(sync));
        } catch (jakarta.transaction.RollbackException e) {
            throw ExceptionAdapter.adapt(e);
        } catch (jakarta.transaction.SystemException e) {
            throw ExceptionAdapter.adapt(e);
        }
    }

    @Override
    public void rollback() throws javax.transaction.SystemException {
        try {
            this.tx.rollback();
        } catch (jakarta.transaction.SystemException e) {
            throw ExceptionAdapter.adapt(e);
        }
    }

    @Override
    public void setRollbackOnly() throws javax.transaction.SystemException {
        try {
            this.tx.setRollbackOnly();
        } catch (jakarta.transaction.SystemException e) {
            throw ExceptionAdapter.adapt(e);
        }
    }
}