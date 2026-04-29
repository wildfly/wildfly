/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container.persistence31;

import java.io.Serial;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SynchronizationType;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.jboss.as.jpa.container.ExtendedEntityManager;
import org.jboss.as.jpa.container.TransactionScopedEntityManager;
import org.kohsuke.MetaInfServices;

@MetaInfServices(value = {ExtendedEntityManager.Factory.class, TransactionScopedEntityManager.Factory.class})
public final class WildFlyEntityManagerFactory31 implements ExtendedEntityManager.Factory, TransactionScopedEntityManager.Factory {

    @Override
    public ExtendedEntityManager createExtendedEntityManager(String puScopedName, EntityManager underlyingEntityManager, SynchronizationType synchronizationType, TransactionSynchronizationRegistry transactionSynchronizationRegistry, TransactionManager transactionManager) {
        return new ExtendedEntityManager31(puScopedName, underlyingEntityManager, synchronizationType,
                transactionSynchronizationRegistry, transactionManager);
    }

    @Override
    public TransactionScopedEntityManager createTransactionScopedEntityManager(String puScopedName, Map properties, EntityManagerFactory emf, SynchronizationType synchronizationType, TransactionSynchronizationRegistry transactionSynchronizationRegistry, TransactionManager transactionManager) {
        return new TransactionScopedEntityManager31(puScopedName, properties, emf, synchronizationType,
                transactionSynchronizationRegistry, transactionManager);
    }

    public static class ExtendedEntityManager31 extends ExtendedEntityManager {

        @Serial
        private static final long serialVersionUID = -4303887788978456675L;

        private ExtendedEntityManager31(String puScopedName, EntityManager underlyingEntityManager, jakarta.persistence.SynchronizationType synchronizationType, jakarta.transaction.TransactionSynchronizationRegistry transactionSynchronizationRegistry, jakarta.transaction.TransactionManager transactionManager) {
            super(puScopedName, underlyingEntityManager, synchronizationType, transactionSynchronizationRegistry, transactionManager);
        }
    }

    public static class TransactionScopedEntityManager31 extends TransactionScopedEntityManager {

        @Serial
        private static final long serialVersionUID = 4060580192670417074L;

        private TransactionScopedEntityManager31(String puScopedName, Map properties, jakarta.persistence.EntityManagerFactory emf, jakarta.persistence.SynchronizationType synchronizationType, jakarta.transaction.TransactionSynchronizationRegistry transactionSynchronizationRegistry, jakarta.transaction.TransactionManager transactionManager) {
            super(puScopedName, properties, emf, synchronizationType, transactionSynchronizationRegistry, transactionManager);
        }
    }
}
