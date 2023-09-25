/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */



package org.jboss.as.test.integration.ejb.transaction.cmt.fail;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

import org.jboss.as.test.integration.transactions.spi.TestLastResource;
import org.jboss.as.test.integration.transactions.TestXAResource;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TxTestUtil;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class InnerBean {

    @Inject
    private TransactionCheckerSingleton checker;

    @Resource(name = "java:jboss/TransactionManager")
    private TransactionManager tm;

    public void innerMethodXA() {
        try {
            TestXAResource xaResource = new TestXAResource(TestXAResource.TestAction.COMMIT_THROW_XAER_RMFAIL, checker);
            TxTestUtil.enlistTestXAResource(tm.getTransaction(), xaResource);
        } catch (SystemException se) {
            throw new IllegalStateException("Can't get transaction from transaction manager: " + tm);
        }
    }

    public void innerMethod2pcXA() {
        try {
            TestXAResource xaResource1 = new TestXAResource(TestXAResource.TestAction.COMMIT_THROW_XAER_RMFAIL, checker);
            TestXAResource xaResource2 = new TestXAResource(checker);
            TxTestUtil.enlistTestXAResource(tm.getTransaction(), xaResource1);
            TxTestUtil.enlistTestXAResource(tm.getTransaction(), xaResource2);
        } catch (SystemException se) {
            throw new IllegalStateException("Can't get transaction from transaction manager: " + tm);
        }
    }

    public void innerMethodLocal() {
        try {
            TestXAResource xaResource = new TestLastResource(TestXAResource.TestAction.COMMIT_THROW_XAER_RMFAIL, checker);
            TxTestUtil.enlistTestXAResource(tm.getTransaction(), xaResource);
        } catch (SystemException se) {
            throw new IllegalStateException("Can't get transaction from transaction manager: " + tm);
        }
    }
}
