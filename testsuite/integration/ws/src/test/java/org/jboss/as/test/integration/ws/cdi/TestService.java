/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.cdi;

import jakarta.ejb.Stateless;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;

/**
 * Simple web service
 *
 * @author Stuart Douglas
 */
@WebService
@Stateless
public class TestService {
    @WebMethod
    public String echo(@WebParam final String toEcho) {
        return toEcho;
    }

}
