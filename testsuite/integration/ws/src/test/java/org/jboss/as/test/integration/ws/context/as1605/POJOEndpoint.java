/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.context.as1605;

import jakarta.jws.WebService;

/**
 * POJO endpoint.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@WebService(
        endpointInterface = "org.jboss.as.test.integration.ws.context.as1605.EndpointIface",
        targetNamespace = "org.jboss.as.test.integration.ws.context.as1605"
)
public class POJOEndpoint {

    public String echo(final String s) {
        return "POJO " + s;
    }

}
