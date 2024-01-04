/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.authentication;

import jakarta.jws.WebService;

/**
 * Simple POJO endpoint
 *
 * @author Rostislav Svoboda
 */
@WebService(
        serviceName = "POJOAuthService",
        targetNamespace = "http://jbossws.org/authentication",
        endpointInterface = "org.jboss.as.test.integration.ws.authentication.PojoEndpointIface"
)
public class PojoEndpoint implements PojoEndpointIface {

    public String hello(String input) {
        return "Hello " + input + "!";
    }
}
