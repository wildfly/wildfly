/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.wsba.coordinatorcompletion.service;

import org.jboss.as.test.xts.base.TestApplicationException;
import org.jboss.as.test.xts.util.ServiceCommand;

import jakarta.jws.HandlerChain;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.servlet.annotation.WebServlet;

@WebService(serviceName = "BACoordinatorCompletionService3",
        portName = "BACoordinatorCompletion", name = "BACoordinatorCompletion",
        targetNamespace = "http://www.jboss.com/jbossas/test/xts/ba/coordinatorcompletion/")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@HandlerChain(file = "/context-handlers.xml")
@WebServlet(name = "BACoordinatorCompletionService3", urlPatterns = {"/BACoordinatorCompletionService3"})
public class BACoordinatorCompletionService3 extends BACoordinatorCompletionSuperService {
    public static final String SERVICE_EVENTLOG_NAME = "bacoordinator_completition_service3";

    @WebMethod
    public void saveData(ServiceCommand... serviceCommands) throws TestApplicationException {
        super.saveData(SERVICE_EVENTLOG_NAME, serviceCommands);
    }
}
