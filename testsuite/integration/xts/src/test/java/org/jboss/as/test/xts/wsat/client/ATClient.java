/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.wsat.client;

import io.undertow.util.NetworkUtils;

import java.net.URL;

import javax.xml.namespace.QName;
import jakarta.xml.ws.Service;

import org.jboss.as.test.xts.wsat.service.AT;
import org.jboss.logging.Logger;

/**
 * Atomic transaction client
 */
public class ATClient {
    private static final Logger log = Logger.getLogger(ATClient.class);

    private static final String NODE0_ADDR = NetworkUtils.formatPossibleIpv6Address(System.getProperty("node0", "localhost"));
    // parametrize this one day in the future?
    private static final int NODE0_PORT = 8080;

    private static final String TARGET_NAMESPACE = "http://www.jboss.com/jbossas/test/xts/wsat/at/";
    private static final String DEFAULT_PORT_NAME = "AT";

    public static AT newInstance(String serviceNamespaceName) throws Exception {
        return ATClient.newInstance(serviceNamespaceName, serviceNamespaceName);
    }

    public static AT newInstance(String serviceUrl, String serviceNamespaceName) throws Exception {

        URL wsdlLocation = new URL("http://" + NODE0_ADDR + ":" + NODE0_PORT + "/" + ATTestCase.ARCHIVE_NAME + "/" + serviceUrl + "?wsdl");
        log.trace("wsdlLocation for service: " + wsdlLocation);
        QName serviceName = new QName(TARGET_NAMESPACE, serviceNamespaceName);
        QName portName = new QName(TARGET_NAMESPACE, DEFAULT_PORT_NAME);

        Service service = Service.create(wsdlLocation, serviceName);

        AT atService = service.getPort(portName, AT.class);

        return atService;
    }
}

