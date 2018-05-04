/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.txn.service.internal.tsr;

import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionSynchronizationRegistry;

import org.wildfly.transaction.client.AbstractTransaction;
import org.wildfly.transaction.client.ContextTransactionManager;
import org.wildfly.transaction.client.ContextTransactionSynchronizationRegistry;

/**
 * Most of this implementation delegates down to the underlying transactions implementation to provide the services of the
 * TransactionSynchronizationRegistry. The one area it modifies is the registration of the interposed Synchronizations. The
 * reason this implementation needs to differ is because the JCA Synchronization and JPA Synchronizations are both specified as
 * Interposed however there are defined ordering requirements between them both.
 *
 * The current implementation orders JCA relative to all other Synchronizations. For beforeCompletion, it would be possible to
 * restrict this to the one case where JCA is ordered before JPA, however it is possible that other interposed Synchronizations
 * would require the services of JCA and as such if the JCA is allowed to execute delistResource during beforeCompletion as
 * mandated in JCA spec the behaviour of those subsequent interactions would be broken. For afterCompletion the JCA
 * synchronizations are called last as that allows JCA to detect connection leaks from frameworks that have not closed the JCA
 * managed resources. This is described in (for example)
 * http://docs.oracle.com/javaee/5/api/javax/transaction/TransactionSynchronizationRegistry
 * .html#registerInterposedSynchronization(javax.transaction.Synchronization) where it says that during afterCompletion
 * "Resources can be closed but no transactional work can be performed with them".
 *
 * One implication of this approach is that if the underlying transactions implementation has special handling for various types
 * of Synchronization that can also implement other interfaces (i.e. if interposedSync instanceof OtherInterface) these
 * behaviours cannot take effect as the underlying implementation will never directly see the actual Synchronizations.
 */
public class TransactionSynchronizationRegistryWrapper implements TransactionSynchronizationRegistry {

    private final Object key = new Object();

    public TransactionSynchronizationRegistryWrapper() {
    }

    @Override
    public void registerInterposedSynchronization(Synchronization sync)
        throws IllegalStateException {
        try {
            AbstractTransaction tx = ContextTransactionManager.getInstance().getTransaction();
            JCAOrderedLastSynchronizationList jcaOrderedLastSynchronization = (JCAOrderedLastSynchronizationList) tx.getResource(key);
            if (jcaOrderedLastSynchronization == null) {
                final ContextTransactionSynchronizationRegistry tsr = ContextTransactionSynchronizationRegistry.getInstance();
                synchronized (key) {
                    jcaOrderedLastSynchronization = (JCAOrderedLastSynchronizationList) tx.getResource(key);
                    if (jcaOrderedLastSynchronization == null) {
                        jcaOrderedLastSynchronization = new JCAOrderedLastSynchronizationList();
                        tx.putResource(key, jcaOrderedLastSynchronization);
                        tsr.registerInterposedSynchronization(jcaOrderedLastSynchronization);
                    }
                }
            }
            jcaOrderedLastSynchronization.registerInterposedSynchronization(sync);
        } catch (SystemException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Object getTransactionKey() {
        return ContextTransactionSynchronizationRegistry.getInstance().getTransactionKey();
    }

    @Override
    public int getTransactionStatus() {
        return ContextTransactionSynchronizationRegistry.getInstance().getTransactionStatus();
    }

    @Override
    public boolean getRollbackOnly() throws IllegalStateException {
        return ContextTransactionSynchronizationRegistry.getInstance().getRollbackOnly();
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException {
        ContextTransactionSynchronizationRegistry.getInstance().setRollbackOnly();
    }

    @Override
    public Object getResource(Object key) throws IllegalStateException {
        return ContextTransactionSynchronizationRegistry.getInstance().getResource(key);
    }

    @Override
    public void putResource(Object key, Object value)
        throws IllegalStateException {
        ContextTransactionSynchronizationRegistry.getInstance().putResource(key, value);
    }

}
