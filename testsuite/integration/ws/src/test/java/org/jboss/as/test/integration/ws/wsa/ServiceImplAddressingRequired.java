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
package org.jboss.as.test.integration.ws.wsa;

import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.xml.ws.soap.Addressing;

@WebService
        (
                portName = "AddressingServicePort",
                serviceName = "AddressingService",
                targetNamespace = "http://www.jboss.org/jbossws/ws-extensions/wsaddressing",
                endpointInterface = "org.jboss.as.test.integration.ws.wsa.ServiceIface"
        )
@Addressing(enabled = true, required = true)
@HandlerChain(file = "ws-handler.xml")
public class ServiceImplAddressingRequired implements ServiceIface {

    public String sayHello(String name) {
        return "Hello " + name + "!";
    }
}
