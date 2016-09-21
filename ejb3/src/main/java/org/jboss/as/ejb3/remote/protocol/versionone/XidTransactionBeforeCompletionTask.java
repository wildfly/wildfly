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

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.ejb.client.XidTransactionID;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.tm.ImportedTransaction;

/**
 * @author Jaikiran Pai
 */
class XidTransactionBeforeCompletionTask extends XidTransactionManagementTask {

    XidTransactionBeforeCompletionTask(final TransactionRequestHandler txRequestHandler, final EJBRemoteTransactionsRepository transactionsRepository,
                                       final MarshallerFactory marshallerFactory, final XidTransactionID xidTransactionID,
                                       final ChannelAssociation channelAssociation, final short invocationId) {
        super(txRequestHandler, transactionsRepository, marshallerFactory, xidTransactionID, channelAssociation, invocationId);
    }

    @Override
    protected void manageTransaction() throws Throwable {
        final ImportedTransaction subordinateTransaction = this.transactionsRepository.getImportedTransaction(this.xidTransactionID);
        if (subordinateTransaction == null) {
            throw EjbLogger.ROOT_LOGGER.noSubordinateTransactionPresentForXid(this.xidTransactionID.getXid());
        }
        // first associate the tx on this thread, by resuming the tx
        this.resumeTransaction(subordinateTransaction);
        try {
            // invoke the beforeCompletion
            // Courtesy: com.arjuna.ats.internal.jta.transaction.arjunacore.jca.XATerminatorImple
            // do beforeCompletion()
            subordinateTransaction.doBeforeCompletion();
        } finally {
            // disassociate the tx that was associated (resumed) on this thread.
            // This needs to be done explicitly because the SubOrdinationManager isn't responsible
            // for clearing the tx context from the thread
            this.transactionsRepository.getTransactionManager().suspend();
        }
    }


}
