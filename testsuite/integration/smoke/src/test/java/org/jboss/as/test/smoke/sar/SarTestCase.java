/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.sar;

import javax.management.Attribute;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xnio.IoUtils;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class SarTestCase {

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment(testable = false)
    public static JavaArchive createDeployment() throws Exception {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "sar-example.sar");
        jar.addPackage(SarTestCase.class.getPackage());
        jar.addAsManifestResource(SarTestCase.class.getPackage(), "jboss-service.xml", "jboss-service.xml");
        return jar;
    }

    @Test
    public void testMBean() throws Exception {
        final JMXConnector connector = JMXConnectorFactory.connect(managementClient.getRemoteJMXURL());
        try {
            MBeanServerConnection mbeanServer = connector.getMBeanServerConnection();
            ObjectName objectName = new ObjectName("jboss:name=test,type=config");
            mbeanServer.getAttribute(objectName, "IntervalSeconds");
            mbeanServer.setAttribute(objectName, new Attribute("IntervalSeconds", 2));
        } finally {
            IoUtils.safeClose(connector);
        }
    }

}
