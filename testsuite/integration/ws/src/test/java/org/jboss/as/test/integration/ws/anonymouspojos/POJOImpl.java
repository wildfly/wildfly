/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.anonymouspojos;

import jakarta.jws.WebService;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@WebService(
        endpointInterface = "org.jboss.as.test.integration.ws.anonymouspojos.POJOIface",
        targetNamespace = "org.jboss.as.test.integration.ws.anonymouspojos",
        serviceName = "POJOImplService"
)
public class POJOImpl {

    public String echo(final String s) {
        return s + " from POJO";
    }

}
