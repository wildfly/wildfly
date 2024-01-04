/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.context.as1605;

import jakarta.ejb.Stateless;
import jakarta.jws.WebService;

/**
 * EJB3 endpoint.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@Stateless
@WebService(
        endpointInterface = "org.jboss.as.test.integration.ws.context.as1605.EndpointIface",
        targetNamespace = "org.jboss.as.test.integration.ws.context.as1605"
)
public class EJB3Endpoint {

    public String echo(final String s) {
        return "EJB3 " + s;
    }

}
