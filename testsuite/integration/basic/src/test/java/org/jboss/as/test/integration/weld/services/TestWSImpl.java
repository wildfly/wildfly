/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.services;

import jakarta.inject.Inject;
import jakarta.jws.WebService;

@WebService(
    serviceName = TestWSImpl.SERVICE_NAME,
    portName = "TestWS",
    targetNamespace = "http://www.jboss.org/jboss/as/test/TestWS",
    endpointInterface = "org.jboss.as.test.integration.weld.services.TestWS")
public class TestWSImpl {

    public static final String SERVICE_NAME = "TestService";

    @Inject
    ContextBean contextBean;

    public String sayHello() {
        return "Hello" + contextBean.getName();
    }
}
