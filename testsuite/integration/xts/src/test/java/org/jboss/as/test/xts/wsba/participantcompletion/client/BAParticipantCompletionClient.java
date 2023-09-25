/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.wsba.participantcompletion.client;

import io.undertow.util.NetworkUtils;

import javax.xml.namespace.QName;
import jakarta.xml.ws.Service;

import org.jboss.as.test.xts.wsba.participantcompletion.service.BAParticipantCompletion;
import org.jboss.logging.Logger;

import java.net.URL;

/**
 * Service implemenation - this implemetation is inherited by annotated web services.
 */
public class BAParticipantCompletionClient {
    private static final Logger log = Logger.getLogger(BAParticipantCompletionClient.class);

    private static final String NODE0_ADDR = NetworkUtils.formatPossibleIpv6Address(System.getProperty("node0", "localhost"));
    private static final int NODE0_PORT = 8080;

    private static final String TARGET_NAMESPACE = "http://www.jboss.com/jbossas/test/xts/ba/participantcompletion/";
    private static final String DEFAULT_PORT_NAME = "BAParticipantCompletion";

    public static BAParticipantCompletion newInstance(String serviceNamespaceName) throws Exception {
        return BAParticipantCompletionClient.newInstance(serviceNamespaceName, serviceNamespaceName);
    }

    public static BAParticipantCompletion newInstance(String serviceUrl, String serviceNamespaceName) throws Exception {
        URL wsdlLocation = new URL("http://" + NODE0_ADDR + ":" + NODE0_PORT + "/" + BAParticipantCompletionTestCase.ARCHIVE_NAME + "/" + serviceUrl + "?wsdl");
        log.trace("wsdlLocation for service: " + wsdlLocation);
        QName serviceName = new QName(TARGET_NAMESPACE, serviceNamespaceName);
        QName portName = new QName(TARGET_NAMESPACE, DEFAULT_PORT_NAME);

        Service service = Service.create(wsdlLocation, serviceName);

        BAParticipantCompletion client = service.getPort(portName, BAParticipantCompletion.class);

        return client;
    }
}

