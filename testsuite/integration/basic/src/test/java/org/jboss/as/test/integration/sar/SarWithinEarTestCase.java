/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.sar;

import java.io.IOException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.DefaultConfiguration;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

/**
 * Test that a service configured in a .sar within a .ear deployment works fine, both when the .ear contains a application.xml
 * and when it doesn't.
 * <p/>
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SarWithinEarTestCase {

    private static final String EAR_WITHOUT_APPLICATION_XML = "sar-within-ear-without-application-xml.ear";

    private static final String EAR_WITH_APPLICATION_XML = "sar-within-ear-with-application-xml.ear";

    @ContainerResource
    private ManagementClient managementClient;

    private JMXConnector connector;

    @After
    public void closeConnector() {
        IoUtils.safeClose(connector);
    }
    /**
     * Create a .ear, without an application.xml, with a nested .sar deployment
     *
     * @return
     */
    @Deployment(name = "ear-without-application-xml", testable = false)
    public static EnterpriseArchive getEarWithoutApplicationDotXml() {
        final JavaArchive sar = ShrinkWrap.create(JavaArchive.class, "simple-sar.sar");
        sar.addClasses(SarWithinEarServiceMBean.class, SarWithinEarService.class);
        sar.addAsManifestResource(SarWithinEarTestCase.class.getPackage(), "jboss-service-without-application-xml.xml", "jboss-service.xml");

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_WITHOUT_APPLICATION_XML);
        ear.addAsModule(sar);
        return ear;
    }

    /**
     * Create a .ear with an application.xml and a nested .sar deployment
     *
     * @return
     */
    @Deployment(name = "ear-with-application-xml", testable = false)
    public static EnterpriseArchive getEarWithApplicationDotXml() {
        final JavaArchive sar = ShrinkWrap.create(JavaArchive.class, "simple-sar.sar");
        sar.addClasses(SarWithinEarServiceMBean.class, SarWithinEarService.class);
        sar.addAsManifestResource(SarWithinEarTestCase.class.getPackage(), "jboss-service-with-application-xml.xml", "jboss-service.xml");

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_WITH_APPLICATION_XML);
        ear.addAsModule(sar);
        ear.addAsManifestResource(SarWithinEarTestCase.class.getPackage(), "application.xml", "application.xml");
        return ear;
    }

    /**
     * Tests that invocation on a service deployed within a .sar, inside a .ear without an application.xml, is successful.
     *
     * @throws Exception
     */
    @OperateOnDeployment("ear-without-application-xml")
    @Test
    public void testSarWithinEarWithoutApplicationXml() throws Exception {
        this.testSarWithinEar("jboss:name=service-in-sar-within-a-ear-without-application-xml");
    }

    /**
     * Tests that invocation on a service deployed within a .sar, inside a .ear with an application.xml, is successful.
     *
     * @throws Exception
     */
    @OperateOnDeployment("ear-with-application-xml")
    @Test
    public void testSarWithinEarWithApplicationXml() throws Exception {
        this.testSarWithinEar("jboss:name=service-in-sar-within-a-ear-with-application-xml");
    }

    private void testSarWithinEar(final String serviceName) throws Exception {
        final MBeanServerConnection mBeanServerConnection = this.getMBeanServerConnection();
        final ObjectName mbeanObjectName = new ObjectName(serviceName);
        final int num1 = 3;
        final int num2 = 4;
        // invoke the operation on MBean
        Integer sum = (Integer) mBeanServerConnection.invoke(mbeanObjectName, "add", new Object[]{num1, num2}, new String[]{Integer.TYPE.getName(), Integer.TYPE.getName()});
        Assert.assertEquals("Unexpected return value from MBean: " + mbeanObjectName, num1 + num2, (int) sum);
    }

    private MBeanServerConnection getMBeanServerConnection() throws IOException {
        connector = JMXConnectorFactory.connect(managementClient.getRemoteJMXURL(), DefaultConfiguration.credentials());
        return connector.getMBeanServerConnection();
    }
}