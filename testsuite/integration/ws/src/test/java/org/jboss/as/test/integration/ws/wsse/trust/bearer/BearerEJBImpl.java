/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsse.trust.bearer;
import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.annotations.EndpointProperty;
import org.jboss.as.test.integration.ws.wsse.trust.ContextProvider;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;

@WebService
        (
                portName = "BearerServicePort",
                serviceName = "BearerService",
                wsdlLocation = "WEB-INF/wsdl/BearerService.wsdl",
                targetNamespace = "http://www.jboss.org/jbossws/ws-extensions/bearerwssecuritypolicy",
                endpointInterface = "org.jboss.as.test.integration.ws.wsse.trust.bearer.BearerIface"
        )
@EndpointProperties(value = {
        @EndpointProperty(key = "ws-security.signature.properties", value = "serviceKeystore.properties"),
})
@org.apache.cxf.interceptor.InInterceptors (interceptors = {"org.jboss.as.test.integration.ws.wsse.trust.SamlSecurityContextInInterceptor" })
public class BearerEJBImpl implements BearerIface {
    @Resource
    WebServiceContext context;

     @EJB
    ContextProvider ejbContext;
    public String sayHello() {
        String wsprincipal =  context.getUserPrincipal().getName();
        return wsprincipal + "&" + ejbContext.getEjbCallerPrincipalName();
    }
}
