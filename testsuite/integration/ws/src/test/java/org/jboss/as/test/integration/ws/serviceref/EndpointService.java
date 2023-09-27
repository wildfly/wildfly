/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.serviceref;

import java.net.URL;

import javax.xml.namespace.QName;
import jakarta.xml.ws.WebEndpoint;
import jakarta.xml.ws.WebServiceClient;
import jakarta.xml.ws.WebServiceFeature;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@WebServiceClient(name = "EndpointService", targetNamespace = "http://www.openuri.org/2004/04/HelloWorld")
public class EndpointService extends jakarta.xml.ws.Service {
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
