/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.multinode.transaction;

import jakarta.annotation.Resource;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.transaction.TransactionSynchronizationRegistry;

/**
 * @author Stuart Douglas
 * @author Ivo Studensky
 */
@Remote(TransactionalRemote.class)
@Stateless
public class TransactionalStatelessBean implements TransactionalRemote {

    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public int transactionStatus() {
        return transactionSynchronizationRegistry.getTransactionStatus();
    }

}
