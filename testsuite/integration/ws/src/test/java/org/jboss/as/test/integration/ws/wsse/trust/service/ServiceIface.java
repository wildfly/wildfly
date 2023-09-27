/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsse.trust.service;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService
        (
                targetNamespace = "http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy"
        )
public interface ServiceIface {
    @WebMethod
    String sayHello();
}
