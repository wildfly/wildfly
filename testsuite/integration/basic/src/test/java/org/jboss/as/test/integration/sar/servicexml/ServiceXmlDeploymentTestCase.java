/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.servicexml;

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
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

/**
 * Tests that jboss-service.xml is processed correctly in WAR, EJB JAR, and RAR deployments,
 * not just in .sar archives.
 *
 * @see <a href="https://issues.redhat.com/browse/WFLY-14858">WFLY-14858</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ServiceXmlDeploymentTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    private JMXConnector connector;

    @After
    public void closeConnector() {
        IoUtils.safeClose(connector);
    }

    @Deployment(name = "war", testable = false)
    public static WebArchive getWarDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "servicexml-war-test.war");
        war.addClasses(ServiceXmlDeploymentServiceMBean.class, ServiceXmlDeploymentService.class);
        war.addAsManifestResource(ServiceXmlDeploymentTestCase.class.getPackage(), "jboss-service-war.xml", "jboss-service.xml");
        return war;
    }

    @Deployment(name = "ejbjar", testable = false)
    public static JavaArchive getEjbJarDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "servicexml-ejbjar-test.jar");
        jar.addClasses(ServiceXmlDeploymentServiceMBean.class, ServiceXmlDeploymentService.class);
        jar.addAsManifestResource(ServiceXmlDeploymentTestCase.class.getPackage(), "jboss-service-ejbjar.xml", "jboss-service.xml");
        return jar;
    }

    @Deployment(name = "rar", testable = false)
    public static ResourceAdapterArchive getRarDeployment() {
        final JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClasses(ServiceXmlDeploymentServiceMBean.class, ServiceXmlDeploymentService.class, NoOpResourceAdapter.class);

        final ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "servicexml-rar-test.rar");
        rar.addAsLibrary(lib);
        rar.addAsManifestResource(ServiceXmlDeploymentTestCase.class.getPackage(), "jboss-service-rar.xml", "jboss-service.xml");
        rar.addAsManifestResource(ServiceXmlDeploymentTestCase.class.getPackage(), "ra.xml", "ra.xml");
        return rar;
    }

    @OperateOnDeployment("war")
    @Test
    public void testServiceXmlInWar() throws Exception {
        testMBeanAccessible("jboss:name=service-in-war");
    }

    @OperateOnDeployment("ejbjar")
    @Test
    public void testServiceXmlInEjbJar() throws Exception {
        testMBeanAccessible("jboss:name=service-in-ejbjar");
    }

    @OperateOnDeployment("rar")
    @Test
    public void testServiceXmlInRar() throws Exception {
        testMBeanAccessible("jboss:name=service-in-rar");
    }

    private void testMBeanAccessible(final String serviceName) throws Exception {
        final MBeanServerConnection mBeanServerConnection = getMBeanServerConnection();
        final ObjectName mbeanObjectName = new ObjectName(serviceName);
        final int num1 = 3;
        final int num2 = 4;
        Integer sum = (Integer) mBeanServerConnection.invoke(mbeanObjectName, "add", new Object[]{num1, num2},
                new String[]{Integer.TYPE.getName(), Integer.TYPE.getName()});
        Assert.assertEquals("Unexpected return value from MBean: " + mbeanObjectName, num1 + num2, (int) sum);
    }

    private MBeanServerConnection getMBeanServerConnection() throws IOException {
        connector = JMXConnectorFactory.connect(managementClient.getRemoteJMXURL(), DefaultConfiguration.credentials());
        return connector.getMBeanServerConnection();
    }
}
