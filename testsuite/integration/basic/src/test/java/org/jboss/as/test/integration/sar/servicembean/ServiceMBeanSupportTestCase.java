/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.sar.servicembean;

import javax.management.MBeanPermission;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.DefaultConfiguration;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.system.ServiceMBean;
import org.jboss.system.ServiceMBeanSupport;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.naming.java.permission.JndiPermission;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * Test MBeans which implement {@link ServiceMBean} and extend {@link ServiceMBeanSupport}.
 *
 * @author Eduardo Martins
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ServiceMBeanSupportTestCase {

    private static final String UNMANAGED_SAR_DEPLOYMENT_NAME = "service-mbean-support-test";

    @ContainerResource
    private ManagementClient managementClient;

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = ServiceMBeanSupportTestCase.UNMANAGED_SAR_DEPLOYMENT_NAME, managed = false)
    public static JavaArchive geTestMBeanSar() {
        final JavaArchive sar = ShrinkWrap.create(JavaArchive.class, "service-mbean-support-test.sar");
        sar.addClasses(TestServiceMBean.class, TestService.class);
        sar.addAsManifestResource(ServiceMBeanSupportTestCase.class.getPackage(), "jboss-service.xml", "jboss-service.xml");
        sar.addAsManifestResource(createPermissionsXmlAsset(
                new JndiPermission("global/env/foo/legacy", "bind,unbind"),
                new MBeanPermission(TestResultService.class.getPackage().getName() + ".*", "*")),
                "permissions.xml");


        return sar;
    }

    @Deployment
    public static JavaArchive getTestResultMBeanSar() {
        final JavaArchive sar = ShrinkWrap.create(JavaArchive.class, "service-mbean-support-test-result.sar");
        sar.addClasses(TestResultServiceMBean.class, TestResultService.class);
        sar.addAsManifestResource(ServiceMBeanSupportTestCase.class.getPackage(), "result-jboss-service.xml",
                "jboss-service.xml");
        return sar;
    }

    /**
     * Tests that invocation on a service deployed within a .sar, inside a .ear without an application.xml, is successful.
     *
     * @throws Exception
     */
    @Test
    public void testSarWithServiceMBeanSupport() throws Exception {
        // get mbean server
        final JMXConnector connector = JMXConnectorFactory.connect(managementClient.getRemoteJMXURL(), DefaultConfiguration.credentials());
        final MBeanServerConnection mBeanServerConnection = connector.getMBeanServerConnection();
        try {
            // deploy the unmanaged sar
            deployer.deploy(ServiceMBeanSupportTestCase.UNMANAGED_SAR_DEPLOYMENT_NAME);
            // check the unmanaged mbean state
            int state = (Integer) mBeanServerConnection.getAttribute(new ObjectName("jboss:name=service-mbean-support-test"),
                    "State");
            Assert.assertEquals("Unexpected return state from Test MBean: " + state, ServiceMBean.STARTED, state);
        } finally {
            // undeploy it
            deployer.undeploy(ServiceMBeanSupportTestCase.UNMANAGED_SAR_DEPLOYMENT_NAME);
        }

        // check the result of life-cycle methods invocation, using result mbean
        // also check that the result mbean received lifecycle notifications
        String[] expectedAttributes = new String[] {"CreateServiceInvoked", "StartServiceInvoked", "StopServiceInvoked", "DestroyServiceInvoked",
            "StartingNotificationReceived", "StartedNotificationReceived", "StoppingNotificationReceived", "StoppedNotificationReceived"};

        // each of these attributes should be 'true'
        for(String attribute : expectedAttributes) {
            Boolean result = (Boolean) mBeanServerConnection.getAttribute(new ObjectName("jboss:name=service-mbean-support-test-result"),
                    attribute);
            Assert.assertTrue("Unexpected result for " + attribute + ": " + result, result);
        }
    }
}