/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.remote.bean;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.transaction.TransactionManager;

import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TxTestUtil;

@Stateless
@Remote(Incrementor.class)
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class StatelessTransactionBean extends IncrementorBean {
    @EJB
    private TransactionCheckerSingleton txnChecker;

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager tm;

    @Override
    public Result<Integer> increment() {
        TxTestUtil.addSynchronization(tm, txnChecker);
        TxTestUtil.enlistTestXAResource(tm, txnChecker);
        return super.increment();
    }
}
