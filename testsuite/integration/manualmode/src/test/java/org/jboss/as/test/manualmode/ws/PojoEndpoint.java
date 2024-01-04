/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.ws;

import jakarta.jws.WebService;
import jakarta.xml.ws.BindingType;

/**
 * Simple POJO endpoint
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a>
 */
@WebService(
        serviceName = "POJOService",
        targetNamespace = "http://jbossws.org/basic",
        endpointInterface = "org.jboss.as.test.manualmode.ws.EndpointIface"
        )
@BindingType(jakarta.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class PojoEndpoint implements EndpointIface {

    public String helloString(String input) {
        return "Hello " + input + "!";
    }
}
