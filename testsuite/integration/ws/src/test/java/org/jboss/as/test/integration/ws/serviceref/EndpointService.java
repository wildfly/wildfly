/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ws.serviceref;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@WebServiceClient(name = "EndpointService", targetNamespace = "http://www.openuri.org/2004/04/HelloWorld")
public class EndpointService extends javax.xml.ws.Service {
    private static final QName TEST_ENDPOINT_PORT = new QName("http://www.openuri.org/2004/04/HelloWorld", "EJB3BeanPort");

    public EndpointService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public EndpointService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    @WebEndpoint(name = "EJB3BeanPort")
    public EndpointInterface getEndpointPort() {
        return (EndpointInterface) super.getPort(TEST_ENDPOINT_PORT, EndpointInterface.class);
    }
}
