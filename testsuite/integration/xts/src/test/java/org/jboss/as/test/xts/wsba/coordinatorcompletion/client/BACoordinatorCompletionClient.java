/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.wsba.coordinatorcompletion.client;


import org.jboss.as.test.xts.wsba.coordinatorcompletion.service.BACoordinatorCompletion;
import org.jboss.logging.Logger;

import io.undertow.util.NetworkUtils;

import javax.xml.namespace.QName;
import jakarta.xml.ws.Service;

import java.net.URL;

/**
 * Coordinator completition client
 */
public class BACoordinatorCompletionClient {
    private static final Logger log = Logger.getLogger(BACoordinatorCompletionClient.class);

    private static final String NODE0_ADDR = NetworkUtils.formatPossibleIpv6Address(System.getProperty("node0", "localhost"));
    private static final int NODE0_PORT = 8080;

    private static final String TARGET_NAMESPACE = "http://www.jboss.com/jbossas/test/xts/ba/coordinatorcompletion/";
    private static final String DEFAULT_PORT_NAME = "BACoordinatorCompletion";

    public static BACoordinatorCompletion newInstance(String serviceNamespaceName) throws Exception {
        return BACoordinatorCompletionClient.newInstance(serviceNamespaceName, serviceNamespaceName);
    }

    public static BACoordinatorCompletion newInstance(String serviceUrl, String serviceNamespaceName) throws Exception {
        URL wsdlLocation = new URL("http://" + NODE0_ADDR + ":" + NODE0_PORT + "/" + BACoordinatorCompletionTestCase.ARCHIVE_NAME + "/" + serviceUrl + "?wsdl");
        log.trace("wsdlLocation for service: " + wsdlLocation);
        QName serviceName = new QName(TARGET_NAMESPACE, serviceNamespaceName);
        QName portName = new QName(TARGET_NAMESPACE, DEFAULT_PORT_NAME);

        Service service = Service.create(wsdlLocation, serviceName);

        BACoordinatorCompletion client = service.getPort(portName, BACoordinatorCompletion.class);

        return client;
    }
}

