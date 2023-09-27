/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.suspend.wsba;

import java.util.List;

import jakarta.jws.HandlerChain;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

import com.arjuna.mw.wst11.BusinessActivityManager;
import com.arjuna.wst11.BAParticipantManager;
import org.jboss.as.test.xts.suspend.RemoteService;
import org.jboss.logging.Logger;

import com.arjuna.ats.arjuna.common.Uid;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@WebService(targetNamespace = "org.jboss.as.test.xts.suspend", serviceName = "RemoteService", portName = "RemoteService")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@HandlerChain(file = "/context-handlers.xml")
public class BusinessActivityRemoteService implements RemoteService {

    private static final Logger LOGGER = Logger.getLogger(BusinessActivityRemoteService.class);

    @Override
    public void execute() throws Exception {
        LOGGER.debugf("trying to enlist participant to the business activity %s",
                BusinessActivityManager.getBusinessActivityManager().currentTransaction());

        String participantId = new Uid().stringForm();
        BusinessActivityParticipant businessActivityParticipant = new BusinessActivityParticipant(participantId);
        BAParticipantManager participantManager = BusinessActivityManager.getBusinessActivityManager()
                .enlistForBusinessAgreementWithParticipantCompletion(businessActivityParticipant,
                        businessActivityParticipant.getId());
        participantManager.completed();

        LOGGER.debugf("enlisted participant %s", businessActivityParticipant);
    }

    @Override
    public List<String> getParticipantInvocations() {
        return BusinessActivityParticipant.getInvocations();
    }

    @Override
    public void reset() {
        BusinessActivityParticipant.resetInvocations();
    }

}
