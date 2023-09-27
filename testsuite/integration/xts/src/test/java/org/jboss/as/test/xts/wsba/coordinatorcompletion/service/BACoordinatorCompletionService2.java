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

@WebService(serviceName = "BACoordinatorCompletionService2",
        portName = "BACoordinatorCompletion", name = "BACoordinatorCompletion",
        targetNamespace = "http://www.jboss.com/jbossas/test/xts/ba/coordinatorcompletion/")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@HandlerChain(file = "/context-handlers.xml")
@WebServlet(name = "BACoordinatorCompletionService2", urlPatterns = {"/BACoordinatorCompletionService2"})
public class BACoordinatorCompletionService2 extends BACoordinatorCompletionSuperService {
    public static final String SERVICE_EVENTLOG_NAME = "bacoordinator_completition_service2";

    @WebMethod
    public void saveData(ServiceCommand... serviceCommands) throws TestApplicationException {
        super.saveData(SERVICE_EVENTLOG_NAME, serviceCommands);
    }
}
