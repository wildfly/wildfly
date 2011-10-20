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
import org.jboss.ejb.client.remoting.RemotingAttachments;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

/**
 * User: jpai
 */
class EJBRemoteTransactionPropogatingInterceptor implements Interceptor {

    private final EJBRemoteTransactionsRepository ejbRemoteTransactionsRepository;

    EJBRemoteTransactionPropogatingInterceptor(final EJBRemoteTransactionsRepository ejbRemoteTransactionsRepository) {
        this.ejbRemoteTransactionsRepository = ejbRemoteTransactionsRepository;
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final RemotingAttachments remotingAttachments = context.getPrivateData(RemotingAttachments.class);
        final TransactionManager transactionManager = this.ejbRemoteTransactionsRepository.getTransactionManager();
        Transaction originatingRemoteTx = null;
        if (remotingAttachments != null) {
            // get the transaction attachment
            final byte[] transactionIDBytes = remotingAttachments.getPayloadAttachment(0x0001);
            // A (remote) tx is associated with the invocation, so propogate it appropriately
            if (transactionIDBytes != null) {
                final TransactionID transactionID = TransactionID.createTransactionID(transactionIDBytes);
                // if it's UserTransaction then create or resume the UserTransaction corresponding to the ID
                if (transactionID instanceof UserTransactionID) {
                    this.createOrResumeUserTransaction((UserTransactionID) transactionID);
                }
                // the invocation was associated with a remote tx, so keep track of the originating tx
                originatingRemoteTx = transactionManager.getTransaction();
            }
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

}
