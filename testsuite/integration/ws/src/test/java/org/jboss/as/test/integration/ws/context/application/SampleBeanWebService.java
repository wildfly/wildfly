/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.context.application;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@Stateless
@LocalBean
@WebService
public class SampleBeanWebService {
    @WebMethod
    public String sayHello(String name) {
        return "Hello " + name + ".";
    }
}
