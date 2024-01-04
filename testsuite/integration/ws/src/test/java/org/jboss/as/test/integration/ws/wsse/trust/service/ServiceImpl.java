/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsse.trust.service;

import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.annotations.EndpointProperty;

import jakarta.jws.WebService;

@WebService
        (
                portName = "SecurityServicePort",
                serviceName = "SecurityService",
                wsdlLocation = "WEB-INF/wsdl/SecurityService.wsdl",
                targetNamespace = "http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy",
                endpointInterface = "org.jboss.as.test.integration.ws.wsse.trust.service.ServiceIface"
        )
@EndpointProperties(value = {
        @EndpointProperty(key = "ws-security.signature.username", value = "myservicekey"),
        @EndpointProperty(key = "ws-security.signature.properties", value = "serviceKeystore.properties"),
        @EndpointProperty(key = "ws-security.encryption.properties", value = "serviceKeystore.properties"),
        @EndpointProperty(key = "ws-security.callback-handler", value = "org.jboss.as.test.integration.ws.wsse.trust.service.ServerCallbackHandler")
})
public class ServiceImpl implements ServiceIface {
    public String sayHello() {
        return "WS-Trust Hello World!";
    }
}
