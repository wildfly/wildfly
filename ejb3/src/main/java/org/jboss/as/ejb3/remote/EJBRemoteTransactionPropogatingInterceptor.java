/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ejb3.remote;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinationManager;
import org.jboss.ejb.client.TransactionID;
import org.jboss.ejb.client.UserTransactionID;
import org.jboss.ejb.client.XidTransactionID;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

/**
 * An interceptor which is responsible for identifying any remote transaction associated with the invocation
 * and propagating that transaction during the remaining part of the invocation
 *
 * @author Jaikiran Pai
 */
class EJBRemoteTransactionPropogatingInterceptor implements Interceptor {

    /**
     * Remote transactions repository
     */
    private final EJBRemoteTransactionsRepository ejbRemoteTransactionsRepository;

    EJBRemoteTransactionPropogatingInterceptor(final EJBRemoteTransactionsRepository ejbRemoteTransactionsRepository) {
        this.ejbRemoteTransactionsRepository = ejbRemoteTransactionsRepository;
    }

    /**
     * Processes an incoming invocation and checks for the presence of a remote transaction associated with the
     * invocation context.
     *
     * @param context The invocation context
     * @return
     * @throws Exception
     */
    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final TransactionManager transactionManager = this.ejbRemoteTransactionsRepository.getTransactionManager();
        Transaction originatingRemoteTx = null;
        // get the transaction id attachment
        final TransactionID transactionID = (TransactionID) context.getPrivateData(TransactionID.PRIVATE_DATA_KEY);
        if (transactionID != null) {
            // if it's UserTransaction then create or resume the UserTransaction corresponding to the ID
            if (transactionID instanceof UserTransactionID) {
                this.createOrResumeUserTransaction((UserTransactionID) transactionID);
            } else if (transactionID instanceof XidTransactionID) {
                this.createOrResumeXidTransaction((XidTransactionID) transactionID);
            }
            // the invocation was associated with a remote tx, so keep a flag so that we can
            // suspend (on this thread) the originating tx when returning from the invocation
            originatingRemoteTx = transactionManager.getTransaction();
        }
        try {
            // we are done with any tx propogation setup, let's move on
            return context.proceed();
        } finally {
            // suspend the originating remote tx on this thread now that the invocation has been done
            if (originatingRemoteTx != null) {
                transactionManager.suspend();
            }
        }
    }

    /**
     * Creates or resumes a UserTransaction associated with the passed <code>UserTransactionID</code>.
     * When this method returns successfully, the transaction manager will have the correct user transaction
     * associated with it
     *
     * @param userTransactionID The user transaction id
     * @throws Exception
     */
    private void createOrResumeUserTransaction(final UserTransactionID userTransactionID) throws Exception {
        final TransactionManager transactionManager = this.ejbRemoteTransactionsRepository.getTransactionManager();
        final Transaction alreadyCreatedTx = this.ejbRemoteTransactionsRepository.getTransaction(userTransactionID);
        if (alreadyCreatedTx != null) {
            // resume the already created tx
            transactionManager.resume(alreadyCreatedTx);
        } else {
            // begin a new user transaction and add it to the tx repository
            final UserTransaction userTransaction = this.ejbRemoteTransactionsRepository.getUserTransaction();
            userTransaction.begin();
            // get the tx that just got created and associated with the transaction manager
            final Transaction newlyCreatedTx = transactionManager.getTransaction();
            this.ejbRemoteTransactionsRepository.addTransaction(userTransactionID, newlyCreatedTx);
        }
    }

    private void createOrResumeXidTransaction(final XidTransactionID xidTransactionID) throws Exception {
        final TransactionManager transactionManager = this.ejbRemoteTransactionsRepository.getTransactionManager();
        final Transaction alreadyCreatedTx = this.ejbRemoteTransactionsRepository.getTransaction(xidTransactionID);
        if (alreadyCreatedTx != null) {
            // resume the already created tx
            transactionManager.resume(alreadyCreatedTx);
        } else {
            // begin a new tx and add it to the tx repository
            // TODO: Fix the tx timeout (which currently is passed as 300 seconds)
            // TODO: Also it appears that the TransactionReaper isn't cleared of the ReaperElement,
            // after the subordinate tx is committed/rolledback. @see com.arjuna.ats.internal.jta.transaction.arjunacore.subordinate.SubordinateAtomicAction
            // constructor which accepts the timeout value. The subordinate action is added to the reaper but never removed
            // later
            final Transaction newSubOrdinateTx = SubordinationManager.getTransactionImporter().importTransaction(xidTransactionID.getXid(), 300);
            // associate this tx with the thread
            transactionManager.resume(newSubOrdinateTx);
            this.ejbRemoteTransactionsRepository.addTransaction(xidTransactionID, newSubOrdinateTx);
        }
    }

}
