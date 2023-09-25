/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsse.trust.onbehalfof;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

/**
 * User: rsearls@redhat.com
 * Date: 1/26/14
 */
@WebService
        (
                targetNamespace = "http://www.jboss.org/jbossws/ws-extensions/onbehalfofwssecuritypolicy"
        )
public interface OnBehalfOfServiceIface {
    @WebMethod
    String sayHello(String host, String port);
}
