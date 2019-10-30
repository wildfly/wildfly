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

package org.jboss.as.test.integration.sar.injection.resource;

import javax.annotation.Resource;
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
