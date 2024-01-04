/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.suspend.wsat;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.mw.wst11.TransactionManager;
import org.jboss.as.test.xts.suspend.RemoteService;
import org.jboss.logging.Logger;

import jakarta.jws.HandlerChain;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
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
        LOGGER.debugf("trying to enlist participant to the transaction %s",
                TransactionManager.getTransactionManager().currentTransaction());

        String participantId = new Uid().stringForm();
        TransactionParticipant transactionParticipant = new TransactionParticipant(participantId);
        TransactionManager.getTransactionManager().enlistForVolatileTwoPhase(transactionParticipant,
                transactionParticipant.getId());

        LOGGER.debugf("enlisted participant %s", transactionParticipant);
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
