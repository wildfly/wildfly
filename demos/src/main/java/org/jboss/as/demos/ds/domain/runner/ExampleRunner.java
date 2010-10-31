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
package org.jboss.as.demos.ds.domain.runner;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.jboss.as.demos.DeploymentUtils;
import org.jboss.as.demos.DomainDeploymentUtils;
import org.jboss.as.demos.ds.mbean.Test;

import static org.jboss.as.protocol.StreamUtils.safeClose;

/**
 * Runner for the datasource demo
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class ExampleRunner {

    public static void main(String[] args) throws Exception {
        DomainDeploymentUtils utils = null;
        try {
            utils = new DomainDeploymentUtils("ds-mbean.sar", Test.class.getPackage());

            utils.deploy();

            ObjectName objectName = new ObjectName("jboss:name=test,type=ds");

            MBeanServerConnection mbeanServer = utils.getServerOneConnection();
            System.out.println("Calling TestMBean.test() on server one");
            String s = (String) mbeanServer.invoke(objectName, "test", new Object[0], new String[0]);
            System.out.println("Received reply: " + s);

            mbeanServer = utils.getServerTwoConnection();
            System.out.println("Calling TestMBean.test() on server two");
            s = (String) mbeanServer.invoke(objectName, "test", new Object[0], new String[0]);
            System.out.println("Received reply: " + s);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            utils.undeploy();
            safeClose(utils);
        }
    }
}
