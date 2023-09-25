/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.xts.annotation.client;

import org.jboss.as.test.xts.annotation.service.TransactionalService;
import org.jboss.logging.Logger;

import javax.xml.namespace.QName;
import jakarta.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class TransactionalClient {

    private static Logger LOG = Logger.getLogger(TransactionalClient.class);

    private static final String NAME = "TransactionalServiceImpl";

    private static final String TARGET_NAMESPACE = "http://service.annotation.xts.test.as.jboss.org/";

    private static final String PORT_NAME = "TransactionalServiceImplPort";

    private static final String SERVICE_NAME = "TransactionalServiceImplService";

    public static TransactionalService newInstance(final String deploymentUrl) throws MalformedURLException {
        LOG.debug("TransactionalClient.newInstance(deploymentUrl = " + deploymentUrl + ")");

        final URL wsdlLocation = new URL(deploymentUrl + "/" + NAME + "?wsdl");

        LOG.debug("wsdlLocation for service: " + wsdlLocation);

        final QName serviceName = new QName(TARGET_NAMESPACE, SERVICE_NAME);
        final QName portName = new QName(TARGET_NAMESPACE, PORT_NAME);

        final Service service = Service.create(wsdlLocation, serviceName);
        final TransactionalService transactionalService = service.getPort(portName, TransactionalService.class);

        return transactionalService;
    }

}
