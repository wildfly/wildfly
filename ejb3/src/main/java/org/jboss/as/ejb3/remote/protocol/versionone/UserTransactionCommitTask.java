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

package org.jboss.as.ejb3.remote.protocol.versionone;

import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.ejb.client.UserTransactionID;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.remoting3.Channel;

import javax.transaction.Transaction;

/**
 * @author Jaikiran Pai
 */
class UserTransactionCommitTask extends UserTransactionManagementTask {

    UserTransactionCommitTask(final TransactionRequestHandler transactionRequestHandler, final EJBRemoteTransactionsRepository transactionsRepository,
                              final MarshallerFactory marshallerFactory, final UserTransactionID userTransactionID,
                              final Channel channel, final short invocationId) {
        super(transactionRequestHandler, transactionsRepository, marshallerFactory, userTransactionID, channel, invocationId);
    }

    @Override
    protected void manageTransaction() throws Throwable {
        final Transaction transaction = this.transactionsRepository.removeTransaction(this.userTransactionID);
        this.resumeTransaction(transaction);
        this.transactionsRepository.getTransactionManager().commit();
    }
}
