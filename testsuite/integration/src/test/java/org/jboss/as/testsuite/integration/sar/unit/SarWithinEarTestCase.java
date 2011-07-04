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

package org.jboss.as.testsuite.integration.sar.unit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.testsuite.integration.sar.SarWithinEarServiceMBean;
import org.jboss.as.testsuite.integration.sar.SarWithinEarService;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.HashMap;

/**
 * Test that a service configured in a .sar within a .ear deployment works fine.
 *
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SarWithinEarTestCase {

    private static final String EAR_NAME = "sar-within-ear.ear";

    @Deployment (testable = false)
    public static EnterpriseArchive getDeployment() {
        final JavaArchive sar = ShrinkWrap.create(JavaArchive.class, "simple-sar.sar");
        sar.addClasses(SarWithinEarServiceMBean.class, SarWithinEarService.class);
        sar.addAsManifestResource("sar/jboss-service.xml", "jboss-service.xml");

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME);
        ear.addAsModule(sar);
        return ear;
    }

    /**
     * Tests that invocation on a service deployed within a .sar, inside a .ear, is successful.
     * 
     * @throws Exception
     */
    @Test
    public void testSarWithinEar() throws Exception {
        final MBeanServerConnection mBeanServerConnection = this.getMBeanServerConnection();
        final ObjectName mbeanObjectName = new ObjectName("jboss:name=service-in-sar-within-a-ear");
        final int num1 = 3;
        final int num2 = 4;
        // invoke the operation on MBean
        Integer sum = (Integer) mBeanServerConnection.invoke(mbeanObjectName, "add", new Object[]{num1, num2}, new String[]{Integer.TYPE.getName(), Integer.TYPE.getName()});
        Assert.assertEquals("Unexpected return value from MBean: " + mbeanObjectName, num1 + num2, (int) sum);
    }

    private MBeanServerConnection getMBeanServerConnection() throws IOException {
        return JMXConnectorFactory.connect(new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:1090/jmxrmi"),
                new HashMap<String, Object>()).getMBeanServerConnection();
    }
}
