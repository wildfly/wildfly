/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.injection.valuefactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;

@RunWith(Arquillian.class)
@RunAsClient
public final class SarValueFactoryInjectionTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment
    public static JavaArchive createDeployment() throws Exception {
        final JavaArchive sar = ShrinkWrap.create(JavaArchive.class, "injection.sar");
        sar.addPackage(SourceBean.class.getPackage());
        sar.addAsManifestResource(SarValueFactoryInjectionTestCase.class.getPackage(), "jboss-service.xml", "jboss-service.xml");
        return sar;
    }

    @Test
    public void testMBean() throws Exception {
        final JMXConnector connector = JMXConnectorFactory.connect(managementClient.getRemoteJMXURL());
        try {
            final MBeanServerConnection mbeanServer = connector.getMBeanServerConnection();
            final ObjectName objectName = new ObjectName("jboss:name=TargetBean");
            Assert.assertEquals("Injection using value-factory without method arguments failed", 2, ((Integer) mbeanServer.getAttribute(objectName, "SourceCount")).intValue());
            Assert.assertEquals("Injection using value-factory with method argument failed", 4, ((Integer) mbeanServer.getAttribute(objectName, "CountWithArgument")).intValue());
        } finally {
            IoUtils.safeClose(connector);
        }
    }

}
