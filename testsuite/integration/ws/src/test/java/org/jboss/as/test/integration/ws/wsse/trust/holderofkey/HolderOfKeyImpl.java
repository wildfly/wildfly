/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ws.wsse.trust.holderofkey;

import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.annotations.EndpointProperty;

import javax.jws.WebService;

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