/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container.persistence32;

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
public final class WildFlyEntityManagerFactory32 implements ExtendedEntityManager.Factory, TransactionScopedEntityManager.Factory {

    @Override
    public ExtendedEntityManager createExtendedEntityManager(String puScopedName, EntityManager underlyingEntityManager, SynchronizationType synchronizationType, TransactionSynchronizationRegistry transactionSynchronizationRegistry, TransactionManager transactionManager) {
        return new ExtendedEntityManager32(puScopedName, underlyingEntityManager, synchronizationType,
                transactionSynchronizationRegistry, transactionManager);
    }

    @Override
    public TransactionScopedEntityManager createTransactionScopedEntityManager(String puScopedName, Map properties, EntityManagerFactory emf, SynchronizationType synchronizationType, TransactionSynchronizationRegistry transactionSynchronizationRegistry, TransactionManager transactionManager) {
        return new TransactionScopedEntityManager32(puScopedName, properties, emf, synchronizationType,
                transactionSynchronizationRegistry, transactionManager);
    }
}
