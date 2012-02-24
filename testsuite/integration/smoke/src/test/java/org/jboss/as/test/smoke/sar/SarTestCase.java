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
package org.jboss.as.test.smoke.sar;

import javax.management.Attribute;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.smoke.modular.utils.PollingUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SarTestCase {

    @Deployment(testable = false)
    public static JavaArchive createDeployment() throws Exception {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "sar-example.sar");
        jar.addPackage(SarTestCase.class.getPackage());
        jar.addAsManifestResource(SarTestCase.class.getPackage(), "jboss-service.xml", "jboss-service.xml");
        return jar;
    }

    @Test
    public void testMBean() throws Exception {
        MBeanServerConnection mbeanServer = JMXConnectorFactory.connect(new JMXServiceURL("service:jmx:remoting-jmx://127.0.0.1:9999")).getMBeanServerConnection();
        ObjectName objectName = new ObjectName("jboss:name=test,type=config");
        PollingUtils.retryWithTimeout(10000, new PollingUtils.WaitForMBeanTask(mbeanServer, objectName));
        mbeanServer.getAttribute(objectName, "IntervalSeconds");
        mbeanServer.setAttribute(objectName, new Attribute("IntervalSeconds", 2));
    }

}
