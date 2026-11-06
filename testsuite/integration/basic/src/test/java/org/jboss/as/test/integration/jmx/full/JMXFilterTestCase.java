/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jmx.full;

import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.jmx.sar.TestMBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.xnio.IoUtils;

/**
 * @author Kabir Khan
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class JMXFilterTestCase {

    static JMXConnector connector;
    static MBeanServerConnection connection;

    @Deployment
    public static Archive<?> createDeployment() {
        final JavaArchive sar = ShrinkWrap.create(JavaArchive.class, "test-jmx-sar.sar");
        sar.addClasses(org.jboss.as.test.integration.jmx.sar.Test.class, TestMBean.class);
        sar.addAsManifestResource(JMXFilterTestCase.class.getPackage(), "jboss-service.xml", "jboss-service.xml");

        return sar;
    }

    @ContainerResource
    private ManagementClient managementClient;


    @BeforeEach
    public void initialize() throws Exception {
        connection = setupAndGetConnection();
    }

    @AfterEach
    public void closeConnection() throws Exception {
        IoUtils.safeClose(connector);
    }

    @Test
    public void testFilter() throws Exception {

        // Check the non-management JMX domain
        final ObjectName sarMbeanName = new ObjectName("jboss:name=test-sar-1234567890,type=jmx-sar");
        Set<ObjectName> names = connection.queryNames(new ObjectName("*:name=test-sar-1234567890,*"), null);
        Assertions.assertEquals(1, names.size());
        Assertions.assertTrue(names.contains(sarMbeanName));
        // Check that names with no pattern work
        names = connection.queryNames(sarMbeanName, null);
        Assertions.assertEquals(1, names.size());
        Assertions.assertTrue(names.contains(sarMbeanName));
        // Check getMBeanInfo
        Assertions.assertNotNull(connection.getMBeanInfo(sarMbeanName));

        // Check the management JMX domains
        final ObjectName asMbeanName = new ObjectName("jboss.as:subsystem=sar");
        final ObjectName asExprMbeanName = new ObjectName("jboss.as.expr:subsystem=sar");
        names = connection.queryNames(new ObjectName("*:subsystem=sar,*"), null);
        Assertions.assertEquals(4, names.size(), names.toString());
        Assertions.assertTrue(names.contains(asExprMbeanName));
        Assertions.assertTrue(names.contains(asMbeanName));
        Assertions.assertTrue(names.contains(new ObjectName("jboss.as.expr:extension=org.jboss.as.sar,subsystem=sar")));
        Assertions.assertTrue(names.contains(new ObjectName("jboss.as:extension=org.jboss.as.sar,subsystem=sar")));
        // Check that names with no pattern work
        names = connection.queryNames(asMbeanName, null);
        Assertions.assertEquals(1, names.size());
        Assertions.assertTrue(names.contains(asMbeanName));
        names = connection.queryNames(asExprMbeanName, null);
        Assertions.assertEquals(1, names.size());
        Assertions.assertTrue(names.contains(asExprMbeanName));
        // Check getMBeanInfo
        Assertions.assertNotNull(connection.getMBeanInfo(asMbeanName));
        Assertions.assertNotNull(connection.getMBeanInfo(asExprMbeanName));
    }

    private MBeanServerConnection setupAndGetConnection() throws Exception {
        // Make sure that we can connect to the MBean server
        String urlString = System
                .getProperty("jmx.service.url", "service:jmx:http-remoting-jmx://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort());
        JMXServiceURL serviceURL = new JMXServiceURL(urlString);
        connector = JMXConnectorFactory.connect(serviceURL, null);
        return connector.getMBeanServerConnection();
    }
}
