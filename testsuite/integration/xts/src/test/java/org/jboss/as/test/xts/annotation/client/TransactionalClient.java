/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.xts.annotation.client;

import org.jboss.as.test.xts.annotation.service.TransactionalService;
import org.jboss.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
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
