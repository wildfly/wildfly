/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsse.trust.holderofkey;

import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.annotations.EndpointProperty;

import jakarta.jws.WebService;

@WebService
        (
                portName = "HolderOfKeyServicePort",
                serviceName = "HolderOfKeyService",
                wsdlLocation = "WEB-INF/wsdl/HolderOfKeyService.wsdl",
                targetNamespace = "http://www.jboss.org/jbossws/ws-extensions/holderofkeywssecuritypolicy",
                endpointInterface = "org.jboss.as.test.integration.ws.wsse.trust.holderofkey.HolderOfKeyIface"
        )
@EndpointProperties(value = {
        @EndpointProperty(key = "ws-security.is-bsp-compliant", value = "false"),
        @EndpointProperty(key = "ws-security.signature.properties", value = "serviceKeystore.properties"),
        @EndpointProperty(key = "ws-security.callback-handler", value = "org.jboss.as.test.integration.ws.wsse.trust.holderofkey.HolderOfKeyCallbackHandler")
})
public class HolderOfKeyImpl implements HolderOfKeyIface {
    public String sayHello() {
        return "Holder-Of-Key WS-Trust Hello World!";
    }
}
