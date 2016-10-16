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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;
import java.lang.management.ManagementFactory;
import java.net.SocketPermission;
import java.rmi.registry.LocateRegistry;
import java.util.Properties;


import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * Tests that JMX Connector work properly from container.
 *
 * @author baranowb
 * @author Eduardo Martins
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JMXConnectorTestCase {
    // NOTE: this test may fail on Ubuntu, since it has definition of localhost as 127.0.1.1
    private static final Logger log = Logger.getLogger(JMXConnectorTestCase.class.getName());

    private static final String CB_DEPLOYMENT_NAME = "naming-connector-bean"; // module

    private JMXConnectorServer connectorServer;
    private final int port = 11090;
    private JMXServiceURL jmxServiceURL;
    private String rmiServerJndiName;

    @ArquillianResource
    private ManagementClient managementClient;

    @Before
    public void beforeTest() throws Exception {
        LocateRegistry.createRegistry(port);
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final String address = managementClient.getMgmtAddress();
        rmiServerJndiName = "rmi://" + address + ":" + port + "/jmxrmi";
        jmxServiceURL = new JMXServiceURL("service:jmx:rmi:///jndi/"+rmiServerJndiName);
        connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(jmxServiceURL, null, mbs);
        connectorServer.start();
    }

    @After
    public void afterTest() throws Exception {
        if (connectorServer != null) {
            connectorServer.stop();
        }
    }

    @Deployment
    public static JavaArchive createTestArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CB_DEPLOYMENT_NAME);
        archive.addClass(ConnectedBean.class);
        archive.addClass(ConnectedBeanInterface.class);
        archive.addAsManifestResource(createPermissionsXmlAsset(new SocketPermission("*:*", "connect,resolve")), "permissions.xml");
        return archive;
    }

    @Test
    public void testMBeanCount() throws Exception {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, org.jboss.naming.remote.client.InitialContextFactory.class.getName());
        env.put(Context.PROVIDER_URL, managementClient.getRemoteEjbURL().toString());
        env.put("jboss.naming.client.ejb.context", true);
        env.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");
        final javax.naming.InitialContext initialContext = new javax.naming.InitialContext(env);
        try {
            ConnectedBeanInterface connectedBean = (ConnectedBeanInterface) initialContext.lookup(CB_DEPLOYMENT_NAME + "/" + ConnectedBean.class.getSimpleName() + "!" + ConnectedBeanInterface.class.getName());
            int mBeanCountFromJNDI = connectedBean.getMBeanCountFromJNDI(rmiServerJndiName);
            log.trace("MBean server count from jndi: " + mBeanCountFromJNDI);
            int mBeanCountFromConnector = connectedBean.getMBeanCountFromConnector(jmxServiceURL);
            log.trace("MBean server count from connector: " + mBeanCountFromConnector);
            Assert.assertEquals(mBeanCountFromConnector, mBeanCountFromJNDI);
        } finally {
            initialContext.close();
        }
    }

}