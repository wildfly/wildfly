/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.embedded.demos.sar;

import java.net.InetAddress;

import javax.management.Attribute;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.container.MBeanServerConnectionProvider;
import org.jboss.as.demos.sar.archive.ConfigService;
import org.jboss.as.test.modular.utils.PollingUtils;
import org.jboss.as.test.modular.utils.ShrinkWrapUtils;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SarTestCase {

    @Deployment(testable = false)
    public static JavaArchive createDeployment() throws Exception {
        return ShrinkWrapUtils.createJavaArchive("demos/sar-example.sar", ConfigService.class.getPackage());
    }

    @Test
    public void testMBean() throws Exception {
        MBeanServerConnectionProvider provider = new MBeanServerConnectionProvider(InetAddress.getLocalHost(), 1090);
        MBeanServerConnection mbeanServer = provider.getConnection();
        ObjectName objectName = new ObjectName("jboss:name=test,type=config");
        PollingUtils.retryWithTimeout(10000, new PollingUtils.WaitForMBeanTask(mbeanServer, objectName));
        mbeanServer.getAttribute(objectName, "IntervalSeconds");
        mbeanServer.setAttribute(objectName, new Attribute("IntervalSeconds", 2));
    }

}
