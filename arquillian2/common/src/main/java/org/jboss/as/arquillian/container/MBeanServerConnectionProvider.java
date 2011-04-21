/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.container;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * A provider for the JSR160 connection.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 03-Dec-2010
 */
public final class MBeanServerConnectionProvider {

    private static final Logger log = Logger.getLogger(MBeanServerConnectionProvider.class.getName());
    private final InetAddress hostAddr;
    private final int port;

    private JMXConnector jmxConnector;

    public MBeanServerConnectionProvider(InetAddress hostAddr, int port) {
        this.hostAddr = hostAddr;
        this.port = port;
    }

    public MBeanServerConnection getConnection() {
        String host = hostAddr.getHostAddress();
        String urlString = System.getProperty("jmx.service.url", "service:jmx:rmi:///jndi/rmi://" + host + ":" + port
                + "/jmxrmi");
        try {
            if (jmxConnector == null) {
                log.fine("Connecting JMXConnector to: " + urlString);
                JMXServiceURL serviceURL = new JMXServiceURL(urlString);
                jmxConnector = JMXConnectorFactory.connect(serviceURL, null);
            }
            return jmxConnector.getMBeanServerConnection();
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot obtain MBeanServerConnection to: " + urlString, ex);
        }
    }
}