/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsrm;

import jakarta.jws.HandlerChain;
import jakarta.jws.Oneway;
import jakarta.jws.WebService;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
@WebService
        (
                name = "ReliableService",
                serviceName = "ReliableService",
                portName = "ReliableServicePort",
                wsdlLocation = "WEB-INF/wsdl/ReliableService.wsdl",
                targetNamespace = "http://www.jboss.org/jbossws/ws-extensions/wsrm"
        )
@HandlerChain(file = "ws-handler.xml")
public class ReliableServiceImpl {

    private static Logger log = Logger.getLogger(ReliableServiceImpl.class);

    @Oneway
    public void writeLogMessage() {
        log.trace("write method was invoked ...");
    }

    public String sayHello(String name) {
        return "Hello " + name + "!";
    }

}
