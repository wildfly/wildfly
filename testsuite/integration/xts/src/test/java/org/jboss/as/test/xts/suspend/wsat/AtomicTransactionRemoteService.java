/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.xts.suspend.wsat;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.mw.wst11.TransactionManager;
import org.jboss.as.test.xts.suspend.RemoteService;
import org.jboss.logging.Logger;

import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.util.List;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@WebService(targetNamespace = "org.jboss.as.test.xts.suspend", serviceName = "RemoteService", portName = "RemoteService")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@HandlerChain(file = "/context-handlers.xml")
public class AtomicTransactionRemoteService implements RemoteService {

    private static final Logger LOGGER = Logger.getLogger(AtomicTransactionRemoteService.class);

    @Override
    public void execute() throws Exception {
        LOGGER.infof("trying to enlist participant to the transaction %s",
                TransactionManager.getTransactionManager().currentTransaction());

        String participantId = new Uid().stringForm();
        TransactionParticipant transactionParticipant = new TransactionParticipant(participantId);
        TransactionManager.getTransactionManager().enlistForVolatileTwoPhase(transactionParticipant,
                transactionParticipant.getId());

        LOGGER.infof("enlisted participant %s", transactionParticipant);
    }

    @Override
    public List<String> getParticipantInvocations() {
        return TransactionParticipant.getInvocations();
    }

    @Override
    public void reset() {
        TransactionParticipant.resetInvocations();
    }

}
