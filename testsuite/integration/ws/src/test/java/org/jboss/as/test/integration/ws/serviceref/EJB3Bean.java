/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.serviceref;

import jakarta.ejb.Stateless;
import jakarta.jws.WebService;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@WebService(targetNamespace = "http://www.openuri.org/2004/04/HelloWorld", serviceName = "EndpointService", endpointInterface = "org.jboss.as.test.integration.ws.serviceref.EndpointInterface")
@Stateless
public class EJB3Bean implements EndpointInterface {
    public String echo(final String input) {
        return input;
    }
}
