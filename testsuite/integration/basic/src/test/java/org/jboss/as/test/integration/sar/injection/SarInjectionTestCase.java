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

package org.jboss.as.test.integration.sar.injection;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.sar.injection.pojos.A;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;


/**
 * [AS7-2574] setter declared in a superclass prevents SAR deployments from being deployed
 *
 * @author <a href="mailto:opalka.richard@gmail.com">Richard Opalka</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public final class SarInjectionTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment
    public static JavaArchive createDeployment() throws Exception {
        final JavaArchive sar = ShrinkWrap.create(JavaArchive.class, "injection.sar");
        sar.addPackage(A.class.getPackage());
        sar.addAsManifestResource(SarInjectionTestCase.class.getPackage(), "jboss-service.xml", "jboss-service.xml");
        return sar;
    }

    @Test
    public void testMBean() throws Exception {
        final JMXConnector connector = JMXConnectorFactory.connect(managementClient.getRemoteJMXURL());
        try {
            final MBeanServerConnection mbeanServer = connector.getMBeanServerConnection();
            final ObjectName objectName = new ObjectName("jboss:name=POJOService");
            Assert.assertTrue(2 == (Integer) mbeanServer.getAttribute(objectName, "Count"));
            Assert.assertTrue(2 == (Integer) mbeanServer.getAttribute(objectName, "InjectedCount"));
            Assert.assertTrue((Boolean) mbeanServer.getAttribute(objectName, "CreateCalled"));
            Assert.assertTrue((Boolean) mbeanServer.getAttribute(objectName, "StartCalled"));
            Assert.assertFalse((Boolean) mbeanServer.getAttribute(objectName, "StopCalled"));
            Assert.assertFalse((Boolean) mbeanServer.getAttribute(objectName, "DestroyCalled"));
        } finally {
            IoUtils.safeClose(connector);
        }
    }

}
