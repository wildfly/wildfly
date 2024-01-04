/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsse.trust.bearer;

import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.annotations.EndpointProperty;

import jakarta.jws.WebService;

@WebService
        (
                portName = "BearerServicePort",
                serviceName = "BearerService",
                wsdlLocation = "WEB-INF/wsdl/BearerService.wsdl",
                targetNamespace = "http://www.jboss.org/jbossws/ws-extensions/bearerwssecuritypolicy",
                endpointInterface = "org.jboss.as.test.integration.ws.wsse.trust.bearer.BearerIface"
        )
@EndpointProperties(value = {
        @EndpointProperty(key = "ws-security.signature.properties", value = "serviceKeystore.properties")
})
public class BearerImpl implements BearerIface {
    public String sayHello() {
        return "Bearer WS-Trust Hello World!";
    }
}
