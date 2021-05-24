/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.sar.order;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanPermission;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
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
import org.jboss.system.ServiceMBeanSupport;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that MBeans which extend {@link ServiceMBeanSupport} and depend on other such mbeans have their create/start/stop/destroyService methods called in correct dependency order.
 *
 * @author Brian Stansberry
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ServiceMBeanSupportOrderingTestCase {

    private static final String UNMANAGED_SAR_DEPLOYMENT_NAME = "service-mbean-order-test";
    private static final List<String> FORWARD_ORDER = Arrays.asList("A", "B", "C");
    private static final List<String> REVERSE_ORDER = Arrays.asList("C", "B", "A");

    @ContainerResource
    private ManagementClient managementClient;

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = ServiceMBeanSupportOrderingTestCase.UNMANAGED_SAR_DEPLOYMENT_NAME, managed = false)
    public static JavaArchive geTestMBeanSar() {
        final JavaArchive sar = ShrinkWrap.create(JavaArchive.class, "service-mbean-order-test.sar");
        sar.addClasses(LifecycleEmitterMBean.class, LifecycleEmitter.class);
        sar.addAsManifestResource(ServiceMBeanSupportOrderingTestCase.class.getPackage(), "jboss-service.xml", "jboss-service.xml");
        sar.addAsManifestResource(createPermissionsXmlAsset(
                new MBeanPermission(ServiceMBeanSupportOrderingTestCase.class.getPackage().getName() + ".*", "*")),
                "permissions.xml");


        return sar;
    }

    @Deployment
    public static JavaArchive getTestResultMBeanSar() {
        final JavaArchive sar = ShrinkWrap.create(JavaArchive.class, "service-mbean-order-test-result.sar");
        sar.addClasses(LifecycleListenerMBean.class, LifecycleListener.class);
        sar.addAsManifestResource(ServiceMBeanSupportOrderingTestCase.class.getPackage(), "result-jboss-service.xml",
                "jboss-service.xml");
        return sar;
    }

    /**
     * Tests that invocation on a service deployed within a .sar, inside a .ear without an application.xml, is successful.
     *
     * @throws Exception
     */
    @Test
    public void testServiceMBeanSupportLifecycleOrder() throws Exception {
        // get mbean server
        final JMXConnector connector = JMXConnectorFactory.connect(managementClient.getRemoteJMXURL(), DefaultConfiguration.credentials());
        final MBeanServerConnection mBeanServerConnection = connector.getMBeanServerConnection();

        // deploy the unmanaged sar
        deployer.deploy(ServiceMBeanSupportOrderingTestCase.UNMANAGED_SAR_DEPLOYMENT_NAME);
        // undeploy it
        deployer.undeploy(ServiceMBeanSupportOrderingTestCase.UNMANAGED_SAR_DEPLOYMENT_NAME);

        // Check the order of lifecycle events
        checkOrder(mBeanServerConnection, "Starts", FORWARD_ORDER);
        checkOrder(mBeanServerConnection, "Stops", REVERSE_ORDER);
        checkOrder(mBeanServerConnection, "Creates", FORWARD_ORDER);
        checkOrder(mBeanServerConnection, "Destroys", REVERSE_ORDER);
    }

    private static void checkOrder(MBeanServerConnection mBeanServerConnection, String lifecycleStage, List<String> expected) throws MalformedObjectNameException, ReflectionException, AttributeNotFoundException, InstanceNotFoundException, MBeanException, IOException {
        List<?> order = (List<?>) mBeanServerConnection.getAttribute(new ObjectName("jboss:name=OrderListener"),
                lifecycleStage);
        Assert.assertEquals("Unexpected order for " + lifecycleStage, expected, order);
    }
}