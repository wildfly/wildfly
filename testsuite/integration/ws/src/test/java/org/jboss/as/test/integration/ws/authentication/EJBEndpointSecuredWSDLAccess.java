/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ws.authentication;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.jws.WebService;

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
@SecurityDomain("ejb3-tests")
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
