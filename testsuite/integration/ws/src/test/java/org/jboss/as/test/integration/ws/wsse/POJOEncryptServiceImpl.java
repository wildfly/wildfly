/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsse;

import jakarta.jws.WebService;

import org.jboss.ws.api.annotation.EndpointConfig;

@WebService
        (
                portName = "EncryptSecurityServicePort",
                serviceName = "EncryptSecurityService",
                wsdlLocation = "WEB-INF/wsdl/SecurityService.wsdl",
                targetNamespace = "http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy",
                endpointInterface = "org.jboss.as.test.integration.ws.wsse.ServiceIface"
        )
@EndpointConfig(configFile = "WEB-INF/jaxws-endpoint-config.xml", configName = "Custom WS-Security Endpoint")
public class POJOEncryptServiceImpl implements ServiceIface {

    public String sayHello() {
        return "Secure Hello World!";
    }
}
