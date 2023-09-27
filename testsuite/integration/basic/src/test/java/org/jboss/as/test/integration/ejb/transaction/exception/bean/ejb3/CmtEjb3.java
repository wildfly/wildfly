/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.exception.bean.ejb3;

import jakarta.annotation.Resource;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import org.jboss.as.test.integration.ejb.transaction.exception.TestConfig.TxManagerException;
import org.jboss.as.test.integration.ejb.transaction.exception.TimeoutTestXAResource;
import org.jboss.as.test.integration.ejb.transaction.exception.bean.TestBean;
import org.jboss.as.test.integration.transactions.TestXAResource.TestAction;

@LocalBean
@Remote
@Stateless
public class CmtEjb3 implements TestBean {

    @Resource(name = "java:jboss/TransactionManager")
    private TransactionManager tm;

    @Override
    public void throwRuntimeException() {
        throw new RuntimeException();
    }

    @Override
    public void throwExceptionFromTm(TxManagerException txManagerException) throws Exception {
        Transaction txn = tm.getTransaction();
        switch (txManagerException) {
        case HEURISTIC_CAUSED_BY_XA_EXCEPTION:
            txn.enlistResource(new TimeoutTestXAResource(TestAction.NONE));
            txn.enlistResource(new TimeoutTestXAResource(TestAction.COMMIT_THROW_XAER_RMERR));
            break;
        case HEURISTIC_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION:
            txn.enlistResource(new TimeoutTestXAResource(TestAction.NONE));
            txn.enlistResource(new TimeoutTestXAResource(TestAction.COMMIT_THROW_UNKNOWN_XA_EXCEPTION));
            break;
        case ROLLBACK_CAUSED_BY_XA_EXCEPTION:
            txn.enlistResource(new TimeoutTestXAResource(TestAction.NONE));
            txn.enlistResource(new TimeoutTestXAResource(TestAction.PREPARE_THROW_XAER_RMERR));
            break;
        case ROLLBACK_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION:
            txn.enlistResource(new TimeoutTestXAResource(TestAction.NONE));
            txn.enlistResource(new TimeoutTestXAResource(TestAction.PREPARE_THROW_UNKNOWN_XA_EXCEPTION));
            break;
        default:
            throw new IllegalArgumentException("Unknown type " + txManagerException);
        }

    }

}
