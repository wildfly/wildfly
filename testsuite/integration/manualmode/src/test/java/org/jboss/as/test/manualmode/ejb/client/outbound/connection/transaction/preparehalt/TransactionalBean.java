/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ejb.client.outbound.connection.transaction.preparehalt;

import org.jboss.as.test.integration.transactions.PersistentTestXAResource;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

/**
 * Bean expected to be called by {@link ClientBean}.
 * Working with {@link javax.transaction.xa.XAResource}s to force the transaction manager to process
 * two-phase commit after the end of the business method.
 */
@Stateless
public class TransactionalBean implements TransactionalRemote {
    private static final Logger log = Logger.getLogger(TransactionalBean.class);

    @EJB
    private TransactionCheckerSingleton transactionCheckerSingleton;

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager tm;

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void enlistOnePersistentXAResource() {
        try {
            log.debugf("Invocation to #enlistOnePersistentXAResource with transaction", tm.getTransaction());
            tm.getTransaction().enlistResource(new PersistentTestXAResource(transactionCheckerSingleton));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot enlist single " + PersistentTestXAResource.class.getSimpleName()
                    + " to transaction", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void intermittentCommitFailure() {
        try {
            Transaction txn = tm.getTransaction();
            log.debugf("Invocation to #intermittentCommitFailure with the transaction: %s", txn);
            txn.enlistResource(new TestCommitFailureXAResource(transactionCheckerSingleton));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot enlist single " + TestCommitFailureXAResource.class.getSimpleName()
                    + " to the transaction", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void intermittentCommitFailureTwoPhase() {
        try {
            Transaction txn = tm.getTransaction();
            log.debugf("Invocation to #intermittentCommitFailure with transaction: %s", txn);
            txn.enlistResource(new TestCommitFailureXAResource(transactionCheckerSingleton));
            txn.enlistResource(new PersistentTestXAResource(transactionCheckerSingleton));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot enlist one of the XAResources " + TestCommitFailureXAResource.class.getSimpleName()
                    + " or " + PersistentTestXAResource.class.getSimpleName() + " to the transaction", e);
        }
    }
}
