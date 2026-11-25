/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.naming.connector;

import static javax.management.remote.rmi.RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE;
import static javax.management.remote.rmi.RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketPermission;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RMISocketFactory;
import java.util.Map;
import java.util.Properties;
import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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

    private static final int port = 11090;

    private JMXConnectorServer connectorServer;
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
        jmxServiceURL = new JMXServiceURL("service:jmx:rmi:///jndi/" +rmiServerJndiName);

        // Use a custom RMISocketFactory to control the interface used by the JMXConnectorServer
        final RMISocketFactory socketFactory = new SocketFactory(address);

        connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(
                jmxServiceURL,
                Map.of(RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, socketFactory, RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, socketFactory),
                mbs
        );
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
        archive.addAsManifestResource(new StringAsset("Dependencies: jdk.naming.rmi\n"), "MANIFEST.MF");
        archive.addAsManifestResource(createPermissionsXmlAsset(
            new RuntimePermission("accessClassInPackage.com.sun.jndi.url.rmi"),
            new SocketPermission("*:*", "connect,resolve")
        ), "permissions.xml");
        return archive;
    }

    @Test
    public void testMBeanCount() throws Exception {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, org.wildfly.naming.client.WildFlyInitialContextFactory.class.getName());
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

    public static final class SocketFactory extends RMISocketFactory implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String address;

        public SocketFactory(String address) {
            this.address = address;
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return RMISocketFactory.getDefaultSocketFactory()
                    .createSocket(address, port);
        }

        @Override
        public ServerSocket createServerSocket(int port) throws IOException {
            return new ServerSocket(port, 5, InetAddress.getByName(address));
        }

    }

}
