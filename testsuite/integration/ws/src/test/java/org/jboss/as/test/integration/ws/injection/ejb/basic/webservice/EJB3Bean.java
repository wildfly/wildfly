/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.injection.ejb.basic.webservice;

import org.jboss.ws.api.annotation.WebContext;

import jakarta.ejb.Stateless;
import jakarta.jws.HandlerChain;
import jakarta.jws.WebService;

/**
 * EJB3 bean published as WebService injecting other EJB3 bean.
 *
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
@WebService(
        name = "EJB3",
        serviceName = "EJB3Service",
        targetNamespace = "http://jbossws.org/injection",
        endpointInterface = "org.jboss.as.test.integration.ws.injection.ejb.basic.webservice.EndpointIface"
)
@WebContext(
        urlPattern = "/EJB3Service",
        contextRoot = "/jaxws-injection-ejb3"
)
@HandlerChain(file = "jaxws-handler.xml")
@Stateless
public class EJB3Bean extends AbstractEndpointImpl {

    public String echo(String msg) {
        return super.echo(msg) + ":EJB3Bean";
    }

}
