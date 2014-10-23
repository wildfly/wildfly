/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.sar.dependency;

import javax.management.MBeanInfo;
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
import org.jboss.msc.service.ServiceActivator;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DirectMBeanDependencyTestCase {
    private static final String DEPLOYMENT_NAME = "manual-jboss-service.sar";

    @ContainerResource
    private ManagementClient managementClient;

    @ArquillianResource
    private Deployer deployer;

    @Deployment
    public static JavaArchive getTestResultMBeanSar() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "direct-mbean.jar");
        jar.addClasses(XServiceMBean.class, XService.class, MBeanActivator.class);
        jar.addAsServiceProvider(ServiceActivator.class, MBeanActivator.class);
        return jar;
    }

    @Deployment(name = DEPLOYMENT_NAME, managed = false)
    public static JavaArchive geTestMBeanSar() {
        final JavaArchive sar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT_NAME);
        sar.addClasses(XServiceMBean.class, XService.class);
        sar.addAsManifestResource(DirectMBeanDependencyTestCase.class.getPackage(), "jboss-service.xml", "jboss-service.xml");
        return sar;
    }

    @Test
    public void testSarWithServiceMBeanSupport() throws Exception {
        // get mbean server
        try (JMXConnector connector = JMXConnectorFactory.connect(managementClient.getRemoteJMXURL())) {
            final MBeanServerConnection connection = connector.getMBeanServerConnection();
            deployer.deploy(DEPLOYMENT_NAME);
            try {
                MBeanInfo mBeanInfo = connection.getMBeanInfo(new ObjectName("jboss:name=xservice"));
                Assert.assertNotNull(mBeanInfo);
                // deploy the unmanaged sar
            } finally {
                // undeploy it
                deployer.undeploy(DEPLOYMENT_NAME);
            }
        }
    }
}