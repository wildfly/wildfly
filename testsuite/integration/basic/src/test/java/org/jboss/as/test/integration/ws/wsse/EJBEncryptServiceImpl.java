/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ws.wsse;

import javax.ejb.Stateless;
import javax.jws.WebService;
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
        urlPattern = "/EJBEncryptSecurityService",
        contextRoot = "/jaxws-wsse-sign-encrypt"
)
@EndpointConfig(configFile = "META-INF/jaxws-endpoint-config.xml", configName = "Custom WS-Security Endpoint")
public class EJBEncryptServiceImpl implements ServiceIface {

    public String sayHello() {
        return "Secure Hello World!";
    }
}
