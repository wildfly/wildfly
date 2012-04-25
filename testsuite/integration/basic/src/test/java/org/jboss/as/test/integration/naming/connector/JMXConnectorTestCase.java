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
package org.jboss.as.test.integration.naming.connector;

import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that JMX Connector work properly from container.
 *
 * @author baranowb
 *
 */
@RunWith(Arquillian.class)
public class JMXConnectorTestCase {
    // NOTE: this test may fail on Ubuntu, since it has definition of localhost as 127.0.1.1
    private static final Logger log = Logger.getLogger(JMXConnectorTestCase.class.getName());

    private static final String CB_DEPLOYMENT_NAME = "naming-connector-bean"; // module

    @ArquillianResource
    private ManagementClient managementClient;

    @EJB(mappedName = "java:global/naming-connector-bean/ConnectedBean!org.jboss.as.test.integration.naming.connector.ConnectedBeanInterface")
    private ConnectedBeanInterface connectedBean;

    // ---------- deployment
    // java:global/naming-connector-bean/ConnectedBean!org.jboss.as.test.integration.naming.connector.ConnectedBeanInterface
    @Deployment
    public static JavaArchive createTestArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CB_DEPLOYMENT_NAME);
        archive.addClass(ConnectedBean.class);
        archive.addClass(ConnectedBeanInterface.class);
        // archive.addClass(JMXConnectorTestCase.class);
        archive.addAsManifestResource(new StringAsset("Dependencies: org.jboss.xnio\n"), "MANIFEST.MF");
        log.info(archive.toString(true));
        return archive;
    }

    @Test
    public void testLookup() throws Exception {
        connectedBean.testConnector(getJMXURI());
    }

    private JMXConnectorServer connectorServer;
    private final int port = 11090;
    private String jmxUri;

    @Before
    public void beforeTest() throws Exception {

        LocateRegistry.createRegistry(port);
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        JMXServiceURL url = new JMXServiceURL(getJMXURI());
        connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
        connectorServer.start();
    }

    @After
    public void afterTest() throws Exception {
        if (connectorServer != null) {
            connectorServer.stop();
        }
    }

    private String getJMXURI() {
        if (jmxUri != null) {
            return jmxUri;
        }

        final String address = managementClient.getMgmtAddress();
        final String jmxUri = "service:jmx:rmi:///jndi/rmi://" + address + ":" + port + "/jmxrmi";
        return jmxUri;
    }
}