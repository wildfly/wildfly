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

package org.jboss.as.test.xts.wsat.client;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.handler.Handler;

import org.apache.log4j.Logger;
import org.jboss.as.arquillian.container.NetworkUtils;
import org.jboss.as.test.xts.wsat.service.AT;
import com.arjuna.mw.wst11.client.JaxWSHeaderContextProcessor;

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
        log.info("wsdlLocation for service: " + wsdlLocation);
        QName serviceName = new QName(TARGET_NAMESPACE, serviceNamespaceName);
        QName portName = new QName(TARGET_NAMESPACE, DEFAULT_PORT_NAME);

        Service service = Service.create(wsdlLocation, serviceName);
       
        AT atService = service.getPort(portName, AT.class);

        /*
        * Add client handler chain
        */
        BindingProvider bindingProvider = (BindingProvider) atService;
        List<Handler> handlers = new ArrayList<Handler>(1);
        handlers.add(new JaxWSHeaderContextProcessor());
        bindingProvider.getBinding().setHandlerChain(handlers);
        
        return atService;
    }
}

