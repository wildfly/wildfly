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

import java.util.ServiceLoader;

import org.jboss.as.demos.serviceloader.archive.TestService;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ExampleRunner {

    public static void main(String[] args) throws Exception {
        ServiceLoader<TestService> loader = ServiceLoader.load(TestService.class);
        for (TestService service : loader) {
            System.out.println(service.decorate("Hello"));
        }

//        DeploymentUtils utils = new DeploymentUtils("serviceloader-example.jar", TestService.class.getPackage());
//        utils.addDeployment("serviceloader-mbean.sar", Test.class.getPackage(), true);
//
//        utils.deploy();
//        ObjectName objectName = new ObjectName("jboss:name=test,type=serviceloader");
//        utils.waitForDeploymentHack(objectName);
//
//        MBeanServerConnection mbeanServer = JMXClientConnectionFactory.getConnection("localhost", 1090);
//
//        Thread.sleep(1000);
//        mbeanServer.invoke(objectName, "test", new Object[0], new String[0]);
    }

}
