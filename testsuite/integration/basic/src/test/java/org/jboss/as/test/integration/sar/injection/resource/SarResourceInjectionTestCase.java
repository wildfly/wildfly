/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.injection.resource;

import jakarta.annotation.Resource;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.DefaultConfiguration;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

/**
 * [AS7-1699] Tests {@link Resource} injection on MBeans
 *
 * @author Eduardo Martins
 */
@RunWith(Arquillian.class)
@RunAsClient
public final class SarResourceInjectionTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment
    public static JavaArchive createDeployment() throws Exception {
        return ShrinkWrap.create(JavaArchive.class, "as7-1699.sar")
                .addClasses(XMBean.class, X.class)
                .addAsManifestResource(SarResourceInjectionTestCase.class.getPackage(), "jboss-service.xml", "jboss-service.xml");
    }

    @Test
    public void testMBean() throws Exception {
        final JMXConnector connector = JMXConnectorFactory.connect(managementClient.getRemoteJMXURL(), DefaultConfiguration.credentials());
        try {
            final MBeanServerConnection mbeanServer = connector.getMBeanServerConnection();
            final ObjectName objectName = new ObjectName("jboss:name=X");
            Assert.assertTrue((Boolean) mbeanServer.invoke(objectName, "resourcesInjected", null, null));
        } finally {
            IoUtils.safeClose(connector);
        }
    }

}
