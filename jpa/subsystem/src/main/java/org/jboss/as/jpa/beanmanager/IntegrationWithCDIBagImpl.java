/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.beanmanager;

import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.jipijapa.plugin.spi.IntegrationWithCDIBag;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * IntegrationWithCDIBagImpl
 *
 * @author Scott Marlow
 */
public class IntegrationWithCDIBagImpl implements IntegrationWithCDIBag {

    private volatile EntityManagerFactory entityManagerFactory;
    private volatile TransactionManager transactionManager;
    private volatile TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private volatile PersistenceUnitMetadata persistenceUnitMetadata;

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        return transactionSynchronizationRegistry;
    }

    public void setTransactionSynchronizationRegistry(TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
        this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
    }

    public PersistenceUnitMetadata getPersistenceUnitMetadata() {
        return persistenceUnitMetadata;
    }

    public void setPersistenceUnitMetadata(PersistenceUnitMetadata persistenceUnitMetadata) {
        this.persistenceUnitMetadata = persistenceUnitMetadata;
    }
}
