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

import org.jboss.ejb.client.TransactionID;
import org.jboss.ejb.client.UserTransactionID;
import org.jboss.ejb.client.XidTransactionID;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * An interceptor which is responsible for identifying any remote transaction associated with the invocation
 * and propagating that transaction during the remaining part of the invocation
 *
 * @author Jaikiran Pai
 */
class EJBRemoteTransactionPropagatingInterceptor implements Interceptor {

    /**
     * Remote transactions repository
     */
    private final EJBRemoteTransactionsRepository ejbRemoteTransactionsRepository;

    EJBRemoteTransactionPropagatingInterceptor(final EJBRemoteTransactionsRepository ejbRemoteTransactionsRepository) {
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
            // we are done with any tx propagation setup, let's move on
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
        final Transaction alreadyCreatedTx = this.ejbRemoteTransactionsRepository.getUserTransaction(userTransactionID);
        if (alreadyCreatedTx != null) {
            // resume the already created tx
            transactionManager.resume(alreadyCreatedTx);
            return;
        }
        // begin a new user transaction and add it to the tx repository
        this.ejbRemoteTransactionsRepository.beginUserTransaction(userTransactionID);
    }

    private void createOrResumeXidTransaction(final XidTransactionID xidTransactionID) throws Exception {
        final TransactionManager transactionManager = this.ejbRemoteTransactionsRepository.getTransactionManager();
        final Transaction alreadyCreatedTx = this.ejbRemoteTransactionsRepository.getImportedTransaction(xidTransactionID);
        if (alreadyCreatedTx != null) {
            // resume the already created tx
            transactionManager.resume(alreadyCreatedTx);
        } else {
            // begin a new tx and add it to the tx repository
            // TODO: Fix the tx timeout (which currently is passed as 300 seconds)
            final Transaction newSubOrdinateTx = this.ejbRemoteTransactionsRepository.importTransaction(xidTransactionID, 300);
            // associate this tx with the thread
            transactionManager.resume(newSubOrdinateTx);
        }
    }

}
