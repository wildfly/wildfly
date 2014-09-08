/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.idm.jpa.transaction;

import org.wildfly.extension.picketlink.logging.PicketLinkLogger;

import javax.persistence.EntityManager;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 * <p>A helper that knows how to associate {@link javax.persistence.EntityManager} instances with active transactions.</p>
 *
 * @author Pedro Igor (reusing code from JPA subsystem)
 */
public class TransactionalEntityManagerHelper {

    private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private final TransactionManager transactionManager;

    public TransactionalEntityManagerHelper(TransactionSynchronizationRegistry transactionSynchronizationRegistry,
        TransactionManager transactionManager) {
        this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
        this.transactionManager = transactionManager;
    }

    /**
     * Get current persistence context.  Only call while a transaction is active in the current thread.
     *
     * @param puScopedName
     * @return
     */
    public EntityManager getTransactionScopedEntityManager(String puScopedName) {
        return (EntityManager) this.transactionSynchronizationRegistry.getResource(puScopedName);
    }

    /**
     * Save the specified EntityManager in the local threads active transaction.  The TransactionSynchronizationRegistry
     * will clear the reference to the EntityManager when the transaction completes.
     *
     * @param scopedPuName
     * @param entityManager
     */
    public  void putEntityManagerInTransactionRegistry(String scopedPuName, EntityManager entityManager) {
        try {
            Transaction transaction = this.transactionManager.getTransaction();

            transaction.registerSynchronization(new TransactionalEntityManagerSynchronization(entityManager));

            this.transactionSynchronizationRegistry.putResource(scopedPuName, entityManager);
        } catch (Exception e) {
            throw PicketLinkLogger.ROOT_LOGGER.idmJpaFailedCreateTransactionEntityManager(e);
        }
    }
}
