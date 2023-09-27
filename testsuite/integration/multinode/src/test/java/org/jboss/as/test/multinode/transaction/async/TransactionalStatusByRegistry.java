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
import jakarta.transaction.TransactionSynchronizationRegistry;

/**
 * Asynchronously invoked bean where we expect that transaction registry returns
 * no active status for "current" transaction as propagation should not occur.
 *
 * @author Ondrej Chaloupka
 */
@Stateless
@Asynchronous
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class TransactionalStatusByRegistry implements TransactionalRemote {

    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    public Future<Integer> transactionStatus() {
        return new AsyncResult<Integer>(transactionSynchronizationRegistry.getTransactionStatus());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Future<Integer> asyncWithRequired() {
        throw new RuntimeException("Throw RuntimeException on purpose to cause the transaction rollback");
    }
}
