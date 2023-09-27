/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsse;

import jakarta.ejb.Stateless;
import jakarta.jws.WebService;

import org.jboss.ws.api.annotation.EndpointConfig;
import org.jboss.ws.api.annotation.WebContext;

@Stateless
@WebService
        (
                portName = "EJBEncryptSecurityServicePort",
                serviceName = "EJBEncryptSecurityService",
                wsdlLocation = "META-INF/wsdl/SecurityService.wsdl",
                targetNamespace = "http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy",
                endpointInterface = "org.jboss.as.test.integration.ws.wsse.ServiceIface"
        )
@WebContext(
        urlPattern = "/EJBEncryptSecurityService"
)
@EndpointConfig(configFile = "META-INF/jaxws-endpoint-config.xml", configName = "Custom WS-Security Endpoint")
public class EJBEncryptServiceImpl implements ServiceIface {

    public String sayHello() {
        return "Secure Hello World!";
    }
}
