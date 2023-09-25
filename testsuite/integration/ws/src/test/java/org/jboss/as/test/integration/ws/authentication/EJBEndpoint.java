/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.authentication;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
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
        serviceName = "EJB3AuthService",
        targetNamespace = "http://jbossws.org/authentication",
        endpointInterface = "org.jboss.as.test.integration.ws.authentication.EJBEndpointIface"
)
@WebContext(
        urlPattern = "/EJB3AuthService",
        contextRoot = "/jaxws-authentication-ejb3",
        authMethod = AuthMethod.BASIC,
        transportGuarantee = TransportGuarantee.NONE,
        secureWSDLAccess = false
)
@Stateless
@SecurityDomain("other")
@DeclareRoles({"Role1", "Role2", "guest"})
@RolesAllowed("Role1")
public class EJBEndpoint implements EJBEndpointIface {

    public String hello(String input) {
        return "Hello " + input + "!";
    }

    @PermitAll
    public String helloForAll(String input) {
        return "Hello " + input + "!";
    }

    @DenyAll
    public String helloForNone(String input) {
        return "Hello " + input + "!";
    }

    @RolesAllowed("Role2")
    public String helloForRole(String input) {
        return "Hello " + input + "!";
    }

    @RolesAllowed({"Role1", "Role2"})
    public String helloForRoles(String input) {
        return "Hello " + input + "!";
    }

}
