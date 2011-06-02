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
package org.jboss.as.demos.serviceloader.runner;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.jboss.as.demos.DeploymentUtils;
import org.jboss.as.demos.serviceloader.archive.TestService;
import org.jboss.as.demos.serviceloader.mbean.Test;
import static org.jboss.as.protocol.StreamUtils.safeClose;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ExampleRunner {

    public static void main(String[] args) throws Exception {
        DeploymentUtils utils = null;
        try {
            utils = new DeploymentUtils("serviceloader-example.jar", TestService.class.getPackage());
            utils.addDeployment("serviceloader-mbean.sar", Test.class.getPackage());

            utils.deploy();
            ObjectName objectName = new ObjectName("jboss:name=test,type=serviceloader");

            MBeanServerConnection mbeanServer = utils.getConnection();

            System.out.println("Calling TestMBean.decorateWithServiceLoader(\"Hello\") on server");
            String s = (String)mbeanServer.invoke(objectName, "decorateWithServiceLoader", new Object[] {"Hello"}, new String[] {"java.lang.String"});
            System.out.println("Received reply: " + s);
        } finally {
            utils.undeploy();
            safeClose(utils);
        }
    }

}
