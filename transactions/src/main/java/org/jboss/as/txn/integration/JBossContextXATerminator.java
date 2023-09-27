/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.integration;

import jakarta.resource.spi.XATerminator;
import jakarta.resource.spi.work.Work;
import jakarta.resource.spi.work.WorkCompletedException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.tm.JBossXATerminator;
import org.wildfly.transaction.client.ContextTransactionManager;
import org.wildfly.transaction.client.ImportResult;
import org.wildfly.transaction.client.LocalTransaction;
import org.wildfly.transaction.client.LocalTransactionContext;

import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinationManager;

public class JBossContextXATerminator implements JBossXATerminator {

    private final LocalTransactionContext localTransactionContext;
    private final XATerminator contextXATerminator;
    private final JBossXATerminator jbossXATerminator;

    public JBossContextXATerminator(LocalTransactionContext transactionContext, JBossXATerminator jbossXATerminator) {
        this.localTransactionContext = transactionContext;
        this.contextXATerminator = transactionContext.getXATerminator();
        this.jbossXATerminator = jbossXATerminator;
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        contextXATerminator.commit(xid, onePhase);
    }

    @Override
    public void forget(Xid xid) throws XAException {
        contextXATerminator.forget(xid);
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        return contextXATerminator.prepare(xid);
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        return contextXATerminator.recover(flag);
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        contextXATerminator.rollback(xid);
    }


    /**
     * <p>
     * Interception of register work call to get transaction being imported to wildfly transacton client.
     * <p>
     * For importing a transaction Wildfly transaction client eventually calls {@link SubordinationManager}
     * as Narayana {@link XATerminator}s do. This wrapping then let wildfly transacton client to register the transaction
     * for itself, wildfly transacton client then import transaction to Narayana too and finally this method
     * uses Narayana's {@link XATerminator} to register all {@link Work}s binding.<br>
     * Narayana's {@link XATerminator} tries to import transaction too but as transaction is already
     * imported it just gets instance of transaction already imported via call of wildfly transacton client.
     */
    @Override
    public void registerWork(Work work, Xid xid, long timeout) throws WorkCompletedException {
        try {
            // Jakarta Connectors provides timeout in milliseconds, SubordinationManager expects seconds
            int timeout_seconds = (int) timeout/1000;
            // unlimited timeout for Jakarta Connectors means -1 which fails in wfly client
            if(timeout_seconds <= 0) timeout_seconds = ContextTransactionManager.getGlobalDefaultTransactionTimeout();
            localTransactionContext.findOrImportTransaction(xid, timeout_seconds);
        } catch (XAException xae) {
            throw TransactionLogger.ROOT_LOGGER.cannotFindOrImportInflowTransaction(xid, work, xae);
        }

        jbossXATerminator.registerWork(work, xid, timeout);
    }

    /**
     * <p>
     * Start work gets imported transaction and assign it to current thread.
     * <p>
     * This method mimics behavior of Narayana's {@link JBossXATerminator}.
     */
    @Override
    public void startWork(Work work, Xid xid) throws WorkCompletedException {
        LocalTransaction transaction = null;
        try {
            ImportResult<LocalTransaction> transactionImportResult = localTransactionContext.findOrImportTransaction(xid, 0);
            transaction = transactionImportResult.getTransaction();
            ContextTransactionManager.getInstance().resume(transaction);
        } catch (XAException xae) {
            throw TransactionLogger.ROOT_LOGGER.cannotFindOrImportInflowTransaction(xid, work, xae);
        } catch (InvalidTransactionException ite) {
            throw TransactionLogger.ROOT_LOGGER.importedInflowTransactionIsInactive(xid, work, ite);
        } catch (SystemException se) {
            throw TransactionLogger.ROOT_LOGGER.cannotResumeInflowTransactionUnexpectedError(transaction, work, se);
        }
    }

    /**
     * <p>
     * Suspending transaction and canceling the work.
     * <p>
     * Suspend transaction has to be called on the wildfly transaction manager
     * and the we delegate work cancellation to {@link JBossXATerminator}.<br>
     * First we have to cancel the work for jboss terminator would not work with
     * suspended transaction.
     */
    @Override
    public void endWork(Work work, Xid xid) {
        jbossXATerminator.cancelWork(work, xid);

        try {
            ContextTransactionManager.getInstance().suspend();
        } catch (SystemException se) {
            throw TransactionLogger.ROOT_LOGGER.cannotSuspendInflowTransactionUnexpectedError(work, se);
        }
    }

    /**
     * <p>
     * Calling {@link JBossXATerminator} to cancel the work for us.
     * <p>
     * There should not be need any action to be processed by wildfly transaction client.
     */
    @Override
    public void cancelWork(Work work, Xid xid) {
        jbossXATerminator.cancelWork(work, xid);
    }

}
