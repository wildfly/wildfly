/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ejb.client.outbound.connection.transaction.recovery;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import org.jboss.as.test.integration.transactions.PersistentTestXAResource;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;

@Stateless
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class TransactionalBean implements TransactionalBeanRemote {

    @EJB
    private TransactionCheckerSingleton transactionCheckerSingleton;

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager tm;

    @Override
    public void enlistPersistentXAResource() {
        try {
            Transaction tx = tm.getTransaction();
            tx.enlistResource(new PersistentTestXAResource(transactionCheckerSingleton));
        } catch (Exception e) {
            throw new RuntimeException("Cannot enlist PersistentTestXAResource", e);
        }
    }
}
