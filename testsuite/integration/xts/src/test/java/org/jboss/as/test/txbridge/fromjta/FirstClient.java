/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.txbridge.fromjta;

import javax.xml.namespace.QName;
import jakarta.xml.ws.Service;

import org.jboss.as.test.txbridge.fromjta.service.FirstServiceAT;

import java.net.URL;

public class FirstClient {

    public static FirstServiceAT newInstance() throws Exception {
        URL wsdlLocation = new URL("http://localhost:8080/test/FirstServiceATService/FirstServiceAT?wsdl");
        QName serviceName = new QName("http://www.jboss.com/jbossas/test/txbridge/fromjta/first", "FirstServiceATService");
        QName portName = new QName("http://www.jboss.com/jbossas/test/txbridge/fromjta/first", "FirstServiceAT");

        Service service = Service.create(wsdlLocation, serviceName);
        FirstServiceAT client = service.getPort(portName, FirstServiceAT.class);

        return client;
    }
}

