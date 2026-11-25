/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.plugin.spi;

import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

/**
 * IntegrationWithCDIBag
 *
 * @author Scott Marlow
 */
public interface IntegrationWithCDIBag {
    EntityManagerFactory getEntityManagerFactory();

    TransactionSynchronizationRegistry getTransactionSynchronizationRegistry();

    TransactionManager getTransactionManager();
}
