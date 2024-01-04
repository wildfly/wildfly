/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.injection.ejb.basic.webservice;

import jakarta.jws.HandlerChain;
import jakarta.jws.WebService;

/**
 * POJO bean published as WebService injecting other EJB3 bean.
 *
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
@WebService(
        name = "POJO",
        serviceName = "POJOService",
        targetNamespace = "http://jbossws.org/injection",
        endpointInterface = "org.jboss.as.test.integration.ws.injection.ejb.basic.webservice.EndpointIface"
)
@HandlerChain(file = "jaxws-handler.xml")
public class POJOBean extends AbstractEndpointImpl {

    public String echo(String msg) {
        return super.echo(msg) + ":POJOBean";
    }

}
