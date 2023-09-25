/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.injection.ejb.as1675;

import org.jboss.ws.api.annotation.WebContext;

import jakarta.ejb.Stateless;
import jakarta.jws.WebService;

/**
 * EJB3 bean published as WebService injecting other EJB3 bean and resources.
 *
 * @author <a href="mailto:richard.opalka@jboss.org">Richard Opalka</a>
 */
@Stateless
@WebService(
        name = "EJB3",
        serviceName = "EJB3Service",
        targetNamespace = "http://jbossws.org/as1675",
        endpointInterface = "org.jboss.as.test.integration.ws.injection.ejb.as1675.EndpointIface"
)
@WebContext(
        urlPattern = "/EJB3Service",
        contextRoot = "/as1675"
)
public class EJB3Bean extends AbstractEndpointImpl {
    public String echo(String msg) {
        return super.echo(msg) + ":EJB3Bean";
    }
}
