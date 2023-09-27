/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.ejb;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.jws.HandlerChain;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;

/**
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
@Stateless
@WebService(
        endpointInterface = "org.jboss.as.test.integration.ws.ejb.ContextHandlerEndpointIface",
        targetNamespace = "org.jboss.as.test.integration.ws.ejb",
        serviceName = "ContextHandlerService"
)

@HandlerChain(file = "handler.xml")
public class ContextHandlerEndpointImpl implements ContextHandlerEndpointIface {

    @Resource
    WebServiceContext wsCtx;

    public String doSomething(String msg) {
        if (!"ContextHandler:handleInbound()".equals(wsCtx.getMessageContext().get("invoked"))) {
            throw new IllegalArgumentException("Wrong webservice context instance or handler not invoked");
        }

        return msg;
    }
}
