/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.wsba.participantcompletion.service;

import org.jboss.as.test.xts.base.TestApplicationException;
import org.jboss.as.test.xts.util.ServiceCommand;

import jakarta.jws.HandlerChain;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.servlet.annotation.WebServlet;

@WebService(serviceName = "BAParticipantCompletionService3",
        portName = "BAParticipantCompletion", name = "BAParticipantCompletion",
        targetNamespace = "http://www.jboss.com/jbossas/test/xts/ba/participantcompletion/")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@HandlerChain(file = "/context-handlers.xml")
@WebServlet(name = "BAParticipantCompletionService3", urlPatterns = {"/BAParticipantCompletionService3"})
public class BAParticipantCompletionService3 extends BAParticipantCompletionSuperService {
    public static final String SERVICE_EVENTLOG_NAME = "baparticipant_completition_service3";

    @WebMethod
    public void saveData(ServiceCommand... serviceCommands) throws TestApplicationException {
        super.saveData(SERVICE_EVENTLOG_NAME, serviceCommands);
    }
}
