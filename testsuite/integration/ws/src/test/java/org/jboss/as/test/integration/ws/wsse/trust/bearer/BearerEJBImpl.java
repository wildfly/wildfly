/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ws.wsse.trust.bearer;
import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.annotations.EndpointProperty;
import org.jboss.as.test.integration.ws.wsse.trust.ContextProvider;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

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