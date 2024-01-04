/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.authentication;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.jws.WebService;

import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.ws.api.annotation.TransportGuarantee;
import org.jboss.ws.api.annotation.AuthMethod;
import org.jboss.ws.api.annotation.WebContext;

/**
 * Simple EJB3 endpoint
 *
 * @author Rostislav Svoboda
 */
@WebService(
        serviceName = "EJB3ServiceForWSDL",
        targetNamespace = "http://jbossws.org/authenticationForWSDL",
        endpointInterface = "org.jboss.as.test.integration.ws.authentication.EJBEndpointIface"
)
@WebContext(
        urlPattern = "/EJB3ServiceForWSDL",
        contextRoot = "/jaxws-authentication-ejb3-for-wsdl",
        authMethod = AuthMethod.BASIC,
        transportGuarantee = TransportGuarantee.NONE,
        secureWSDLAccess = true
)
@SecurityDomain("other")
@RolesAllowed("Role1")
@Stateless
public class EJBEndpointSecuredWSDLAccess implements EJBEndpointIface {

    public String hello(String input) {
        return "Hello " + input + "!";
    }

    public String helloForAll(String input) {
        return "Hello " + input + "!";
    }

    public String helloForNone(String input) {
        return "Hello " + input + "!";
    }

    public String helloForRole(String input) {
        return "Hello " + input + "!";
    }

    public String helloForRoles(String input) {
        return "Hello " + input + "!";
    }
}
