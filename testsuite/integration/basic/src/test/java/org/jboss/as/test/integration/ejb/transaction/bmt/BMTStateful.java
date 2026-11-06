/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.transaction.bmt;

import jakarta.annotation.Resource;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import org.junit.jupiter.api.Assertions;

/**
 * Stateful session bean that uses the same transaction over two method invocations
 *
 * @author Stuart Douglas
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class BMTStateful {

    @Resource
    private EJBContext ejbContext;

    public void createTransaction() {
        try {
            final UserTransaction userTransaction = ejbContext.getUserTransaction();
            Assertions.assertEquals(Status.STATUS_NO_TRANSACTION, userTransaction.getStatus());
            userTransaction.begin();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        } catch (NotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public void rollbackTransaction() {
        try {
            final UserTransaction userTransaction = ejbContext.getUserTransaction();
            Assertions.assertEquals(Status.STATUS_ACTIVE, userTransaction.getStatus());
            userTransaction.rollback();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }
}
