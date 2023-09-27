/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.tx.timeout;

import jakarta.annotation.Resource;
import jakarta.transaction.TransactionManager;

import org.wildfly.transaction.client.LocalTransaction;

/**
 * @author Tomasz Adamski
 */
public abstract class AbstractTxBean {

    @Resource(lookup = "java:jboss/TransactionManager")
    private TransactionManager transactionManager;

    private LocalTransaction transaction;

    protected int checkTimeoutValue() {
        try {
            transaction = (LocalTransaction) transactionManager.getTransaction();
            return transaction.getTransactionTimeout();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
