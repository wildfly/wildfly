/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.multinode.transaction.async;

import java.util.concurrent.Future;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Bean invoked asynchronously where {@link TransactionAttributeType#MANDATORY} suggests
 * that call to this bean will fail.
 *
 * @author Ondrej Chaloupka
 */
@Stateless
@Asynchronous
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class TransactionalMandatory implements TransactionalRemote {

    public Future<Integer> transactionStatus() {
        return new AsyncResult<Integer>(-1);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Future<Integer> asyncWithRequired() {
        throw new RuntimeException("Throw RuntimeException on purpose to cause the transaction rollback");
    }
}
