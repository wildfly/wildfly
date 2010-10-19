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
package org.jboss.as.demos.managedbean.runner;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.jboss.as.demos.DeploymentUtils;
import org.jboss.as.demos.managedbean.archive.SimpleManagedBean;
import org.jboss.as.demos.managedbean.mbean.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ExampleRunner {

    public static void main(String[] args) throws Exception {
        DeploymentUtils utils = new DeploymentUtils("managedbean-example.jar", SimpleManagedBean.class.getPackage());
        try {
            utils.addDeployment("managedbean-mbean.sar", Test.class.getPackage());

            utils.deploy();
            ObjectName objectName = new ObjectName("jboss:name=test,type=managedbean");
            utils.waitForDeploymentHack(objectName);

            MBeanServerConnection mbeanServer = utils.getConnection();

            //Thread.sleep(2000);
            System.out.println("Calling echo(\"Hello\")");
            Object o = mbeanServer.invoke(objectName, "echo", new Object[] {"Hello"}, new String[] {"java.lang.String"});
            System.out.println("echo returned " + o);
        } finally {
            utils.undeploy();
        }
    }

}
