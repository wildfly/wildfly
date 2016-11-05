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

package org.jboss.as.test.integration.sar.injection.depends;

import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
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


/**
 *
 * @author Tomasz Adamski
 */
@RunWith(Arquillian.class)
@RunAsClient
public final class DependsTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment
    public static JavaArchive createDeployment() throws Exception {
        final JavaArchive sar = ShrinkWrap.create(JavaArchive.class, "injection.sar");
        sar.addPackage(A.class.getPackage());
        sar.addAsManifestResource(DependsTestCase.class.getPackage(), "jboss-service.xml", "jboss-service.xml");
        return sar;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMBean() throws Exception {
        final MBeanServerConnection mbeanServer = JMXConnectorFactory.connect(managementClient.getRemoteJMXURL(), DefaultConfiguration.credentials()).getMBeanServerConnection();
        final ObjectName a1ObjectName = new ObjectName("test:service=A1");
        final ObjectName aObjectName =  (ObjectName) mbeanServer.getAttribute(a1ObjectName, "ObjectName");
        Assert.assertTrue(aObjectName.equals(new ObjectName("test:service=A")));
        final ObjectName bObjectName = new ObjectName("test:service=B");
        final List<ObjectName> objectNames =  (List<ObjectName>) mbeanServer.getAttribute(bObjectName, "ObjectNames");
        Assert.assertTrue(objectNames.contains(new ObjectName("test:service=A")));
        Assert.assertTrue(objectNames.contains(new ObjectName("test:service=A1")));
    }

}
