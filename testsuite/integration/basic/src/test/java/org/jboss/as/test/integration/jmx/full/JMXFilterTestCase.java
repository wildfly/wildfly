/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.jmx.full;

import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.jmx.sar.TestMBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

/**
 * @author Kabir Khan
 */
@RunWith(Arquillian.class)
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


    @Before
    public void initialize() throws Exception {
        connection = setupAndGetConnection();
    }

    @After
    public void closeConnection() throws Exception {
        IoUtils.safeClose(connector);
    }

    @Test
    public void testFilter() throws Exception {

        // Check the non-management JMX domain
        final ObjectName sarMbeanName = new ObjectName("jboss:name=test-sar-1234567890,type=jmx-sar");
        Set<ObjectName> names = connection.queryNames(new ObjectName("*:name=test-sar-1234567890,*"), null);
        Assert.assertEquals(1, names.size());
        Assert.assertTrue(names.contains(sarMbeanName));
        // Check that names with no pattern work
        names = connection.queryNames(sarMbeanName, null);
        Assert.assertEquals(1, names.size());
        Assert.assertTrue(names.contains(sarMbeanName));
        // Check getMBeanInfo
        Assert.assertNotNull(connection.getMBeanInfo(sarMbeanName));

        // Check the management JMX domains
        final ObjectName asMbeanName = new ObjectName("jboss.as:subsystem=sar");
        final ObjectName asExprMbeanName = new ObjectName("jboss.as.expr:subsystem=sar");
        names = connection.queryNames(new ObjectName("*:subsystem=sar,*"), null);
        Assert.assertEquals(names.toString(), 4, names.size());
        Assert.assertTrue(names.contains(asExprMbeanName));
        Assert.assertTrue(names.contains(asMbeanName));
        Assert.assertTrue(names.contains(new ObjectName("jboss.as.expr:extension=org.jboss.as.sar,subsystem=sar")));
        Assert.assertTrue(names.contains(new ObjectName("jboss.as:extension=org.jboss.as.sar,subsystem=sar")));
        // Check that names with no pattern work
        names = connection.queryNames(asMbeanName, null);
        Assert.assertEquals(1, names.size());
        Assert.assertTrue(names.contains(asMbeanName));
        names = connection.queryNames(asExprMbeanName, null);
        Assert.assertEquals(1, names.size());
        Assert.assertTrue(names.contains(asExprMbeanName));
        // Check getMBeanInfo
        Assert.assertNotNull(connection.getMBeanInfo(asMbeanName));
        Assert.assertNotNull(connection.getMBeanInfo(asExprMbeanName));
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
