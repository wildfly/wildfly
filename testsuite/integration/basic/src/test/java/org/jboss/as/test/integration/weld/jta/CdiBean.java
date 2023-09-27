/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.jta;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CdiBean {

    @Resource
    TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    public boolean isTransactionSynchronizationRegistryInjected() {
        return transactionSynchronizationRegistry != null;
    }

    @Transactional
    public boolean isTransactionActive() {
        return transactionSynchronizationRegistry.getTransactionStatus() == Status.STATUS_ACTIVE;
    }

    public boolean isTransactionInactive() {
        return transactionSynchronizationRegistry.getTransactionStatus() == Status.STATUS_NO_TRANSACTION;
    }
}
