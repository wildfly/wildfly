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
package org.jboss.as.demos.sar.runner;

import javax.management.Attribute;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.jboss.as.demos.DeploymentUtils;
import org.jboss.as.demos.sar.archive.ConfigService;
import static org.jboss.as.protocol.StreamUtils.safeClose;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ExampleRunner {

    public static void main(String[] args) throws Exception {
        DeploymentUtils deploymentUtils = null;
        try {
            deploymentUtils = new DeploymentUtils("sar-example.sar", ConfigService.class.getPackage());

            deploymentUtils.deploy();
            ObjectName objectName = new ObjectName("jboss:name=test,type=config");

            MBeanServerConnection mbeanServer = deploymentUtils.getConnection();

            //A little sleep to give the logging done by the bean time to kick in
            Thread.sleep(1500);

            System.out.println("Checking the IntervalSeconds property");
            Object o = mbeanServer.getAttribute(objectName, "IntervalSeconds");
            System.out.println("IntervalSeconds was " + o + ", setting it to 2");
            mbeanServer.setAttribute(objectName, new Attribute("IntervalSeconds", 2));
            System.out.println("IntervalSeconds set");

            //A little sleep to give the logging resulting from the new interval time to show up in the logs
            Thread.sleep(3000);
        } finally {
            deploymentUtils.undeploy();
            safeClose(deploymentUtils);
        }
    }
}
