/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.wsat.service;

import org.jboss.as.test.xts.base.TestApplicationException;
import org.jboss.as.test.xts.util.ServiceCommand;

import jakarta.jws.HandlerChain;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.servlet.annotation.WebServlet;


@WebService(serviceName = "ATService2", portName = "AT", name = "AT", targetNamespace = "http://www.jboss.com/jbossas/test/xts/wsat/at/")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@HandlerChain(file = "/context-handlers.xml")
@WebServlet(name = "ATService2", urlPatterns = {"/ATService2"})
public class ATService2 extends ATSuperService {
    public static final String LOG_NAME = "service2";

    @WebMethod
    public void invoke(ServiceCommand... serviceCommands) throws TestApplicationException {
        super.invokeWithCallName(LOG_NAME, serviceCommands);
    }
}
