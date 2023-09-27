/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsse.trust.bearer;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService
        (
                targetNamespace = "http://www.jboss.org/jbossws/ws-extensions/bearerwssecuritypolicy"
        )
public interface BearerIface {
    @WebMethod
    String sayHello();
}
