/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.multinode.transaction.async;

import java.util.concurrent.Future;
import jakarta.annotation.Resource;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

/**
 * Asynchronously invoked bean where we expect that transaction manager returns
 * no active status for "current" transaction as propagation should not occur.
 *
 * @author Ondrej Chaloupka
 */
@Stateless
public class TransactionalStatusByManager implements TransactionalRemote {

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager txnManager;

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Future<Integer> transactionStatus() {
        try {
            return new AsyncResult<Integer>(txnManager.getStatus());
        } catch (SystemException se) {
            throw new RuntimeException("Can't get transaction status", se);
        }
    }

    @Override
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Future<Integer> asyncWithRequired() {
        throw new RuntimeException("Throw RuntimeException on purpose to cause the transaction rollback");
    }
}
